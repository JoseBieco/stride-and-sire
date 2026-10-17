package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.SaddlebagAccess;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagItem;
import com.josebieco.strideandsire.item.saddlebag.SaddlebagMaterial;
import com.josebieco.strideandsire.registry.ModItems;
import com.josebieco.strideandsire.render.SaddlebagRendering;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;
import org.jspecify.annotations.Nullable;

/**
 * Visibility predicate of the saddlebag render layer ({@code client.SaddlebagLayer} delegates to
 * {@link SaddlebagRendering#materialToRender}). GameTest cannot assert rendered pixels, so this checks the seam that
 * decides whether, and in which tier, the bags are drawn.
 *
 * <p>Test instance: data/strideandsire/test_instance/saddlebag_layer_material_to_render_matches_equipped_tier.json
 */
public final class SaddlebagLayerVisibilityGameTest {
    private SaddlebagLayerVisibilityGameTest() {}

    public static void materialToRenderMatchesEquippedTier(GameTestHelper helper) {
        helper.makeFloor();
        List<RegistryObject<SaddlebagItem>> bags = List.of(
            ModItems.LEATHER_SADDLEBAG, ModItems.IRON_SADDLEBAG, ModItems.DIAMOND_SADDLEBAG, ModItems.GOLD_SADDLEBAG, ModItems.NETHERITE_SADDLEBAG);
        for (EntityType<? extends AbstractHorse> type : List.of(
                EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE)) {
            AbstractHorse horse = helper.spawn(type, 2, 1, 2);
            horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            assertMaterial(helper, horse, null, type + " without saddlebag");
            for (RegistryObject<SaddlebagItem> bag : bags) {
                SaddlebagAccess.set(horse, new ItemStack(bag.get()));
                assertMaterial(helper, horse, bag.get().getMaterial(), type + " with " + bag.getId());
            }
            SaddlebagAccess.set(horse, ItemStack.EMPTY);
            assertMaterial(helper, horse, null, type + " after removing saddlebag");
        }
        helper.succeed();
    }

    /**
     * Chest-aware placement of the 3D bags: a chested Donkey/Mule gets the under-chest variant, everything else (and a
     * Donkey/Mule without chest) the flank one. Test instance:
     * data/strideandsire/test_instance/saddlebag_layer_placement_follows_chest.json
     */
    public static void placementFollowsChest(GameTestHelper helper) {
        helper.makeFloor();
        for (EntityType<? extends AbstractHorse> type : List.of(
                EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE)) {
            AbstractHorse horse = helper.spawn(type, 2, 1, 2);
            assertPlacement(helper, horse, SaddlebagRendering.Placement.FLANK, type + " without chest");
            if (horse instanceof AbstractChestedHorse chested) {
                chested.setChest(true);
                assertPlacement(helper, horse, SaddlebagRendering.Placement.UNDER_CHEST, type + " with chest");
                chested.setChest(false);
                assertPlacement(helper, horse, SaddlebagRendering.Placement.FLANK, type + " after removing chest");
            }
        }
        helper.succeed();
    }

    private static void assertPlacement(GameTestHelper helper, AbstractHorse horse, SaddlebagRendering.Placement expected, String what) {
        SaddlebagRendering.Placement actual = SaddlebagRendering.placement(horse);
        helper.assertTrue(actual == expected, what + ": expected " + expected + " but got " + actual);
    }

    /** Null-safe (GameTestHelper#assertValueEqual NPEs on a null actual value). */
    private static void assertMaterial(GameTestHelper helper, AbstractHorse horse, @Nullable SaddlebagMaterial expected, String what) {
        SaddlebagMaterial actual = SaddlebagRendering.materialToRender(horse);
        helper.assertTrue(actual == expected, what + ": expected " + expected + " but got " + actual);
    }
}
