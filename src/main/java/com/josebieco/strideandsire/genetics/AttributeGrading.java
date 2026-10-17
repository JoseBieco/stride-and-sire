package com.josebieco.strideandsire.genetics;

/** Pure grading of an attribute value against the vanilla range. No Minecraft dependencies. */
public final class AttributeGrading {
    private AttributeGrading() {}

    /**
     * Fraction used for grading: within the vanilla range it is the position inside
     * [vanillaMin, vanillaMax]; above vanillaMax it is value / vanillaMax (so 1.0 is vanilla max,
     * 1.2 is the expanded ceiling).
     */
    public static double percentOf(double value, double vanillaMin, double vanillaMax) {
        if (!(vanillaMax > 0) || !(vanillaMax > vanillaMin)) {
            throw new IllegalArgumentException(
                    "Invalid vanilla range: min=" + vanillaMin + ", max=" + vanillaMax);
        }
        return value <= vanillaMax ? (value - vanillaMin) / (vanillaMax - vanillaMin) : value / vanillaMax;
    }

    /** E [0,25%), D [25,50%), C [50,75%), B [75,100%], A (100,110%], S (110,120%]. */
    public static Grade gradeOf(double value, double vanillaMin, double vanillaMax) {
        double pct = percentOf(value, vanillaMin, vanillaMax);
        if (pct < 0.25) return Grade.E;
        if (pct < 0.50) return Grade.D;
        if (pct < 0.75) return Grade.C;
        if (pct <= 1.0) return Grade.B;
        if (pct <= 1.1) return Grade.A;
        return Grade.S;
    }
}
