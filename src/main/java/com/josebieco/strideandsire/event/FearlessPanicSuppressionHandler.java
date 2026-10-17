package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.genetics.LineageTrait;
import java.lang.reflect.Field;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

/**
 * Handles sudden panic-inducing damage (zombie attack or nearby creeper/explosion) on mounted eligible horses:
 * <ul>
 *   <li>When {@link LineageTrait#FEARLESS} is {@code MANIFEST}, suppresses rearing ({@code standIfPossible}) and rider
 *       dismount while still applying the full damage to the horse's health (Review Focus item 4).</li>
 *   <li>When {@link LineageTrait#FEARLESS} is {@code LATENT} or {@code ABSENT}, triggers the panic reaction (rearing
 *       and dismounting the rider).</li>
 * </ul>
 */
public final class FearlessPanicSuppressionHandler {
    private static final Field ENTITY_RANDOM_FIELD = entityRandomField();

    private FearlessPanicSuppressionHandler() {}

    public static void register() {
        LivingDamageEvent.BUS.addListener(FearlessPanicSuppressionHandler::onLivingDamage);
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
        if (!(event.getEntity() instanceof AbstractHorse horse)
                || horse.level().isClientSide()
                || !HorseshoeAccess.isEligible(horse)
                || !isPanicSource(event.getSource())) {
            return;
        }
        if (HorseGeneticsAccess.hasManifest(horse, LineageTrait.FEARLESS)) {
            horse.clearStanding();
            suppressNextRearRoll(horse);
        } else if (horse.isVehicle()) {
            horse.standIfPossible();
            horse.ejectPassengers();
        }
    }

    /**
     * {@code AbstractHorse.hurtServer} calls {@code this.random.nextInt(3) == 0} immediately after
     * {@code super.hurtServer} (which fires {@link LivingDamageEvent}). Wrapping {@code Entity.random} for that single
     * {@code nextInt(3)} call guarantees it returns {@code 1} so {@code standIfPossible()} is never invoked on a
     * Fearless horse.
     */
    private static void suppressNextRearRoll(AbstractHorse horse) {
        try {
            RandomSource current = (RandomSource) ENTITY_RANDOM_FIELD.get(horse);
            if (current instanceof SuppressNextRearRandom) {
                return;
            }
            ENTITY_RANDOM_FIELD.set(horse, new SuppressNextRearRandom(horse, current));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access Entity.random", e);
        }
    }

    private static Field entityRandomField() {
        try {
            Field field = Entity.class.getDeclaredField("random");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot access Entity.random", e);
        }
    }

    private static final class SuppressNextRearRandom implements RandomSource {
        private final AbstractHorse horse;
        private final RandomSource delegate;
        private boolean suppressed;

        private SuppressNextRearRandom(AbstractHorse horse, RandomSource delegate) {
            this.horse = horse;
            this.delegate = delegate;
        }

        private void restore() {
            try {
                if (ENTITY_RANDOM_FIELD.get(horse) == this) {
                    ENTITY_RANDOM_FIELD.set(horse, delegate);
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        @Override
        public int nextInt(int bound) {
            if (!suppressed && bound == 3) {
                suppressed = true;
                restore();
                return 1;
            }
            return delegate.nextInt(bound);
        }

        @Override
        public RandomSource fork() {
            return delegate.fork();
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            return delegate.forkPositional();
        }

        @Override
        public void setSeed(long seed) {
            delegate.setSeed(seed);
        }

        @Override
        public int nextInt() {
            return delegate.nextInt();
        }

        @Override
        public long nextLong() {
            return delegate.nextLong();
        }

        @Override
        public boolean nextBoolean() {
            return delegate.nextBoolean();
        }

        @Override
        public float nextFloat() {
            return delegate.nextFloat();
        }

        @Override
        public double nextDouble() {
            return delegate.nextDouble();
        }

        @Override
        public double nextGaussian() {
            return delegate.nextGaussian();
        }
    }
}
