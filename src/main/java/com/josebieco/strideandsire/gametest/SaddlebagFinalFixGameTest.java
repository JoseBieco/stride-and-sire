package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/**
 * Fixes from the final whole-branch review. Test instances:
 * data/strideandsire/test_instance/saddlebag_fix_*.json
 */
public final class SaddlebagFinalFixGameTest {
    private SaddlebagFinalFixGameTest() {}

    private static final List<RegistryObject<? extends Item>> BAGS = List.of(ModItems.LEATHER_SADDLEBAG,
        ModItems.IRON_SADDLEBAG, ModItems.GOLD_SADDLEBAG, ModItems.NETHERITE_SADDLEBAG);

    private static ItemStack leatherBagWithItems() {
        ItemStack bag = new ItemStack(ModItems.LEATHER_SADDLEBAG.get());
        SaddlebagItem.setContents(bag, List.of(new ItemStack(Items.DIAMOND, 5), ItemStack.EMPTY,
            new ItemStack(Items.APPLE, 3)));
        return bag;
    }

    private static int countItem(GameTestHelper helper, Item item) {
        return helper.getEntities(EntityTypes.ITEM).stream().filter(e -> e.getItem().is(item))
            .mapToInt(e -> e.getItem().getCount()).sum();
    }

    /** No bag-in-bundle (and no bag-in-shulker-box) nesting: every tier refuses to go inside container items. */
    public static void bundleRefusesSaddlebag(GameTestHelper helper) {
        for (RegistryObject<? extends Item> holder : BAGS) {
            ItemStack bag = new ItemStack(holder.get());
            String name = holder.getId().toString();
            helper.assertTrue(!bag.getItem().canFitInsideContainerItems(), name + " must not fit inside container items");
            helper.assertTrue(!BundleContents.canItemBeInBundle(bag), name + " must not be accepted by a bundle");
            BundleContents.Mutable bundle = new BundleContents.Mutable();
            helper.assertValueEqual(bundle.tryInsert(bag.copy()), 0, name + " items inserted into a bundle");
            ShulkerBoxSlot shulkerSlot = new ShulkerBoxSlot(new SimpleContainer(27), 0, 0, 0);
            helper.assertTrue(!shulkerSlot.mayPlace(bag), name + " must not be placeable in a shulker box slot");
        }
        helper.succeed();
    }

    /** A saddlebag item entity destroyed by lava damage spills its contents as item entities (bundle/shulker style). */
    public static void destroyedBagSpillsContents(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        ItemEntity bagEntity = new ItemEntity(level, pos.x, pos.y, pos.z, leatherBagWithItems());
        level.addFreshEntity(bagEntity);

        bagEntity.hurtServer(level, level.damageSources().lava(), 1000.0F);
        helper.assertTrue(bagEntity.isRemoved(), "bag item entity should be destroyed by lava damage");
        helper.assertValueEqual(countItem(helper, Items.DIAMOND), 5, "spilled diamonds");
        helper.assertValueEqual(countItem(helper, Items.APPLE), 3, "spilled apples");
        helper.succeed();
    }

    /**
     * A horse that enters the level (e.g. chunk load) carrying a saddlebag but no saddle gets corrected on the next
     * server tick - not synchronously during the join - while a saddled horse keeps its bag.
     */
    public static void joinWithBagButNoSaddleDropsNextTick(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        BlockPos unsaddledPos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos saddledPos = helper.absolutePos(new BlockPos(3, 1, 3));

        Horse unsaddled = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        Horse saddled = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(unsaddled != null && saddled != null, "could not create horses");
        unsaddled.snapTo(unsaddledPos.getX() + 0.5, unsaddledPos.getY(), unsaddledPos.getZ() + 0.5);
        saddled.snapTo(saddledPos.getX() + 0.5, saddledPos.getY(), saddledPos.getZ() + 0.5);
        saddled.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        SaddlebagAccess.set(unsaddled, leatherBagWithItems());
        SaddlebagAccess.set(saddled, new ItemStack(ModItems.IRON_SADDLEBAG.get()));

        level.addFreshEntity(unsaddled);
        level.addFreshEntity(saddled);
        helper.assertTrue(!SaddlebagAccess.get(unsaddled).isEmpty(), "bag must not be dropped synchronously on join");
        helper.assertValueEqual(helper.getEntities(EntityTypes.ITEM).size(), 0, "item entities right after join");

        helper.runAfterDelay(3, () -> {
            helper.assertTrue(SaddlebagAccess.get(unsaddled).isEmpty(), "unsaddled horse should have dropped its bag");
            List<ItemEntity> drops = helper.getEntities(EntityTypes.ITEM).stream()
                .filter(e -> e.getItem().is(ModItems.LEATHER_SADDLEBAG.get())).toList();
            helper.assertValueEqual(drops.size(), 1, "dropped leather saddlebag entities");
            List<ItemStack> contents = SaddlebagItem.getContents(drops.get(0).getItem());
            helper.assertTrue(contents.get(0).is(Items.DIAMOND) && contents.get(0).getCount() == 5,
                "dropped bag slot 0: " + contents.get(0));
            helper.assertTrue(SaddlebagAccess.get(saddled).is(ModItems.IRON_SADDLEBAG.get()),
                "saddled horse must keep its bag");
            unsaddled.discard();
            saddled.discard();
            helper.succeed();
        });
    }
}
