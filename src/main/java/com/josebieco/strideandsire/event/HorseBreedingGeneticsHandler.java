package com.josebieco.strideandsire.event;

import com.josebieco.strideandsire.attachment.HorseGeneticsAccess;
import com.josebieco.strideandsire.attachment.HorseGeneticsAttachment;
import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.genetics.AttributeInheritance;
import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.genetics.HorseAttributeRanges;
import com.josebieco.strideandsire.genetics.HorseAttributes;
import com.josebieco.strideandsire.genetics.LineageTrait;
import com.josebieco.strideandsire.genetics.TraitInheritance;
import com.josebieco.strideandsire.genetics.TraitState;
import com.josebieco.strideandsire.item.genetics.BreedingDietItem;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Hooks horse feeding and foal creation ({@link BabyEntitySpawnEvent}) to apply diet-guided or unguided attribute
 * inheritance (Task 4) and lineage trait inheritance (Task 5).
 */
public final class HorseBreedingGeneticsHandler {
    private static final Map<UUID, BreedingDietItem> ACTIVE_DIET_BY_HORSE = new ConcurrentHashMap<>();

    private HorseBreedingGeneticsHandler() {}

    public static void register() {
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(HorseBreedingGeneticsHandler::onEntityInteractSpecific);
        VanillaGameEvent.BUS.addListener(HorseBreedingGeneticsHandler::onVanillaGameEvent);
        BabyEntitySpawnEvent.BUS.addListener(HorseBreedingGeneticsHandler::onBabyEntitySpawn);
    }

    /** Records the diet item that put {@code horse} into love mode for its upcoming breeding. */
    public static void recordDietItem(AbstractHorse horse, BreedingDietItem item) {
        if (!HorseshoeAccess.isEligible(horse)) {
            return;
        }
        if (item == null || item == BreedingDietItem.NONE) {
            ACTIVE_DIET_BY_HORSE.remove(horse.getUUID());
        } else {
            ACTIVE_DIET_BY_HORSE.put(horse.getUUID(), item);
        }
    }

    /** Returns the currently recorded diet item for {@code horse}, or {@link BreedingDietItem#NONE}. */
    public static BreedingDietItem getRecordedDietItem(AbstractHorse horse) {
        if (!horse.isInLove()) {
            ACTIVE_DIET_BY_HORSE.remove(horse.getUUID());
            return BreedingDietItem.NONE;
        }
        return ACTIVE_DIET_BY_HORSE.getOrDefault(horse.getUUID(), BreedingDietItem.NONE);
    }

    /** Resolves and consumes the diet item active for a breeding pair. */
    public static BreedingDietItem consumeBreedingDiet(AbstractHorse parentA, AbstractHorse parentB) {
        BreedingDietItem dietA = ACTIVE_DIET_BY_HORSE.remove(parentA.getUUID());
        BreedingDietItem dietB = ACTIVE_DIET_BY_HORSE.remove(parentB.getUUID());
        if (dietA != null && dietA != BreedingDietItem.NONE) {
            return dietA;
        }
        if (dietB != null && dietB != BreedingDietItem.NONE) {
            return dietB;
        }
        return BreedingDietItem.NONE;
    }

    /** Applies the Fase C attribute inheritance rules to {@code child} from {@code parentA} and {@code parentB}. */
    public static void applyOffspringAttributes(
            AbstractHorse parentA,
            AbstractHorse parentB,
            AbstractHorse child,
            BreedingDietItem dietItem,
            RandomSource random) {
        boolean dietGuided = dietItem != null && dietItem.isDietGuided();
        for (GeneticAttribute attr : GeneticAttribute.values()) {
            HorseAttributeRanges range = HorseAttributeRanges.of(attr);
            double p1 = HorseAttributes.get(parentA, attr);
            double p2 = HorseAttributes.get(parentB, attr);
            boolean favored = dietGuided && dietItem.favors(attr);
            double childVal = AttributeInheritance.rollChildValue(p1, p2, range, favored, dietGuided, random);
            HorseAttributes.setBase(child, attr, childVal);
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
        EnumMap<LineageTrait, TraitState> childTraits = new EnumMap<>(LineageTrait.class);
        for (LineageTrait trait : LineageTrait.values()) {
            TraitState rolled = TraitInheritance.rollChildState(genA.state(trait), genB.state(trait), random);
            childTraits.put(trait, rolled);
        }
        HorseGeneticsAccess.set(child, HorseGeneticsAttachment.of(childTraits));
    }

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
        ItemStack stack = player.getItemInHand(event.getHand());
        BreedingDietItem diet = BreedingDietItem.fromStack(stack);
        if (diet.isDietGuided()) {
            recordDietItem(horse, diet);
        }
        return false;
    }

    private static boolean onVanillaGameEvent(VanillaGameEvent event) {
        if (event.getLevel().isClientSide()
                || event.getVanillaEvent() != GameEvent.EAT.value()
                || !(event.getCause() instanceof AbstractHorse horse)
                || !HorseshoeAccess.isEligible(horse)
                || !horse.isInLove()) {
            return false;
        }
        if (ACTIVE_DIET_BY_HORSE.containsKey(horse.getUUID())) {
            return false;
        }
        ServerPlayer loveCause = horse.getLoveCause();
        if (loveCause != null) {
            for (InteractionHand hand : InteractionHand.values()) {
                BreedingDietItem diet = BreedingDietItem.fromStack(loveCause.getItemInHand(hand));
                if (diet.isDietGuided()) {
                    recordDietItem(horse, diet);
                    break;
                }
            }
        }
        return false;
    }

    private static boolean onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof AbstractHorse parentA)
                || !(event.getParentB() instanceof AbstractHorse parentB)
                || !(event.getChild() instanceof AbstractHorse child)
                || !HorseshoeAccess.isEligible(child)) {
            return false;
        }
        BreedingDietItem diet = consumeBreedingDiet(parentA, parentB);
        RandomSource random = parentA.getRandom();
        applyOffspringAttributes(parentA, parentB, child, diet, random);
        applyOffspringTraits(parentA, parentB, child, random);
        return false;
    }
}
