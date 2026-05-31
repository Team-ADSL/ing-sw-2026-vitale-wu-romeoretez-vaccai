# GUI Responsive Layout — Design

**Date:** 2026-05-30
**Branch:** layout-resizing
**Scope:** All JavaFX client screens (`client/src/main/java/org/adsl/client/view/gui/screens/`)

## Problem

The GUI does not adapt well to window resizing:

1. The content does not grow with a larger window — only the background scales; the
   content stays at a fixed size.
2. Shrinking the window clips the opponent panels (left/right/top) and, when small
   enough, hides everything except the central track band (top card row, offer track,
   bottom card row).
3. The self hand caps at a static card count (`SELF_HAND_PER_PAGE = 11`), so the
   pagination arrow appears even when horizontal space is still available.
4. Card sizes are computed per row, so cards in different rows (e.g. the self hand vs
   the top board row) shrink at different times and end up different sizes.
5. List screens (Home, Lobby) use a fixed `prefHeight="180"` ListView, showing only
   ~3 rows regardless of available vertical space.

## Goals

- A single coherent responsive model applied across every screen.
- Panels grow/shrink with the window while keeping a **fixed padding** from the edges.
- Dynamic lists show **more items** when more vertical space is available.
- In the game board, **all cards share exactly one size**; hands paginate by fill, not
  by a fixed count; nothing is ever clipped.

## Decisions (from brainstorming)

- **Growth model:** elements grow up to a MAX size; beyond that, extra space reveals
  MORE content (more hand cards, more list rows) rather than growing further.
- **Underflow model:** enforce a minimum window size; if the OS forces the window
  smaller, the whole content zooms out uniformly (never clipped).
- **Unified card size scope:** all *cards* (top row, bottom row, self hand, opponent
  hands) share one size. Offer **tiles** keep their own independent sizing (they form a
  continuous track and have a different aspect ratio).
- **Panel width on wide screens:** panels fill `window − padding` but cap at a maximum
  readable width, then center; the fixed padding is always respected.

## Architecture

### Shared primitive: `ResponsiveScaler`

A reusable wrapper used by every screen. Given a content node, it observes the window
size and applies a uniform `Scale` to shrink the content to fit (contain), centered,
clamped to `[MIN_SCALE, 1.0]`.

- Never scales **above 1.0** → menu screens never become oversized on large monitors.
- Never scales **below MIN_SCALE** → guarantees content is never clipped on small
  windows.
- This is the generalization of the ad-hoc `applyContentScale` zoom-out currently in
  `GameScreen`; that method is removed and replaced by this primitive.

`ResponsiveScaler` is a *fallback* layer. Normal/large windows are handled by fluid
fill + caps + list growth (below); the scaler only engages when content cannot fit even
at minimum element sizes.

Proposed location: `client/.../view/gui/ResponsiveScaler.java` (sibling of `Chip`,
`ImageCatalog`). It exposes a static helper to wrap a content node in a centering
`StackPane` and install size listeners that recompute the scale (debounced, consistent
with the existing resize debounce in `GameScreen`).

### Common layout rules (all screens)

- **Fixed padding** `PAD ≈ 28px` from the window edges, always respected.
- **Content max width** `MENU_MAX_W ≈ 900px` for menu/list screens: panel width =
  `min(window − 2·PAD, MENU_MAX_W)`, centered. GameScreen uses the full width
  (`window − 2·PAD`, no cap).
- **Dynamic lists fill height** → more rows visible with more vertical space; scroll
  only on overflow.
- **`Stage` minimum size** `≈ 900×620`, set once at GUI bootstrap. Below that (forced),
  `ResponsiveScaler` zooms out.

### Per screen-family plan

**Menu screens** — Login, Connecting, Disconnected, TotemPicking
- Centered panel, `MENU_MAX_W` cap + `PAD`, wrapped in `ResponsiveScaler`.
- TotemPicking already uses a `FlowPane` (the 5 totems wrap); keep it, add the scaler.

**List screens** — Home, Lobby
- Remove the fixed `prefHeight="180"` on the `ListView`; let it `VGrow` to fill the
  panel, which itself fills the available height (panel `VGrow`).
- Panel width capped at `MENU_MAX_W`, padding `PAD`.
- Result: more games / players visible as the window grows taller.

**EndGame**
- Already responsive (`SplitPane` + `VGrow` + tables with `maxWidth=Infinity`).
- Add the fixed padding and the `ResponsiveScaler` fallback only.

**GameScreen** (the core)
- Fills `window − PAD` (no width cap).
- **Unified card width:** `cardW = clamp(centerWidth / max(N_top, N_bottom), CARD_MIN,
  CARD_MAX)`. Applied to the top row, bottom row, self hand, and opponent hands — all
  cards identical size.
- **Hands paginate by fill:** `perPage = floor((handWidth + GAP) / (cardW + GAP))`; the
  pagination arrow is shown only when `perPage < handCardCount`. (Replaces the fixed
  `SELF_HAND_PER_PAGE` / `OPP_HAND_PER_PAGE` caps.)
- **Offer tiles:** keep independent `tileW = clamp(centerWidth / N_offer, TILE_MIN,
  TILE_MAX)` so the track stays continuous.
- Opponent panels keep a fixed minimum width so they remain visible.
- `applyContentScale` (height-only ad-hoc scale) is replaced by `ResponsiveScaler`.
- Board card rows never paginate (single line, shrink to MIN); if they cannot fit even
  at MIN, `ResponsiveScaler` zooms the whole board out.

## Behavior summary

| Window change | Menu/List screens | GameScreen |
|---|---|---|
| Wider | panel grows to `MENU_MAX_W`, then centered; padding fixed | cards grow to `CARD_MAX`, then more hand cards per page |
| Taller | lists show more rows | board centers; hands unaffected (paginate by width) |
| Narrower | panel shrinks to cap/min; scaler if needed | cards shrink to `CARD_MIN`, then hands paginate; scaler if board can't fit |
| Below min size | `ResponsiveScaler` zoom-out, never clipped | `ResponsiveScaler` zoom-out, never clipped |

## Out of scope

- TUI client (text UI) — unaffected.
- Visual restyle / colors / fonts beyond what resizing requires.
- The card chip rasterization caching (already implemented separately).

## Open risks

- `ResponsiveScaler` interacts with the existing resize debounce and full-board
  re-render in `GameScreen`; the scaler must not itself trigger a re-render loop (the
  `centerBox` size listeners were already removed for this reason).
- Setting `Stage` min size requires locating the GUI bootstrap where the primary
  `Stage`/`Scene` is created.
