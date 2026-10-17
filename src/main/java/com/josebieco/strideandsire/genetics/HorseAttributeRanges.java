package com.josebieco.strideandsire.genetics;

/**
 * Vanilla 26.3 horse attribute ranges, mirroring AbstractHorse:
 * MIN/MAX_MOVEMENT_SPEED = (float) generateSpeed(0.0 / 1.0) with
 * generateSpeed(p) = (0.45F + 3 * p * 0.3) * 0.25; MIN/MAX_JUMP_STRENGTH = (float)
 * generateJumpStrength(0.0 / 1.0) with 0.4F + 3 * p * 0.2; MIN/MAX_HEALTH = generateMaxHealth
 * with 15 + (0..7) + (0..8). Vanilla uses these as the generation range and the offspring clamp.
 * The ceiling is 1.2 x vanillaMax. No Minecraft dependencies.
 */
public enum HorseAttributeRanges {
    SPEED(GeneticAttribute.SPEED, (float) (0.45F * 0.25), (float) ((0.45F + 0.3 + 0.3 + 0.3) * 0.25)),
    JUMP(GeneticAttribute.JUMP, (float) 0.4F, (float) (0.4F + 0.2 + 0.2 + 0.2)),
    HEALTH(GeneticAttribute.HEALTH, 15.0F, 15.0F + 7 + 8);

    public static final double CEILING_FACTOR = 1.2;

    private final GeneticAttribute attribute;
    private final double vanillaMin;
    private final double vanillaMax;

    HorseAttributeRanges(GeneticAttribute attribute, double vanillaMin, double vanillaMax) {
        this.attribute = attribute;
        this.vanillaMin = vanillaMin;
        this.vanillaMax = vanillaMax;
    }

    public static HorseAttributeRanges of(GeneticAttribute attribute) {
        return switch (attribute) {
            case SPEED -> SPEED;
            case JUMP -> JUMP;
            case HEALTH -> HEALTH;
        };
    }

    public GeneticAttribute attribute() {
        return attribute;
    }

    public double vanillaMin() {
        return vanillaMin;
    }

    public double vanillaMax() {
        return vanillaMax;
    }

    /** Highest value a diet-guided breeding may reach: 1.2 x vanillaMax. */
    public double ceiling() {
        return vanillaMax * CEILING_FACTOR;
    }

    public Grade gradeOf(double value) {
        return AttributeGrading.gradeOf(value, vanillaMin, vanillaMax);
    }
}
