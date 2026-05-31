# GUI Responsive Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every JavaFX client screen resize coherently — fixed edge padding, width-capped fluid panels, height-filling lists, one shared shrink-to-fit fallback, and a single unified card size with fill-based hand pagination in the game board.

**Architecture:** Extract pure layout math into a unit-tested `LayoutMath` helper. Add a `ResponsiveScaler` (built on `LayoutMath.fitScale`) wired once in `GUI` so it wraps every screen root and zooms content out (never above 1.0, never below a floor) when the window is too small. Per-screen FXML/code changes handle fixed padding, width caps, and list growth. `GameScreen` gets a unified card width and fill-based pagination.

**Tech Stack:** Java 25, JavaFX 21, Maven, JUnit Jupiter 6.0.3.

**Spec:** `docs/superpowers/specs/2026-05-30-gui-responsive-layout-design.md`

---

## File Structure

- Create `client/src/main/java/org/adsl/client/view/gui/LayoutMath.java` — pure layout math (no JavaFX scene deps).
- Create `client/src/main/java/org/adsl/client/view/gui/ResponsiveScaler.java` — wraps a screen root, applies uniform zoom-out.
- Create `client/src/test/java/org/adsl/client/view/gui/LayoutMathTest.java` — unit tests for the math.
- Modify `client/pom.xml` — add JUnit test dependency.
- Modify `client/src/main/java/org/adsl/client/view/gui/GUI.java` — wrap roots in scaler, bump min window size.
- Modify `client/src/main/resources/fxml/home.fxml` — ListView fills height, panel cap + padding.
- Modify `client/src/main/resources/fxml/lobby.fxml` — same as home.
- Modify `client/src/main/java/org/adsl/client/view/gui/screens/GameScreen.java` — unified card width + fill pagination + remove `applyContentScale`.
- Modify menu FXML/screens (`login.fxml`, `connecting.fxml`, `disconnected.fxml`, `TotemPickingScreen.java`) — fixed padding + min=pref so the scaler zooms them.
- Modify `client/src/main/resources/fxml/endgame.fxml` — fixed padding.

**Note on testing strategy:** JavaFX scene-graph layout is not practically unit-testable without TestFX (out of scope). Only the pure math (`LayoutMath`) is unit-tested. All UI wiring is verified by running the app at specific window sizes (manual checkpoints). Build command used throughout: `mvn -pl client -am package -DskipTests`. Run the GUI with `tools\start\windows\run_gui_nobuild.bat` after a build.

---

## Task 1: JUnit test dependency for the client module

**Files:**
- Modify: `client/pom.xml`

- [ ] **Step 1: Add the test dependency**

In `client/pom.xml`, inside `<dependencies>`, after the `shared` dependency block (around line 20), add:

```xml
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
```

(The version is managed by the parent pom — `junit.version` = 6.0.3 — so no `<version>` here.)

- [ ] **Step 2: Verify it resolves**

Run: `mvn -pl client -am test-compile`
Expected: `BUILD SUCCESS` (no compile, no tests yet).

- [ ] **Step 3: Commit**

```bash
git add client/pom.xml
git commit -m "Add JUnit test dependency to client module"
```

---

## Task 2: `LayoutMath` pure helpers (TDD)

**Files:**
- Create: `client/src/main/java/org/adsl/client/view/gui/LayoutMath.java`
- Test: `client/src/test/java/org/adsl/client/view/gui/LayoutMathTest.java`

- [ ] **Step 1: Write the failing test**

Create `client/src/test/java/org/adsl/client/view/gui/LayoutMathTest.java`:

```java
package org.adsl.client.view.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutMathTest {

    // ── cardWidth ────────────────────────────────────────────────────────────
    @Test
    void cardWidth_fitsWithinMaxWhenAmpleSpace() {
        // 6 cards, lots of width -> clamps to max (95)
        assertEquals(95.0, LayoutMath.cardWidth(2000, 6, 10, 48, 95), 0.001);
    }

    @Test
    void cardWidth_shrinksToFitButNotBelowMin() {
        // 6 cards, tiny width -> clamps to min (48)
        assertEquals(48.0, LayoutMath.cardWidth(100, 6, 10, 48, 95), 0.001);
    }

    @Test
    void cardWidth_dividesSpaceMinusGaps() {
        // 4 cards, gap 10 => (avail - 30)/4. avail=430 -> (400)/4 = 100 -> clamped to 95
        assertEquals(95.0, LayoutMath.cardWidth(430, 4, 10, 48, 95), 0.001);
        // avail=270 -> (240)/4 = 60, within [48,95]
        assertEquals(60.0, LayoutMath.cardWidth(270, 4, 10, 48, 95), 0.001);
    }

    @Test
    void cardWidth_zeroCardsReturnsMax() {
        assertEquals(95.0, LayoutMath.cardWidth(500, 0, 10, 48, 95), 0.001);
    }

    // ── perPage ──────────────────────────────────────────────────────────────
    @Test
    void perPage_countsCardsThatFit() {
        // width 300, card 60, gap 10 => floor((300+10)/(60+10)) = floor(310/70)=4
        assertEquals(4, LayoutMath.perPage(300, 60, 10));
    }

    @Test
    void perPage_atLeastOne() {
        assertEquals(1, LayoutMath.perPage(10, 60, 10));
    }

    // ── fitScale ─────────────────────────────────────────────────────────────
    @Test
    void fitScale_oneWhenContentFits() {
        assertEquals(1.0, LayoutMath.fitScale(800, 600, 1000, 800, 0.4), 0.001);
    }

    @Test
    void fitScale_shrinksToFitTheTighterAxis() {
        // content 1000x600 into 500x600 -> width-bound -> 0.5
        assertEquals(0.5, LayoutMath.fitScale(1000, 600, 500, 600, 0.4), 0.001);
    }

    @Test
    void fitScale_neverBelowFloor() {
        double s = LayoutMath.fitScale(2000, 2000, 200, 200, 0.4);
        assertEquals(0.4, s, 0.001);
    }

    @Test
    void fitScale_safeOnZeroContent() {
        assertEquals(1.0, LayoutMath.fitScale(0, 0, 500, 500, 0.4), 0.001);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl client -am test -Dtest=LayoutMathTest`
Expected: FAIL — compilation error, `LayoutMath` does not exist.

- [ ] **Step 3: Implement `LayoutMath`**

Create `client/src/main/java/org/adsl/client/view/gui/LayoutMath.java`:

```java
package org.adsl.client.view.gui;

/**
 * Pure layout arithmetic shared by the responsive GUI. No JavaFX scene-graph
 * dependencies so it can be unit-tested in isolation.
 */
public final class LayoutMath {

    private LayoutMath() {}

    /**
     * The single card width shared by every card row. Sizes the most-constrained
     * single-line row ({@code maxCardsInARow}) to fit {@code availWidth} minus the
     * inter-card gaps, clamped to {@code [min, max]}.
     */
    public static double cardWidth(double availWidth, int maxCardsInARow,
                                   double gap, double min, double max) {
        if (maxCardsInARow <= 0) return max;
        double w = (availWidth - gap * (maxCardsInARow - 1)) / maxCardsInARow;
        return Math.max(min, Math.min(max, w));
    }

    /**
     * How many fixed-width cards fit in a hand row of {@code availWidth}. Always
     * at least 1 so a page is never empty.
     */
    public static int perPage(double availWidth, double cardWidth, double gap) {
        if (cardWidth <= 0) return 1;
        return Math.max(1, (int) Math.floor((availWidth + gap) / (cardWidth + gap)));
    }

    /**
     * Uniform shrink-to-fit scale for content of {@code contentW x contentH} inside
     * {@code availW x availH}. Clamped to {@code [minScale, 1.0]} — never enlarges,
     * never shrinks past the floor (so content stays legible rather than vanishing).
     */
    public static double fitScale(double contentW, double contentH,
                                  double availW, double availH, double minScale) {
        if (contentW <= 0 || contentH <= 0) return 1.0;
        double s = Math.min(availW / contentW, availH / contentH);
        if (s > 1.0) s = 1.0;
        if (s < minScale) s = minScale;
        return s;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl client -am test -Dtest=LayoutMathTest`
