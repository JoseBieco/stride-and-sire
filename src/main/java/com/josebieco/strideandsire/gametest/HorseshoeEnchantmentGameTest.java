package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.enchantment.HorseshoeEnchantments;
import com.josebieco.strideandsire.event.HorseshoeEnchantmentEffectHandler;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeEnchantmentKind;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeItem;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeMaterial;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Task 7 enchantments. Test instances: data/strideandsire/test_instance/horseshoe_enchant_*.json
 */
public final class HorseshoeEnchantmentGameTest {
    private HorseshoeEnchantmentGameTest() {}

    private static final List<ResourceKey<Enchantment>> HORSESHOE_ENCHANTMENTS =
        List.of(HorseshoeEnchantments.FEATHER_STEP, HorseshoeEnchantments.FROST_STEP, HorseshoeEnchantments.LAVA_WALKER);

    // ---- helpers -----------------------------------------------------------------------------------------------

    private static Holder<Enchantment> holder(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
            .orElseThrow(() -> helper.assertionException(Component.literal("enchantment not registered: " + key.identifier())));
    }

    private static ItemStack shoe(GameTestHelper helper, Item item, ResourceKey<Enchantment> enchantment) {
        ItemStack stack = new ItemStack(item);
        if (enchantment != null) {
            stack.enchant(holder(helper, enchantment), 1);
        }
        return stack;
    }

    private static Item itemFor(HorseshoeMaterial material) {
        return switch (material) {
            case COPPER -> ModItems.COPPER_HORSESHOE.get();
            case IRON -> ModItems.IRON_HORSESHOE.get();
            case GOLD -> ModItems.GOLD_HORSESHOE.get();
            case NETHERITE -> ModItems.NETHERITE_HORSESHOE.get();
        };
    }

    private static ResourceKey<Enchantment> keyFor(HorseshoeEnchantmentKind kind) {
        return switch (kind) {
            case FEATHER_STEP -> HorseshoeEnchantments.FEATHER_STEP;
            case FROST_STEP -> HorseshoeEnchantments.FROST_STEP;
            case LAVA_WALKER -> HorseshoeEnchantments.LAVA_WALKER;
        };
    }

    /** A horse that stays put: no AI goals (so no wandering / FloatGoal), but full physics. */
    private static Horse stillHorse(GameTestHelper helper, int x, int y, int z) {
        Horse horse = helper.spawn(EntityTypes.HORSE, x, y, z);
        horse.goalSelector.removeAllGoals(goal -> true);
        return horse;
    }

