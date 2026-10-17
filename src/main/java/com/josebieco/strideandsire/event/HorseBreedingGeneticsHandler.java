package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseGeneticsAttachment;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.genetics.AttributeInheritance;
import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.genetics.HorseAttributes;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitInheritance;
import com.josebieco.strideandsire.genetics.TraitState;
import com.josebieco.strideandsire.item.genetics.BreedingDietItem;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.random.RandomGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Hooks horse feeding and foal creation ({@link BabyEntitySpawnEvent}) to apply diet-guided or unguided attribute
 * inheritance (Task 4) and lineage trait inheritance (Task 5).
 *
 * <p>Each horse records the diet item that put <i>it</i> in love in its Forge persistent data (saved with the entity,
 * like vanilla's {@code InLove} counter). The record is consumed by the breeding and cleared as soon as the horse is no
 * longer in love (love timed out, {@code resetLove}, age change), so a later love mode started by code or another mod is
 * never mistaken for a diet-guided one.
 */
public final class HorseBreedingGeneticsHandler {
    static final String DIET_KEY = StrideAndSire.MODID + ":breeding_diet";

    private HorseBreedingGeneticsHandler() {}

    public static void register() {
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(HorseBreedingGeneticsHandler::onEntityInteractSpecific);
        LivingEvent.LivingTickEvent.BUS.addListener(HorseBreedingGeneticsHandler::onLivingTick);
        BabyEntitySpawnEvent.BUS.addListener(HorseBreedingGeneticsHandler::onBabyEntitySpawn);
    }

    /** Records the diet item that put {@code horse} into love mode for its upcoming breeding (server side). */
    public static void recordDietItem(AbstractHorse horse, BreedingDietItem item) {
        if (!HorseshoeAccess.isEligible(horse) || horse.level().isClientSide()) {
            return;
        }
        if (item == null || item == BreedingDietItem.NONE) {
            horse.getPersistentData().remove(DIET_KEY);
        } else {
            horse.getPersistentData().putString(DIET_KEY, item.name());
        }
    }

    /** Returns the currently recorded diet item for {@code horse}, or {@link BreedingDietItem#NONE}. */
    public static BreedingDietItem getRecordedDietItem(AbstractHorse horse) {
        CompoundTag data = horse.getPersistentData();
        if (!horse.isInLove()) {
            data.remove(DIET_KEY);
            return BreedingDietItem.NONE;
        }
        String name = data.getStringOr(DIET_KEY, "");
        for (BreedingDietItem item : BreedingDietItem.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return BreedingDietItem.NONE;
    }

    /**
     * Resolves and consumes both parents' diet records. Returns the favored set: the union of the attributes favored by
     * each parent's diet item (empty = unguided breeding).
     */
    public static EnumSet<GeneticAttribute> consumeBreedingDiet(AbstractHorse parentA, AbstractHorse parentB) {
        EnumSet<GeneticAttribute> favored = EnumSet.noneOf(GeneticAttribute.class);
        for (AbstractHorse parent : new AbstractHorse[] {parentA, parentB}) {
            GeneticAttribute attribute = getRecordedDietItem(parent).favoredGeneticAttribute();
            if (attribute != null) {
                favored.add(attribute);
            }
            parent.getPersistentData().remove(DIET_KEY);
        }
        return favored;
    }

    /**
     * Applies the Fase C attribute inheritance rules to {@code child}: diet-guided iff {@code favored} is non-empty,
     * each attribute in {@code favored} getting the best-parent chance.
     */
    public static void applyOffspringAttributes(
            AbstractHorse parentA,
            AbstractHorse parentB,
            AbstractHorse child,
            EnumSet<GeneticAttribute> favored,
            RandomSource random) {
        Map<GeneticAttribute, Double> rolled = AttributeInheritance.rollChildValues(
            attributesOf(parentA), attributesOf(parentB), favored, adapt(random));
        for (Map.Entry<GeneticAttribute, Double> entry : rolled.entrySet()) {
            HorseAttributes.setBase(child, entry.getKey(), entry.getValue());
        }
        child.setHealth(child.getMaxHealth());
    }

    /** Applies the Fase C lineage trait inheritance rules to {@code child} from {@code parentA} and {@code parentB}. */
    public static void applyOffspringTraits(
            AbstractHorse parentA,
            AbstractHorse parentB,
            AbstractHorse child,
            RandomSource random) {
        HorseGeneticsAttachment genA = HorseGeneticsAccess.get(parentA);
        HorseGeneticsAttachment genB = HorseGeneticsAccess.get(parentB);
        RandomGenerator generator = adapt(random);
        EnumMap<LineageTrait, TraitState> childTraits = new EnumMap<>(LineageTrait.class);
        for (LineageTrait trait : LineageTrait.values()) {
            childTraits.put(trait, TraitInheritance.rollChildState(genA.state(trait), genB.state(trait), generator));
        }
        HorseGeneticsAccess.set(child, HorseGeneticsAttachment.of(childTraits));
    }

    /** Adapts Minecraft's {@link RandomSource} to the JDK abstraction the pure genetics classes take. */
    public static RandomGenerator adapt(RandomSource random) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return random.nextLong();
            }

            @Override
            public double nextDouble() {
                return random.nextDouble();
            }
        };
    }

    private static Map<GeneticAttribute, Double> attributesOf(AbstractHorse horse) {
        EnumMap<GeneticAttribute, Double> values = new EnumMap<>(GeneticAttribute.class);
        for (GeneticAttribute attr : GeneticAttribute.values()) {
            values.put(attr, HorseAttributes.get(horse, attr));
        }
        return values;
    }

    /**
     * Runs before vanilla's feeding ({@code Player.interactOn} posts this event before {@code Entity.interact}). Records
     * a golden carrot / golden apple exactly when vanilla's {@code handleEating} is about to call {@code setInLove}
     * (tamed adult, age 0, not in love, not ridden, not opening the inventory). If the feeding ends up not starting love
     * the record is dropped on the next tick.
     */
    private static boolean onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()
                || !(event.getTarget() instanceof AbstractHorse horse)
                || !HorseshoeAccess.isEligible(horse)) {
            return false;
        }
        Player player = event.getEntity();
        if (player.isSecondaryUseActive() || horse.isVehicle() || horse.isBaby() || !horse.isTamed() || horse.isInLove() || horse.getAge() != 0) {
            return false;
        }
        BreedingDietItem diet = BreedingDietItem.fromStack(player.getItemInHand(event.getHand()));
        if (diet == BreedingDietItem.GOLDEN_CARROT || diet == BreedingDietItem.GOLDEN_APPLE) {
            recordDietItem(horse, diet);
        }
        return false;
    }

    /** Drops a diet record once its love mode is over, so it can never guide a later, unrelated breeding. */
    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse
                && !horse.level().isClientSide()
                && !horse.isInLove()
                && horse.getPersistentData().contains(DIET_KEY)) {
            horse.getPersistentData().remove(DIET_KEY);
        }
    }

    private static boolean onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof AbstractHorse parentA)
                || !(event.getParentB() instanceof AbstractHorse parentB)
                || !(event.getChild() instanceof AbstractHorse child)
                || !HorseshoeAccess.isEligible(child)) {
            return false;
        }
        EnumSet<GeneticAttribute> favored = consumeBreedingDiet(parentA, parentB);
        RandomSource random = parentA.getRandom();
        applyOffspringAttributes(parentA, parentB, child, favored, random);
        applyOffspringTraits(parentA, parentB, child, random);
        return false;
    }
}
