package com.josebieco.strideandsire.item.saddlebag;

/**
 * Saddlebag material tiers. Pure Java: no Minecraft imports, so it stays unit-testable.
 */
public enum SaddlebagMaterial {
    LEATHER(9, false, false, false),
    IRON(9, true, false, false),
    DIAMOND(9, true, false, false),
    GOLD(9, true, false, true),
    NETHERITE(9, true, true, false);

    private final int slotCount;
    private final boolean explosionProtection;
    private final boolean fireProtection;
    private final boolean goldPiglinPassive;

    SaddlebagMaterial(int slotCount,
                      boolean explosionProtection,
                      boolean fireProtection,
                      boolean goldPiglinPassive) {
        this.slotCount = slotCount;
        this.explosionProtection = explosionProtection;
        this.fireProtection = fireProtection;
        this.goldPiglinPassive = goldPiglinPassive;
    }

    /** Container slot count. Fixed at 9 for every tier; Fase C changes this via a different item, not here. */
    public int slotCount() {
        return slotCount;
    }

    /** Contents survive the horse's death by explosion damage. */
    public boolean protectsContentsFromExplosion() {
        return explosionProtection;
    }

    /** Contents survive the horse's death by fire/lava damage. */
    public boolean protectsContentsFromFire() {
        return fireProtection;
    }

    /** Mounted horse with this saddlebag makes Piglins neutral (shared with the gold horseshoe check). */
    public boolean hasGoldPiglinPassive() {
        return goldPiglinPassive;
    }
}