Expected: PASS — 10 tests green.

- [ ] **Step 5: Commit**

```bash
git add client/src/main/java/org/adsl/client/view/gui/LayoutMath.java client/src/test/java/org/adsl/client/view/gui/LayoutMathTest.java
git commit -m "Add LayoutMath responsive helpers with unit tests"
```

---

## Task 3: `ResponsiveScaler` + wire into `GUI`

**Files:**
- Create: `client/src/main/java/org/adsl/client/view/gui/ResponsiveScaler.java`
- Modify: `client/src/main/java/org/adsl/client/view/gui/GUI.java`

- [ ] **Step 1: Create `ResponsiveScaler`**

Create `client/src/main/java/org/adsl/client/view/gui/ResponsiveScaler.java`:

```java
package org.adsl.client.view.gui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Wraps a screen root in a centering holder that uniformly scales the content
 * DOWN when the window is smaller than the content's laid-out size — the shared
 * "never clip" fallback for every screen. When the content fits, the scale is
 * 1.0 (no-op). It never enlarges content and never shrinks past
 * {@link #MIN_SCALE}, so menu screens stay natural-sized on big monitors and
 * legible on tiny ones.
 *
 * <p>Reads the content's actual laid-out size (not preferred): a StackPane keeps
 * its child at the child's minimum size when the holder is smaller, so the child
 * overflows and we scale by {@code holder/child}. Scaling is a visual transform
 * and does not feed back into layout, so this cannot loop.
 */
public final class ResponsiveScaler {

    private static final double MIN_SCALE = 0.4;

    private ResponsiveScaler() {}

    /** Returns a holder Region that contains and scales {@code content} to fit. */
    public static Region wrap(Parent content) {
        StackPane holder = new StackPane(content);
        holder.setMinSize(0, 0);
        StackPane.setAlignment(content, Pos.CENTER);

        Runnable apply = () -> {
            if (!(content instanceof Region r)) return;
            double s = LayoutMath.fitScale(
                    r.getWidth(), r.getHeight(),
                    holder.getWidth(), holder.getHeight(), MIN_SCALE);
            content.setScaleX(s);
            content.setScaleY(s);
        };

        holder.widthProperty().addListener((_, _, _) -> apply.run());
        holder.heightProperty().addListener((_, _, _) -> apply.run());
        if (content instanceof Region r) {
            r.widthProperty().addListener((_, _, _) -> apply.run());
            r.heightProperty().addListener((_, _, _) -> apply.run());
        }
        return holder;
    }
}
```

- [ ] **Step 2: Wrap every screen root in `GUI`**

In `client/src/main/java/org/adsl/client/view/gui/GUI.java`:

The initial scene is built at line 84:
```java
            Scene scene = new Scene(currentScreen.getRoot(), WINDOW_W, WINDOW_H);
```
Change it to:
```java
            Scene scene = new Scene(ResponsiveScaler.wrap(currentScreen.getRoot()), WINDOW_W, WINDOW_H);
```

The transition path is at line 228:
```java
            stage.setScene(new Scene(screen.getRoot(), stage.getWidth(), stage.getHeight()));
```
Change it to:
```java
            stage.setScene(new Scene(ResponsiveScaler.wrap(screen.getRoot()), stage.getWidth(), stage.getHeight()));
```

(Add `import` only if the class is in a different package — it is the same package `org.adsl.client.view.gui`, so no import needed.)

- [ ] **Step 3: Build**

