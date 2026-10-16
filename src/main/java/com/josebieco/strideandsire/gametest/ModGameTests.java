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
    }

    private ModGameTests() {}

    public static void register(BusGroup modBusGroup) {
        TEST_FUNCTIONS.register(modBusGroup);
    }
}
