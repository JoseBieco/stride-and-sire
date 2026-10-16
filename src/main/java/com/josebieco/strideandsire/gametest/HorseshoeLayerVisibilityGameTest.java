package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.attachment.HorseshoeAccess;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeItem;
import com.josebieco.strideandsire.item.horseshoe.HorseshoeMaterial;
import com.josebieco.strideandsire.registry.ModItems;
import com.josebieco.strideandsire.render.HorseshoeRendering;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import org.jspecify.annotations.Nullable;

/**
 * Visibility predicate of the hoof render layer ({@code client.HorseshoeLayer} delegates to
 * {@link HorseshoeRendering#materialToRender}). GameTest cannot assert rendered pixels, so this checks the seam that
 * decides whether, and in which tier, the hooves are drawn.
 *
 * <p>Test instance: data/strideandsire/test_instance/horseshoe_layer_material_to_render_matches_equipped_tier.json
 */
public final class HorseshoeLayerVisibilityGameTest {
    private HorseshoeLayerVisibilityGameTest() {}

    public static void materialToRenderMatchesEquippedTier(GameTestHelper helper) {
        helper.makeFloor();
        List<RegistryObject<HorseshoeItem>> shoes = List.of(
            ModItems.COPPER_HORSESHOE, ModItems.IRON_HORSESHOE, ModItems.GOLD_HORSESHOE, ModItems.NETHERITE_HORSESHOE);
        for (EntityType<? extends AbstractHorse> type : List.of(
                EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE)) {
            AbstractHorse horse = helper.spawn(type, 2, 1, 2);
            assertMaterial(helper, horse, null, type + " without horseshoe");
            for (RegistryObject<HorseshoeItem> shoe : shoes) {
                HorseshoeAccess.set(horse, new ItemStack(shoe.get()));
                assertMaterial(helper, horse, shoe.get().getMaterial(), type + " with " + shoe.getId());
            }
            HorseshoeAccess.set(horse, ItemStack.EMPTY);
            assertMaterial(helper, horse, null, type + " after removing horseshoe");
        }
        helper.succeed();
    }

    /** Null-safe (GameTestHelper#assertValueEqual NPEs on a null actual value). */
    private static void assertMaterial(GameTestHelper helper, AbstractHorse horse, @Nullable HorseshoeMaterial expected, String what) {
        HorseshoeMaterial actual = HorseshoeRendering.materialToRender(horse);
        helper.assertTrue(actual == expected, what + ": expected " + expected + " but got " + actual);
    }
}