Run: `mvn -pl client -am package -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Run-checkpoint**

Run `tools\start\windows\run_gui_nobuild.bat`, reach the login screen, then shrink the window below the minimum content size.
Expected: the whole screen scales down uniformly and stays fully visible (nothing clipped); at normal sizes it looks unchanged. Stop the clients.

- [ ] **Step 5: Commit**

```bash
git add client/src/main/java/org/adsl/client/view/gui/ResponsiveScaler.java client/src/main/java/org/adsl/client/view/gui/GUI.java
git commit -m "Add ResponsiveScaler shrink-to-fit wrapper for all screens"
```

---

## Task 4: Window minimum size

**Files:**
- Modify: `client/src/main/java/org/adsl/client/view/gui/GUI.java:38-39`

- [ ] **Step 1: Raise the minimum window size**

Change the constants (lines 38-39):
```java
    private static final double MIN_W = 800;
    private static final double MIN_H = 540;
```
to:
```java
    private static final double MIN_W = 900;
    private static final double MIN_H = 620;
```

- [ ] **Step 2: Build**

Run: `mvn -pl client -am package -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add client/src/main/java/org/adsl/client/view/gui/GUI.java
git commit -m "Raise GUI minimum window size to 900x620"
```

---

## Task 5: Home screen — list fills height, panel cap + padding

**Files:**
- Modify: `client/src/main/resources/fxml/home.fxml`

- [ ] **Step 1: Make the outer VBox use fixed edge padding and fill height**

In `home.fxml`, the outer content VBox (line 14) is:
```xml
    <VBox alignment="TOP_CENTER" spacing="14">
        <padding><Insets top="40" right="24" bottom="24" left="24"/></padding>
```
Change the padding to a uniform fixed margin:
```xml
    <VBox alignment="TOP_CENTER" spacing="14">
        <padding><Insets top="28" right="28" bottom="28" left="28"/></padding>
```

- [ ] **Step 2: Let the Join panel grow and the ListView fill it**

The Join panel (lines 32-40) currently is:
```xml
        <VBox styleClass="panel" spacing="10" alignment="CENTER" maxWidth="700" fillWidth="false">
            <padding><Insets top="16" right="20" bottom="16" left="20"/></padding>
            <Label text="Join an active game" style="-fx-font-size: 15px; -fx-text-fill: #F2B035;"/>
            <ListView fx:id="gamesList" prefWidth="460" prefHeight="180"/>
            <HBox spacing="10" alignment="CENTER">
                <Button fx:id="joinButton" text="Join Selected" onAction="#onJoinSelected"/>
                <Button fx:id="logoutButton" text="Logout" onAction="#onLogout"/>
            </HBox>
        </VBox>
```
Replace it with (panel grows vertically; ListView fills it; cap stays 700):
```xml
        <VBox styleClass="panel" spacing="10" alignment="CENTER" maxWidth="700" fillWidth="true" VBox.vgrow="ALWAYS">
            <padding><Insets top="16" right="20" bottom="16" left="20"/></padding>
            <Label text="Join an active game" style="-fx-font-size: 15px; -fx-text-fill: #F2B035;"/>
            <ListView fx:id="gamesList" maxWidth="Infinity" VBox.vgrow="ALWAYS"/>
            <HBox spacing="10" alignment="CENTER">
                <Button fx:id="joinButton" text="Join Selected" onAction="#onJoinSelected"/>
                <Button fx:id="logoutButton" text="Logout" onAction="#onLogout"/>
            </HBox>
        </VBox>
```

(`prefHeight="180"` is removed so the ListView height follows the panel; `VBox.vgrow="ALWAYS"` on both panel and ListView makes them consume the spare vertical space.)

- [ ] **Step 2b: Add the import for the larger window**

No new FXML imports are needed (`Insets`, `ListView`, `VBox` already imported).

- [ ] **Step 3: Build**

Run: `mvn -pl client -am package -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Run-checkpoint**

Run the GUI, log in, create/join so several games exist (use `python3 testers/socket_auto_plays.py` or open multiple clients), reach Home. Grow the window vertically.
Expected: more game rows become visible as the window gets taller (instead of a fixed ~3); padding to the edges stays constant; the panel never exceeds 700px wide. Stop the clients.

- [ ] **Step 5: Commit**

```bash
git add client/src/main/resources/fxml/home.fxml
git commit -m "Home: games list fills available height, fixed edge padding"
```

---

## Task 6: Lobby screen — same treatment

**Files:**
- Modify: `client/src/main/resources/fxml/lobby.fxml`

