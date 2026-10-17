package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.LogicalSide;

/**
 * Sangue Frio (Fearless). Vanilla 26.3 {@code AbstractHorse.hurtServer} makes a hurt horse rear
 * ({@code standIfPossible()}) one time in three, right after the damage is applied; it never ejects the rider. A horse
 * with {@link LineageTrait#FEARLESS} {@code MANIFEST} does not rear from sudden zombie / creeper / explosion damage:
 * the damage event marks the horse, and its standing state is cleared at the next level-tick boundary — before the
 * entity data is ever broadcast, so clients never see it rear. The damage itself is untouched (Review Focus item 4).
 * Horses without the manifest trait (ABSENT or LATENT) keep vanilla behaviour exactly; nothing is done to them.
 */
public final class FearlessPanicSuppressionHandler {
    /** Fearless horses hurt by a panic source since the last level-tick boundary (server thread only). */
    private static List<AbstractHorse> pendingClear = new ArrayList<>();

    private FearlessPanicSuppressionHandler() {}

    public static void register() {
        LivingDamageEvent.BUS.addListener(FearlessPanicSuppressionHandler::onLivingDamage);
        TickEvent.LevelTickEvent.Pre.BUS.addListener(event -> {
            if (event.side() == LogicalSide.SERVER) {
                clearPendingRears();
            }
        });
        TickEvent.LevelTickEvent.Post.BUS.addListener(event -> {
            if (event.side() == LogicalSide.SERVER) {
                clearPendingRears();
            }
        });
    }

    /** True if {@code source} is a zombie hit, a creeper attack/explosion, or any explosion damage. */
    public static boolean isPanicSource(DamageSource source) {
        if (source == null) {
            return false;
        }
        return source.getEntity() instanceof Zombie
            || source.getDirectEntity() instanceof Zombie
            || source.getEntity() instanceof Creeper
            || source.getDirectEntity() instanceof Creeper
            || source.is(DamageTypeTags.IS_EXPLOSION);
    }

    private static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse
                && !horse.level().isClientSide()
                && HorseshoeAccess.isEligible(horse)
                && isPanicSource(event.getSource())
                && HorseGeneticsAccess.hasManifest(horse, LineageTrait.FEARLESS)) {
            pendingClear.add(horse);
        }
    }

    /** Undoes the rear vanilla may have rolled after the damage of a marked Fearless horse. */
    public static void clearPendingRears() {
        if (pendingClear.isEmpty()) {
            return;
        }
        List<AbstractHorse> horses = pendingClear;
        pendingClear = new ArrayList<>();
        for (AbstractHorse horse : horses) {
            if (!horse.isRemoved() && horse.isStanding()) {
                horse.clearStanding();
            }
        }
    }
}
