package com.josebieco.strideandsire.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AttributeGradingTest {
    @Test void bandsMatchSpecTable() {
        double min = 0, max = 100;
        assertEquals(Grade.E, AttributeGrading.gradeOf(10, min, max));
        assertEquals(Grade.D, AttributeGrading.gradeOf(30, min, max));
        assertEquals(Grade.C, AttributeGrading.gradeOf(60, min, max));
        assertEquals(Grade.B, AttributeGrading.gradeOf(90, min, max));
        assertEquals(Grade.A, AttributeGrading.gradeOf(105, min, max));
        assertEquals(Grade.S, AttributeGrading.gradeOf(115, min, max));
    }

    @Test void bandBoundariesAreExact() {
        double min = 0, max = 100;
        assertEquals(Grade.B, AttributeGrading.gradeOf(100, min, max));
        assertEquals(Grade.A, AttributeGrading.gradeOf(100.0001, min, max));
        assertEquals(Grade.S, AttributeGrading.gradeOf(110.0001, min, max));
    }

    @Test void nonZeroMinUsesRangePercentBelowMax() {
        double min = 15, max = 30;
        assertEquals(Grade.E, AttributeGrading.gradeOf(14, min, max));
        assertEquals(Grade.E, AttributeGrading.gradeOf(15, min, max));
        assertEquals(Grade.D, AttributeGrading.gradeOf(18.75, min, max));
        assertEquals(Grade.B, AttributeGrading.gradeOf(30, min, max));
        assertEquals(Grade.A, AttributeGrading.gradeOf(30.0001, min, max));
        assertEquals(Grade.A, AttributeGrading.gradeOf(33, min, max));
        assertEquals(Grade.S, AttributeGrading.gradeOf(33.0001, min, max));
        assertEquals(Grade.S, AttributeGrading.gradeOf(36, min, max));
    }

    @Test void lowerEdgesBelongToHigherBand() {
        double min = 0, max = 100;
        assertEquals(Grade.E, AttributeGrading.gradeOf(24.999, min, max));
        assertEquals(Grade.D, AttributeGrading.gradeOf(25, min, max));
        assertEquals(Grade.D, AttributeGrading.gradeOf(49.999, min, max));
        assertEquals(Grade.C, AttributeGrading.gradeOf(50, min, max));
        assertEquals(Grade.C, AttributeGrading.gradeOf(74.999, min, max));
        assertEquals(Grade.B, AttributeGrading.gradeOf(75, min, max));
    }

    @Test void percentOfFollowsRuling() {
        assertEquals(0.25, AttributeGrading.percentOf(18.75, 15, 30), 1e-12);
        assertEquals(1.1, AttributeGrading.percentOf(33, 15, 30), 1e-12);
    }

    @Test void degenerateRangesRejected() {
        assertThrows(IllegalArgumentException.class, () -> AttributeGrading.gradeOf(1, 5, 5));
        assertThrows(IllegalArgumentException.class, () -> AttributeGrading.gradeOf(1, 6, 5));
        assertThrows(IllegalArgumentException.class, () -> AttributeGrading.gradeOf(1, -5, 0));
    }
}
