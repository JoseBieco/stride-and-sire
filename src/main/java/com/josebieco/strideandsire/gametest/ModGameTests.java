package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.StrideAndSire;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;

/**
 * GameTest harness for Minecraft/Forge 26.3.
 *
 * <p>Since 1.21.5 vanilla GameTests are data-driven, and Forge 66 does not wire its
 * {@code @GameTest} annotation into registration. A test therefore has two halves:
 * <ol>
 *   <li>a <b>test function</b> ({@code Consumer<GameTestHelper>}) registered here in the
 *       static {@code minecraft:test_function} registry, and</li>
 *   <li>a <b>test instance</b> JSON at
 *       {@code data/strideandsire/test_instance/<name>.json} (datapack registry
 *       {@code minecraft:test_instance}) pointing at that function, the structure
 *       (normally {@code strideandsire:empty}) and the max tick budget.</li>
 * </ol>
 * Run all mod tests headlessly with {@code ./gradlew runGameTestServer}; the server
 * exits with the number of failed required tests, so a failure fails the Gradle task.
 */
public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, StrideAndSire.MODID);

    static {
        TEST_FUNCTIONS.register("horseshoe_items_are_registered_with_correct_material",
            () -> HorseshoeItemGameTest::horseshoeItemsAreRegisteredWithCorrectMaterial);
        TEST_FUNCTIONS.register("horseshoe_attachment_set_then_get_on_horse",
            () -> HorseshoeAttachmentGameTest::setThenGetOnHorse);
        TEST_FUNCTIONS.register("horseshoe_attachment_all_eligible_types_store_horseshoe",
            () -> HorseshoeAttachmentGameTest::allEligibleTypesStoreHorseshoe);
        TEST_FUNCTIONS.register("horseshoe_attachment_persists_across_save_and_load",
            () -> HorseshoeAttachmentGameTest::persistsAcrossSaveAndLoad);
        TEST_FUNCTIONS.register("horseshoe_attachment_ineligible_entities_have_no_horseshoe",
            () -> HorseshoeAttachmentGameTest::ineligibleEntitiesHaveNoHorseshoe);
        TEST_FUNCTIONS.register("horseshoe_attachment_drops_horseshoe_on_death",
            () -> HorseshoeAttachmentGameTest::dropsHorseshoeOnDeath);
        TEST_FUNCTIONS.register("horseshoe_menu_slot_accepts_only_horseshoes",
            () -> HorseshoeMenuGameTest::slotAcceptsOnlyHorseshoes);
        TEST_FUNCTIONS.register("horseshoe_menu_keeps_vanilla_slot_layout",
            () -> HorseshoeMenuGameTest::keepsVanillaSlotLayout);
        TEST_FUNCTIONS.register("horseshoe_menu_quick_move_and_place_route_to_storage",
            () -> HorseshoeMenuGameTest::quickMoveAndPlaceRouteToStorage);
        TEST_FUNCTIONS.register("horseshoe_passive_gold_speed_modifier",
            () -> HorseshoePassiveEffectGameTest::goldSpeedModifier);
        TEST_FUNCTIONS.register("horseshoe_passive_netherite_fire_immunity",
            () -> HorseshoePassiveEffectGameTest::netheriteFireImmunity);
        TEST_FUNCTIONS.register("horseshoe_passive_speed_modifier_isolation",
            () -> HorseshoePassiveEffectGameTest::speedModifierIsolation);
        TEST_FUNCTIONS.register("horseshoe_passive_piglin_neutral_when_riding_gold",
            () -> HorseshoePassiveEffectGameTest::piglinNeutralWhenRidingGold);
        TEST_FUNCTIONS.register("horseshoe_passive_piglin_target_suppressed",
            () -> HorseshoePassiveEffectGameTest::piglinTargetSuppressed);
        TEST_FUNCTIONS.register("horseshoe_passive_gold_speed_survives_reload",
            () -> HorseshoePassiveEffectGameTest::goldSpeedSurvivesReload);
    }

    private ModGameTests() {}

    public static void register(BusGroup modBusGroup) {
        TEST_FUNCTIONS.register(modBusGroup);
    }
}