- [ ] **Step 1: Fixed edge padding**

Change the outer VBox padding (line 14) from:
```xml
        <padding><Insets top="40" right="24" bottom="24" left="24"/></padding>
```
to:
```xml
        <padding><Insets top="28" right="28" bottom="28" left="28"/></padding>
```

- [ ] **Step 2: Players panel grows, ListView fills**

Replace the players panel (lines 20-28):
```xml
        <VBox styleClass="panel" spacing="10" alignment="CENTER" maxWidth="700" fillWidth="false">
            <padding><Insets top="16" right="20" bottom="16" left="20"/></padding>
            <Label text="Players in lobby" style="-fx-font-size: 15px; -fx-text-fill: #F2B035;"/>
            <ListView fx:id="playersList" prefWidth="460" prefHeight="180"/>
            <HBox spacing="10" alignment="CENTER">
                <Button text="Start Game" onAction="#onStart"/>
                <Button text="Leave Lobby" onAction="#onLeave"/>
            </HBox>
        </VBox>
```
with:
```xml
        <VBox styleClass="panel" spacing="10" alignment="CENTER" maxWidth="700" fillWidth="true" VBox.vgrow="ALWAYS">
            <padding><Insets top="16" right="20" bottom="16" left="20"/></padding>
            <Label text="Players in lobby" style="-fx-font-size: 15px; -fx-text-fill: #F2B035;"/>
            <ListView fx:id="playersList" maxWidth="Infinity" VBox.vgrow="ALWAYS"/>
            <HBox spacing="10" alignment="CENTER">
                <Button text="Start Game" onAction="#onStart"/>
                <Button text="Leave Lobby" onAction="#onLeave"/>
            </HBox>
        </VBox>
```

- [ ] **Step 3: Build**

Run: `mvn -pl client -am package -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Run-checkpoint**

Run the GUI, create a game to enter the lobby, grow the window.
Expected: players list area grows with height; padding constant; panel capped at 700px. Stop the clients.

- [ ] **Step 5: Commit**

```bash
git add client/src/main/resources/fxml/lobby.fxml
git commit -m "Lobby: players list fills available height, fixed edge padding"
```

---

## Task 7: GameScreen — unified card width + fill-based pagination

**Files:**
- Modify: `client/src/main/java/org/adsl/client/view/gui/screens/GameScreen.java`
- Modify: `client/src/main/resources/fxml/game.fxml`

This task replaces three things: (a) per-row card widths → one unified `cardW`; (b) the
fixed `SELF_HAND_PER_PAGE` / `OPP_HAND_PER_PAGE` caps → `LayoutMath.perPage`; (c) the
height-only `applyContentScale` → the `ResponsiveScaler` from Task 3 (board zoom-out via
fixed edge padding + content min size).

Read the methods before editing: `computeCardWidth` (around line 504), `renderBoard`
(around line 513), `renderSelfPanel`, `renderOpponentPanels` / `buildOpponentPanel`, and
the `SELF_HAND_PER_PAGE`/`OPP_HAND_PER_PAGE` usages.

- [ ] **Step 1: Compute one unified card width in `renderBoard`**

In `renderBoard` the rows are currently sized independently (lines ~519-528):
```java
        double topW = computeCardWidth(topN, CARD_GAP, CARD_MAX_W, CARD_MIN_W);
        double botW = computeCardWidth(botN, CARD_GAP, CARD_MAX_W, CARD_MIN_W);
        double offW = computeCardWidth(offN, 0,         TILE_MAX_W, TILE_MIN_W);

        renderRow(topRow, top, Row.UPPER, topW);
        renderRow(bottomRow, bot, Row.LOWER, botW);
```
Replace with a single shared card width driven by the most-constrained card row
(offer tiles keep their own independent sizing):
```java
        // One card size for EVERY card (top row, bottom row, hands): size the
        // most-constrained single-line card row, so cards never differ in size.
        int maxRowCards = Math.max(topN, botN);
        double cardW = LayoutMath.cardWidth(availableWidth(), maxRowCards, CARD_GAP, CARD_MIN_W, CARD_MAX_W);
        double offW = LayoutMath.cardWidth(availableWidth(), offN, 0, TILE_MIN_W, TILE_MAX_W);

        renderRow(topRow, top, Row.UPPER, cardW);
        renderRow(bottomRow, bot, Row.LOWER, cardW);
