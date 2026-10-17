package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.event.HorseBreedingGeneticsHandler;
import com.josebieco.strideandsire.genetics.GeneticAttribute;
import com.josebieco.strideandsire.item.genetics.BreedingDietItem;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Test instance: data/strideandsire/test_instance/genetics_breeding_diet_record_lifecycle.json
 */
public final class HorseBreedingDietRecordGameTest {
    private HorseBreedingDietRecordGameTest() {}

    /**
     * Controller rulings on diet detection (I1/I2):
     * <ul>
     *   <li>each parent records the diet item that put it in love, and the record survives save/load while in love;</li>
     *   <li>a carrot-fed x apple-fed breeding is guided with the favored set = union {Speed, Health}; consuming it
     *       clears both records;</li>
     *   <li>an Enchanted Golden Apple starts love but is unguided;</li>
     *   <li>a stale record (love reset, or love timed out) never makes a later code-triggered {@code setInLove}
     *       breeding guided.</li>
     * </ul>
     */
    public static void dietRecordUnionEnchantedAppleStaleAndPersistence(GameTestHelper helper) {
        helper.makeFloor();
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        Horse carrotFed = tamedAdult(helper, 0, 0);
        Horse appleFed = tamedAdult(helper, 2, 0);
        Horse enchantedFed = tamedAdult(helper, 4, 0);
        Horse resetHorse = tamedAdult(helper, 0, 2);
        Horse timeoutHorse = tamedAdult(helper, 2, 2);
        Horse partner = tamedAdult(helper, 4, 2);
        List<Horse> all = List.of(carrotFed, appleFed, enchantedFed, resetHorse, timeoutHorse, partner);

        feed(player, carrotFed, Items.GOLDEN_CARROT);
        feed(player, appleFed, Items.GOLDEN_APPLE);
        feed(player, enchantedFed, Items.ENCHANTED_GOLDEN_APPLE);
        feed(player, resetHorse, Items.GOLDEN_CARROT);
        feed(player, timeoutHorse, Items.GOLDEN_CARROT);
        for (Horse horse : all) {
            if (horse != partner) {
                helper.assertTrue(horse.isInLove(), "feeding should start love mode");
            }
        }
        helper.assertValueEqual(HorseBreedingGeneticsHandler.getRecordedDietItem(carrotFed), BreedingDietItem.GOLDEN_CARROT,
            "carrot-fed horse diet record");
        helper.assertValueEqual(HorseBreedingGeneticsHandler.getRecordedDietItem(appleFed), BreedingDietItem.GOLDEN_APPLE,
            "apple-fed horse diet record");
        helper.assertValueEqual(HorseBreedingGeneticsHandler.getRecordedDietItem(enchantedFed), BreedingDietItem.NONE,
            "Enchanted Golden Apple must not count as a diet item");

        // Persisted with the love state.
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        carrotFed.saveWithoutId(output);
        CompoundTag tag = output.buildResult();
        Horse reloaded = EntityTypes.HORSE.create(level, EntitySpawnReason.LOAD);
        helper.assertTrue(reloaded != null, "could not create reloaded horse");
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        helper.assertTrue(reloaded.isInLove(), "reloaded horse should still be in love");
        helper.assertValueEqual(HorseBreedingGeneticsHandler.getRecordedDietItem(reloaded), BreedingDietItem.GOLDEN_CARROT,
            "diet record must survive save/load while in love");

        // Union of both parents' diets.
        EnumSet<GeneticAttribute> favored = HorseBreedingGeneticsHandler.consumeBreedingDiet(carrotFed, appleFed);
        helper.assertValueEqual(favored, EnumSet.of(GeneticAttribute.SPEED, GeneticAttribute.HEALTH),
            "carrot x apple favored set");
        helper.assertValueEqual(HorseBreedingGeneticsHandler.getRecordedDietItem(carrotFed), BreedingDietItem.NONE,
            "breeding consumes the carrot record");
        helper.assertValueEqual(HorseBreedingGeneticsHandler.getRecordedDietItem(appleFed), BreedingDietItem.NONE,
            "breeding consumes the apple record");

        // Enchanted golden apple x code-triggered love: unguided.
        partner.setInLove(null);
        helper.assertTrue(HorseBreedingGeneticsHandler.consumeBreedingDiet(enchantedFed, partner).isEmpty(),
            "Enchanted Golden Apple x setInLove breeding must be unguided");

        // Love ends: by resetLove() and by timing out.
        resetHorse.resetLove();
        timeoutHorse.setInLoveTime(1);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!timeoutHorse.isInLove(), "love should have timed out");
            resetHorse.setInLove(null);
            timeoutHorse.setInLove(null);
            partner.setInLove(null);
            helper.assertTrue(HorseBreedingGeneticsHandler.consumeBreedingDiet(resetHorse, partner).isEmpty(),
                "a record whose love was reset must not guide a later code-triggered breeding");
            helper.assertTrue(HorseBreedingGeneticsHandler.consumeBreedingDiet(timeoutHorse, partner).isEmpty(),
                "a record whose love timed out must not guide a later code-triggered breeding");
            for (Horse horse : all) {
                horse.resetLove();
                horse.discard();
            }
            helper.succeed();
        });
    }

    private static Horse tamedAdult(GameTestHelper helper, int x, int z) {
        Horse horse = helper.spawn(EntityTypes.HORSE, x, 1, z);
        horse.setTamed(true);
        horse.setAge(0);
        return horse;
    }

    private static void feed(ServerPlayer player, Horse horse, Item item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 4));
        player.interactOn(horse, InteractionHand.MAIN_HAND, horse.position());
    }
}
