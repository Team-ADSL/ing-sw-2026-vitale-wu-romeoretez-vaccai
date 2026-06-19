package org.adsl.client.view.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GlyphLayoutTest {

    private static final String BODY = String.join("\n",
            "scale=2.0",
            "ascent=40",
            "glyph.0.advance=24", "glyph.0.bearingX=2", "glyph.0.bearingTop=38",
            "glyph.1.advance=20", "glyph.1.bearingX=2", "glyph.1.bearingTop=38",
            "glyph.2.advance=22", "glyph.2.bearingX=3", "glyph.2.bearingTop=36",
            "glyph.x.advance=18", "glyph.x.bearingX=1", "glyph.x.bearingTop=30",
            "glyph.minus.advance=14", "glyph.minus.bearingX=1", "glyph.minus.bearingTop=20");

    @Test
    void tokens_mapsDigitsXAndMinus() {
        assertEquals(List.of("x", "1", "2"), GlyphLayout.tokens("x12"));
        assertEquals(List.of("minus", "7"), GlyphLayout.tokens("-7"));
        assertEquals(List.of("4", "2"), GlyphLayout.tokens("42"));
    }

    @Test
    void tokens_unmappedCharYieldsNull() {
        assertNull(GlyphLayout.tokens("1a"));
        assertNull(GlyphLayout.tokens("+5"));
    }

    @Test
    void parse_readsScaleAndAscent() {
        GlyphLayout.Metrics m = GlyphLayout.parse(BODY);
        assertEquals(2.0, m.scale(), 1e-9);
        assertTrue(m.has("1"));
        assertTrue(m.has("x"));
        assertTrue(m.has("minus"));
        assertFalse(m.has("9"));
    }

    @Test
    void place_positionsGlyphsByAdvanceInDisplaySpace() {
        GlyphLayout.Metrics m = GlyphLayout.parse(BODY);
        List<GlyphLayout.Placement> p = GlyphLayout.place("12", m);
        assertEquals(2, p.size());
        // glyph "1": x=(0+2)/2=1, y=(40-38)/2=1
        assertEquals(1.0, p.get(0).x(), 1e-9);
        assertEquals(1.0, p.get(0).y(), 1e-9);
        // glyph "2": pen advanced by 20 → x=(20+3)/2=11.5, y=(40-36)/2=2
        assertEquals(11.5, p.get(1).x(), 1e-9);
        assertEquals(2.0, p.get(1).y(), 1e-9);
    }

    @Test
    void place_handlesPrefixGlyphs() {
        GlyphLayout.Metrics m = GlyphLayout.parse(BODY);
        List<GlyphLayout.Placement> p = GlyphLayout.place("-2", m);
        assertEquals(2, p.size());
        assertEquals("minus", p.get(0).name());
        // minus advance 14 → second glyph pen=14 → x=(14+3)/2=8.5
        assertEquals(8.5, p.get(1).x(), 1e-9);
    }

    @Test
    void place_returnsEmptyWhenGlyphMissing() {
        GlyphLayout.Metrics m = GlyphLayout.parse(BODY);
        assertTrue(GlyphLayout.place("9", m).isEmpty());   // no glyph.9 in BODY
        assertTrue(GlyphLayout.place("1a", m).isEmpty());  // unmapped char
    }
}