```
Add the import at the top of the file (after the other `org.adsl.client.view.gui` imports):
```java
import org.adsl.client.view.gui.LayoutMath;
```
Store `cardW` for the hands: add a private field near `selfHandPage` (line ~177):
```java
    private double sharedCardW = CARD_MIN_W;
```
and set it right after computing `cardW` in `renderBoard`:
```java
        sharedCardW = cardW;
```

- [ ] **Step 2: Remove the now-unused `computeCardWidth`**

Delete the `computeCardWidth` method (around lines 504-511) — it is replaced by
`LayoutMath.cardWidth`. Verify no other callers remain:

Run: `grep -rn "computeCardWidth" client/src/main`
Expected: no matches.

- [ ] **Step 3: Self hand — use `sharedCardW` and fill-based pagination**

In `renderSelfPanel`, the hand currently paginates by the fixed `SELF_HAND_PER_PAGE`
constant. Replace the page-size logic so the page holds however many `sharedCardW`-wide
cards fit in the hand's available width, and each card is rendered at `sharedCardW`.

Find the block that computes `paginated` / `pageCount` from `SELF_HAND_PER_PAGE`
(around lines 549-556):
```java
        boolean paginated = total > SELF_HAND_PER_PAGE;
        int pageCount = paginated ? (int) Math.ceil(total / (double) SELF_HAND_PER_PAGE) : 1;
        if (selfHandPage >= pageCount) selfHandPage = pageCount - 1;
        if (selfHandPage < 0) selfHandPage = 0;

        int from = selfHandPage * SELF_HAND_PER_PAGE;
        int to = Math.min(from + SELF_HAND_PER_PAGE, total);
```
Replace with:
```java
        // Fit as many shared-width cards as the hand area allows; only then paginate.
        double handAvail = Math.max(0, rootStack.getWidth() - IDENTITY_COL_W - 2 * NAV_BTN_W - 60);
        int perPage = LayoutMath.perPage(handAvail, sharedCardW, HAND_GAP);
        boolean paginated = total > perPage;
        int pageCount = paginated ? (int) Math.ceil(total / (double) perPage) : 1;
        if (selfHandPage >= pageCount) selfHandPage = pageCount - 1;
        if (selfHandPage < 0) selfHandPage = 0;

        int from = selfHandPage * perPage;
        int to = Math.min(from + perPage, total);
```
Then ensure each self-hand card is built at `sharedCardW` (not `SELF_HAND_W`). Find where
the hand cards are created in `renderSelfPanel` (the card-width passed to the card builder)
and pass `sharedCardW` clamped to the hand's own min/max, i.e. replace the per-card width
argument with `sharedCardW`.

- [ ] **Step 4: Opponent hands — use `sharedCardW` and fill-based pagination**

In `buildOpponentPanel` / the opponent hand population, replace the `OPP_HAND_PER_PAGE`
page-size logic the same way, using:
```java
        int oppPerPage = LayoutMath.perPage(cardAvail, sharedCardW, HAND_GAP);
```
where `cardAvail` is the existing per-panel available width already computed in
`buildOpponentPanel`. Build opponent cards at `sharedCardW`. Use `oppPerPage` in place of
`OPP_HAND_PER_PAGE` for the page count and slice bounds.

- [ ] **Step 5: Remove the fixed per-page constants**

Delete the now-unused constants (lines ~97-98):
```java
    private static final int    SELF_HAND_PER_PAGE = 11;
    private static final int    OPP_HAND_PER_PAGE  = 8;
