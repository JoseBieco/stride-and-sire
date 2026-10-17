package com.josebieco.strideandsire.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HorseAttributeRangesTest {
    @Test
    void constantsMatchVanilla263() {
        assertEquals(15.0, HorseAttributeRanges.HEALTH.vanillaMin(), 0);
        assertEquals(30.0, HorseAttributeRanges.HEALTH.vanillaMax(), 0);
        assertEquals(0.4, HorseAttributeRanges.JUMP.vanillaMin(), 1e-6);
        assertEquals(1.0, HorseAttributeRanges.JUMP.vanillaMax(), 1e-6);
        assertEquals(0.1125, HorseAttributeRanges.SPEED.vanillaMin(), 1e-6);
        assertEquals(0.3375, HorseAttributeRanges.SPEED.vanillaMax(), 1e-6);
    }

    @Test
    void ceilingIsTwentyPercentAboveMax() {
        assertEquals(36.0, HorseAttributeRanges.HEALTH.ceiling(), 1e-9);
        for (HorseAttributeRanges r : HorseAttributeRanges.values()) {
            assertEquals(1.2 * r.vanillaMax(), r.ceiling(), 1e-12);
            assertEquals(r, HorseAttributeRanges.of(r.attribute()));
        }
    }

    @Test
    void maxIsBAndCeilingIsS() {
        for (HorseAttributeRanges r : HorseAttributeRanges.values()) {
            assertEquals(Grade.B, r.gradeOf(r.vanillaMax()));
            assertEquals(Grade.S, r.gradeOf(r.ceiling()));
            assertEquals(Grade.E, r.gradeOf(r.vanillaMin()));
        }
    }
}