    /** Runs a survival anvil combination of {@code input} + {@code addition} and returns the output slot. */
    private static ItemStack anvil(GameTestHelper helper, ItemStack input, ItemStack addition) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AnvilMenu menu = new AnvilMenu(0, player.getInventory());
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(input);
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(addition);
        menu.createResult();
        return menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
    }

    // ---- (a) / (b): eligibility gate + vanilla's own check -----------------------------------------------------

    /** (a) Frost Step on Netherite is rejected by the gate, by vanilla's canEnchant and by a real anvil. */
    public static void frostStepRejectedOnNetherite(GameTestHelper helper) {
        Holder<Enchantment> frost = holder(helper, HorseshoeEnchantments.FROST_STEP);
        ItemStack netherite = new ItemStack(ModItems.NETHERITE_HORSESHOE.get());
        ItemStack iron = new ItemStack(ModItems.IRON_HORSESHOE.get());
        helper.assertTrue(!HorseshoeEnchantments.isEligible(netherite, HorseshoeEnchantments.FROST_STEP),
            "gate must reject frost_step on netherite");
        helper.assertTrue(!frost.value().canEnchant(netherite), "vanilla canEnchant must reject frost_step on netherite");
        helper.assertTrue(!frost.value().isPrimaryItem(netherite), "enchanting table must not offer frost_step on netherite");
        helper.assertTrue(frost.value().canEnchant(iron), "control: frost_step must be accepted on iron");

        ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(frost, 1));
        helper.assertTrue(anvil(helper, netherite.copy(), book.copy()).isEmpty(),
            "anvil must not produce a netherite horseshoe with frost_step");
        ItemStack ironResult = anvil(helper, iron.copy(), book.copy());
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(frost, ironResult) == 1,
            "control: anvil must apply frost_step to an iron horseshoe");
        helper.succeed();
    }

    /** (b) Lava Walker on Iron is rejected by the gate, by vanilla's canEnchant and by a real anvil. */
    public static void lavaWalkerRejectedOnIron(GameTestHelper helper) {
        Holder<Enchantment> lava = holder(helper, HorseshoeEnchantments.LAVA_WALKER);
        ItemStack iron = new ItemStack(ModItems.IRON_HORSESHOE.get());
        ItemStack netherite = new ItemStack(ModItems.NETHERITE_HORSESHOE.get());
        helper.assertTrue(!HorseshoeEnchantments.isEligible(iron, HorseshoeEnchantments.LAVA_WALKER),
            "gate must reject lava_walker on iron");
        helper.assertTrue(!lava.value().canEnchant(iron), "vanilla canEnchant must reject lava_walker on iron");
        helper.assertTrue(!lava.value().isPrimaryItem(iron), "enchanting table must not offer lava_walker on iron");
        helper.assertTrue(lava.value().canEnchant(netherite), "control: lava_walker must be accepted on netherite");

        ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(lava, 1));
        helper.assertTrue(anvil(helper, iron.copy(), book.copy()).isEmpty(),
            "anvil must not produce an iron horseshoe with lava_walker");
        ItemStack netheriteResult = anvil(helper, netherite.copy(), book.copy());
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(lava, netheriteResult) == 1,
            "control: anvil must apply lava_walker to a netherite horseshoe");
        helper.succeed();
    }

    /**
     * The item tags used by the enchantment JSONs, the Java gate and {@link HorseshoeMaterial#allowsEnchantment}
     * agree for every material x enchantment, and horseshoes accept Unbreaking/Mending but no unrelated enchantment.
     */
    public static void tagsMatchEligibilityMatrix(GameTestHelper helper) {
        for (HorseshoeMaterial material : HorseshoeMaterial.values()) {
            ItemStack stack = new ItemStack(itemFor(material));
            helper.assertTrue(stack.getItem() instanceof HorseshoeItem h && h.getMaterial() == material,
                "item/material mismatch for " + material);
            for (HorseshoeEnchantmentKind kind : HorseshoeEnchantmentKind.values()) {
                ResourceKey<Enchantment> key = keyFor(kind);
                Enchantment enchantment = holder(helper, key).value();
                boolean expected = material.allowsEnchantment(kind);
                String where = material + " x " + key.identifier();
                helper.assertValueEqual(HorseshoeEnchantments.isEligible(stack, key), expected, "gate " + where);
                helper.assertValueEqual(enchantment.canEnchant(stack), expected, "supported_items " + where);
                helper.assertValueEqual(enchantment.isPrimaryItem(stack), expected, "primary_items " + where);
                helper.assertValueEqual(stack.canApplyAtEnchantingTable(holder(helper, key)), expected, "table " + where);
            }
            for (ResourceKey<Enchantment> generic : List.of(Enchantments.UNBREAKING, Enchantments.MENDING)) {
                helper.assertTrue(holder(helper, generic).value().canEnchant(stack), generic.identifier() + " must apply to " + material);
            }
            for (ResourceKey<Enchantment> unrelated : List.of(Enchantments.FEATHER_FALLING, Enchantments.FROST_WALKER,
                    Enchantments.PROTECTION, Enchantments.SHARPNESS, Enchantments.EFFICIENCY, Enchantments.SOUL_SPEED)) {
                helper.assertTrue(!holder(helper, unrelated).value().canEnchant(stack),
                    unrelated.identifier() + " must not apply to " + material);
            }
        }
        // Horseshoe enchantments never apply to non-horseshoes.
        for (ResourceKey<Enchantment> key : HORSESHOE_ENCHANTMENTS) {
            ItemStack boots = new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS);
            helper.assertTrue(!holder(helper, key).value().canEnchant(boots), key.identifier() + " must not apply to boots");
            helper.assertTrue(!HorseshoeEnchantments.isEligible(boots, key), "gate must reject boots for " + key.identifier());
        }
        helper.assertTrue(!Enchantment.areCompatible(holder(helper, HorseshoeEnchantments.FROST_STEP),
                holder(helper, HorseshoeEnchantments.LAVA_WALKER)), "frost_step and lava_walker must be mutually exclusive");
        helper.succeed();
    }

    // ---- (c) Feather Step ---------------------------------------------------------------------------------------

    /** (c) A feather-step horse (and its rider) take no fall damage; plain horses and their riders do. */
    public static void featherStepNegatesFallDamage(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Horse feather = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        Horse plain = helper.spawn(EntityTypes.HORSE, 3, 1, 3);
        HorseshoeAccess.set(feather, shoe(helper, ModItems.IRON_HORSESHOE.get(), HorseshoeEnchantments.FEATHER_STEP));
        HorseshoeAccess.set(plain, shoe(helper, ModItems.IRON_HORSESHOE.get(), null));
        Player featherRider = helper.makeMockPlayer(GameType.SURVIVAL);
        Player plainRider = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            helper.assertTrue(featherRider.startRiding(feather, true, false), "could not mount feather horse");
            helper.assertTrue(plainRider.startRiding(plain, true, false), "could not mount plain horse");
            float horseStart = feather.getHealth();
            float riderStart = featherRider.getHealth();

            helper.assertTrue(!feather.causeFallDamage(20.0, 1.0F, level.damageSources().fall()),
                "feather-step horse must not be hurt by a 20 block fall");
            helper.assertValueEqual(feather.getHealth(), horseStart, "feather-step horse health");
            helper.assertValueEqual(featherRider.getHealth(), riderStart, "rider of feather-step horse health");
            featherRider.causeFallDamage(20.0, 1.0F, level.damageSources().fall());
            helper.assertValueEqual(featherRider.getHealth(), riderStart, "rider of feather-step horse health (own fall)");

            float plainStart = plain.getHealth();
            float plainRiderStart = plainRider.getHealth();
            helper.assertTrue(plain.causeFallDamage(20.0, 1.0F, level.damageSources().fall()),
                "control: plain horse must be hurt by a 20 block fall");
            helper.assertTrue(plain.getHealth() < plainStart, "control: plain horse health did not drop");
            helper.assertTrue(plainRider.getHealth() < plainRiderStart, "control: rider of plain horse health did not drop");
        } finally {
            featherRider.stopRiding();
            plainRider.stopRiding();
        }
        helper.succeed();
    }

    // ---- (d) Frost Step -----------------------------------------------------------------------------------------

    /** Frost Walker II radius (vanilla: clamped linear 3 + 1 per level above first, at level 2). */
    private static final double FROST_WALKER_II_RADIUS = 4.0;

    /**
     * Pool layout in the 11x5x11 structure at relative y=0: stone rim, stone path z=5 x=4..6, water elsewhere.
     */
    private static void buildPool(GameTestHelper helper, Block fluidBlock) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                boolean rim = x == 0 || z == 0 || x == 10 || z == 10;
                boolean path = z == 5 && x >= 4 && x <= 6;
                helper.setBlock(x, 0, z, rim || path ? Blocks.STONE : fluidBlock);
            }
        }
    }

    /** Asserts every pool block is frosted ice iff its center lies strictly within the Frost Walker II disk. */
    private static void assertFrozenDisk(GameTestHelper helper, AbstractHorse horse) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        double hx = horse.getX() - origin.getX();
        double hz = horse.getZ() - origin.getZ();
        for (int x = 1; x <= 9; x++) {
            for (int z = 1; z <= 9; z++) {
                if (z == 5 && x >= 4 && x <= 6) {
                    continue;
                }
                double dx = x + 0.5 - hx;
                double dz = z + 0.5 - hz;
                boolean inside = dx * dx + dz * dz < FROST_WALKER_II_RADIUS * FROST_WALKER_II_RADIUS;
                Block expected = inside ? Blocks.FROSTED_ICE : Blocks.WATER;
                Block actual = helper.getBlockState(new BlockPos(x, 0, z)).getBlock();
                helper.assertTrue(actual == expected, "at (" + x + ",0," + z + ") dist^2=" + (dx * dx + dz * dz)
                    + " expected " + expected + " but was " + actual);
            }
        }
    }

    /** (d) Frost Step freezes water in the vanilla Frost Walker II disk around the hooves, following the horse. */
    public static void frostStepFreezesWaterLikeFrostWalkerII(GameTestHelper helper) {
        buildPool(helper, Blocks.WATER);
        Horse horse = stillHorse(helper, 5, 1, 5);
        HorseshoeAccess.set(horse, shoe(helper, ModItems.COPPER_HORSESHOE.get(), HorseshoeEnchantments.FROST_STEP));

        helper.startSequence()
            .thenWaitUntil(() -> {
                helper.assertTrue(horse.onGround(), "horse not on ground yet");
                assertFrozenDisk(helper, horse);
            })
            .thenExecute(() -> {
                // (9,0,5) is exactly on the radius from x=5.5 (not frozen); moving one block east must freeze it.
                helper.assertBlockPresent(Blocks.WATER, 9, 0, 5);
                BlockPos target = helper.absolutePos(new BlockPos(6, 1, 5));
                horse.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            })
            .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.FROSTED_ICE, 9, 0, 5))
            .thenExecute(() -> helper.assertTrue(
                helper.getLevel().getBlockTicks().hasScheduledTick(helper.absolutePos(new BlockPos(9, 0, 5)), Blocks.FROSTED_ICE),
                "frosted ice must melt through its own vanilla block tick"))
            .thenSucceed();
    }

    /** (f) The frost-step horse dies / is removed mid-test: no exception, no tracking state left, ice decays by block ticks. */
    public static void frostStepHorseRemovedMidTick(GameTestHelper helper) {
        buildPool(helper, Blocks.WATER);
        Horse killed = stillHorse(helper, 5, 1, 5);
        HorseshoeAccess.set(killed, shoe(helper, ModItems.IRON_HORSESHOE.get(), HorseshoeEnchantments.FROST_STEP));
        Horse ridden = stillHorse(helper, 4, 1, 5);
        HorseshoeAccess.set(ridden, shoe(helper, ModItems.COPPER_HORSESHOE.get(), HorseshoeEnchantments.FROST_STEP));
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.addCleanup(passed -> rider.stopRiding());

        helper.startSequence()
            .thenWaitUntil(() -> {
                helper.assertBlockPresent(Blocks.FROSTED_ICE, 5, 0, 4);
                helper.assertTrue(HorseshoeEnchantmentEffectHandler.isTrackingFrostPosition(killed), "killed horse not tracked yet");
                helper.assertTrue(HorseshoeEnchantmentEffectHandler.isTrackingFrostPosition(ridden), "ridden horse not tracked yet");
            })
            .thenExecute(() -> {
                helper.assertTrue(rider.startRiding(ridden, true, false), "could not mount horse");
                killed.kill(helper.getLevel());
                rider.stopRiding();
                ridden.discard();
            })
            .thenIdle(5)
            .thenExecute(() -> {
                helper.assertTrue(!killed.isAlive(), "horse should be dead");
                helper.assertTrue(ridden.isRemoved(), "horse should be removed");
                helper.assertTrue(!HorseshoeEnchantmentEffectHandler.isTrackingFrostPosition(killed),
                    "dead horse still tracked by the frost handler");
                helper.assertTrue(!HorseshoeEnchantmentEffectHandler.isTrackingFrostPosition(ridden),
                    "removed horse still tracked by the frost handler");
                helper.assertTrue(helper.getLevel().getBlockTicks().hasScheduledTick(
                        helper.absolutePos(new BlockPos(5, 0, 4)), Blocks.FROSTED_ICE),
                    "ice decay must be a vanilla block tick");
            })
            .thenSucceed();
    }

    // ---- (e) Lava Walker ----------------------------------------------------------------------------------------

    /** (e) A Lava-Walker netherite horse stands on the lava surface like a Strider; a plain netherite horse sinks. */
    public static void lavaWalkerStandsOnLava(GameTestHelper helper) {
        buildPool(helper, Blocks.LAVA);
        ServerLevel level = helper.getLevel();
        Horse walker = stillHorse(helper, 2, 2, 2);
        HorseshoeAccess.set(walker, shoe(helper, ModItems.NETHERITE_HORSESHOE.get(), HorseshoeEnchantments.LAVA_WALKER));
        Horse sinker = stillHorse(helper, 8, 2, 8);
        HorseshoeAccess.set(sinker, shoe(helper, ModItems.NETHERITE_HORSESHOE.get(), null));

        BlockPos lavaPos = helper.absolutePos(new BlockPos(2, 0, 2));
        double surface = lavaPos.getY() + level.getFluidState(lavaPos).getHeight(level, lavaPos);
        double tolerance = 1.0E-3;

        helper.startSequence()
            .thenIdle(20)
            .thenExecuteFor(40, () -> {
                helper.assertTrue(walker.isAlive(), "walker died");
                helper.assertTrue(walker.getY() >= surface - tolerance,
                    "walker sank: y=" + walker.getY() + " surface=" + surface);
                helper.assertTrue(!walker.isInLava(), "walker must stand on, not in, the lava");
            })
            .thenExecute(() -> helper.assertTrue(sinker.getY() < surface - 0.3,
                "control: plain netherite horse should sink, y=" + sinker.getY() + " surface=" + surface))
            .thenSucceed();
    }
}