```
Run: `grep -rn "SELF_HAND_PER_PAGE\|OPP_HAND_PER_PAGE" client/src/main`
Expected: no matches.

- [ ] **Step 6: Remove `applyContentScale`, rely on ResponsiveScaler**

Delete the `applyContentScale` method (lines ~492-502) and its call at the end of
`renderBoard` (`Platform.runLater(this::applyContentScale);`). The board zoom-out is now
handled by the `ResponsiveScaler` wrapping the screen root.

Run: `grep -rn "applyContentScale" client/src/main`
Expected: no matches.

- [ ] **Step 7: Fixed edge padding + board min size so the scaler zooms it**

In `client/src/main/resources/fxml/game.fxml`, the root is a `StackPane fx:id="rootStack"`
containing a `BorderPane fx:id="rootPane"`. Give the BorderPane a fixed margin from the
window edges by adding padding to `rootStack`. Change:
```xml
<StackPane fx:id="rootStack" xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
           style="-fx-background-color: #2c1a0e;">
```
to add padding via an Insets child at the end of the StackPane (before `</StackPane>`):
```xml
    <padding><Insets top="20" right="20" bottom="20" left="20"/></padding>
```
and ensure `<?import javafx.geometry.Insets?>` is present at the top (it is not in
game.fxml yet — add it).

Then, so the board zooms out (instead of clipping) when the window is below its minimum,
set the BorderPane's minimum size to its preferred size in `GameScreen`'s constructor
after `this.root = fxmlRoot;`:
```java
        rootPane.setMinWidth(Region.USE_PREF_SIZE);
        rootPane.setMinHeight(Region.USE_PREF_SIZE);
```
`Region` is already imported in GameScreen.

- [ ] **Step 8: Build**

Run: `mvn -pl client -am package -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 9: Run-checkpoint**

Run the GUI, start a 2-player game, reach the board. Then:
- Resize wider/narrower: top row, bottom row and your hand cards are always the **same size** as each other and change together.
- Narrow the window: cards shrink to MIN, then the hand shows a `‹ ›` arrow only once they no longer fit (no premature arrow).
- Shrink below the minimum window: the whole board zooms out, nothing clipped; padding to edges constant.
Stop the clients.

- [ ] **Step 10: Commit**

```bash
git add client/src/main/java/org/adsl/client/view/gui/screens/GameScreen.java client/src/main/resources/fxml/game.fxml
git commit -m "GameScreen: unified card size, fill-based pagination, scaler zoom-out"
```

---

## Task 8: Menu screens — fixed padding + zoom-out

**Files:**
- Modify: `client/src/main/resources/fxml/login.fxml`
- Modify: `client/src/main/resources/fxml/connecting.fxml`
- Modify: `client/src/main/resources/fxml/disconnected.fxml`
- Modify: `client/src/main/java/org/adsl/client/view/gui/screens/TotemPickingScreen.java`

These screens are centered panels. The `ResponsiveScaler` (Task 3) already zooms them out
when the window is small **if** their content reports a min size larger than the window.
A centered VBox with `maxHeight="-Infinity"` reports its pref as content size, so the
scaler already works. The only change needed is a consistent fixed outer padding so the
panels never touch the edges, and (TotemPicking) confirming the FlowPane wraps.

- [ ] **Step 1: Login — add fixed padding around the centered content**

In `login.fxml`, the outer `<VBox alignment="CENTER" spacing="10" StackPane.alignment="CENTER">`
(line 13) has no padding. Add:
```xml
        <padding><Insets top="28" right="28" bottom="28" left="28"/></padding>
```
(`<?import javafx.geometry.Insets?>` is already present.)

- [ ] **Step 2: Connecting — add fixed padding**

In `connecting.fxml`, the outer `<VBox ... styleClass="panel">` already has internal
padding; add an outer margin by wrapping is unnecessary — instead add `StackPane` padding.
Change:
```xml
<StackPane xmlns="http://javafx.com/javafx"
           style="-fx-background-color: transparent;">
```
to add before `</StackPane>`:
```xml
    <padding><Insets top="28" right="28" bottom="28" left="28"/></padding>
```
and add `<?import javafx.geometry.Insets?>` at the top.

- [ ] **Step 3: Disconnected — add fixed padding**

In `disconnected.fxml`, add the same `StackPane` padding before `</StackPane>`:
```xml
    <padding><Insets top="28" right="28" bottom="28" left="28"/></padding>
```
`<?import javafx.geometry.Insets?>` is already present.

- [ ] **Step 4: TotemPicking — fixed padding (already a FlowPane)**

In `TotemPickingScreen.java`, the outer VBox uses `outer.setPadding(new Insets(50))`
(line 68). That is already a fixed margin — change it to the shared value for consistency:
```java
        outer.setPadding(new Insets(28));
```
The totem row is a `FlowPane` (line 83) and already wraps, so no further change is needed;
the scaler handles vertical overflow on very small windows.

- [ ] **Step 5: Build**

Run: `mvn -pl client -am package -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Run-checkpoint**

Run the GUI. On the login, connecting, and totem-picking screens, shrink the window:
Expected: content stays padded from the edges and zooms out uniformly when small; never clipped. Stop the clients.

- [ ] **Step 7: Commit**

```bash
git add client/src/main/resources/fxml/login.fxml client/src/main/resources/fxml/connecting.fxml client/src/main/resources/fxml/disconnected.fxml client/src/main/java/org/adsl/client/view/gui/screens/TotemPickingScreen.java
git commit -m "Menu screens: consistent fixed edge padding"
```

---

## Task 9: EndGame — fixed padding

**Files:**
- Modify: `client/src/main/resources/fxml/endgame.fxml`

EndGame is already responsive (SplitPane + VGrow + tables with `maxWidth=Infinity`); only
the outer padding needs aligning to the shared value.

- [ ] **Step 1: Use the shared padding**

In `endgame.fxml`, the outer VBox padding (line 16) is:
```xml
        <padding><Insets top="40" right="48" bottom="32" left="48"/></padding>
```
Change to:
```xml
        <padding><Insets top="28" right="28" bottom="28" left="28"/></padding>
```

- [ ] **Step 2: Build**

Run: `mvn -pl client -am package -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run-checkpoint**

Finish a game to reach EndGame; resize.
Expected: tables grow/shrink with the window; padding constant; scaler zooms out below min. Stop the clients.

- [ ] **Step 4: Commit**

```bash
git add client/src/main/resources/fxml/endgame.fxml
git commit -m "EndGame: consistent fixed edge padding"
```

---

## Task 10: Full verification

- [ ] **Step 1: Full build with tests**

Run: `mvn package -DskipTests=false`
Expected: `BUILD SUCCESS`, `LayoutMathTest` green, existing tests still pass.

- [ ] **Step 2: Manual verification matrix**

Run `tools\start\windows\run_gui_clean.bat` and walk each screen at three window sizes
(maximized, default, and dragged near the minimum), confirming the spec's behavior table:

| Screen | Wider | Taller | Narrower / tiny |
|---|---|---|---|
| Login / Connecting / Disconnected / Totem | centered, padded, capped | — | zoom-out, padded, no clip |
| Home / Lobby | panel capped at 700, padded | more list rows visible | list scrolls; zoom-out if forced tiny |
| GameScreen | cards grow to MAX, then more hand cards/page | board centered | cards shrink to MIN then hand paginates; board zoom-out below min |
| EndGame | tables fill | tables fill | zoom-out below min |

- [ ] **Step 3: Final commit (if any cleanup)**

```bash
git add -A
git commit -m "Responsive layout: final verification pass"
```

---

## Notes for the implementer

- Do not re-add `centerBox` size listeners in `GameScreen` — they previously caused a
  render feedback loop (see commit on `layout-resizing`). Only `rootStack` width/height
  drives re-render, debounced.
- The chip snapshot cache (`Chip.java`) already exists; do not change it.
- `availableWidth()` in `GameScreen` returns `centerBox.getWidth() - 40` (fallback to
  estimate before first layout). The unified `cardW` uses it unchanged.
- If a `Region`/`Parent` cast in `ResponsiveScaler` ever fails for a screen whose root is
  not a `Region`, the scaler is a no-op for it (scale stays 1.0) — acceptable.
