package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.event.HorseBreedingGeneticsHandler;
import com.josebieco.strideandsire.item.genetics.BreedingDietItem;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Test instance: data/strideandsire/test_instance/genetics_breeding_food_glistering_melon_triggers_love.json
 */
public final class HorseBreedingFoodGameTest {
    private HorseBreedingFoodGameTest() {}

    /**
     * Task 6 Step 1: feeds a Glistering Melon Slice to two tamed adult horses and asserts they enter love mode,
     * consume the item, and record {@link BreedingDietItem#GLISTERING_MELON}.
     */
    public static void glisteringMelonTriggersLoveMode(GameTestHelper helper) {
        helper.makeFloor();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        Horse horseA = helper.spawn(EntityTypes.HORSE, 1, 1, 1);
        Horse horseB = helper.spawn(EntityTypes.HORSE, 2, 1, 1);
        horseA.setTamed(true);
        horseB.setTamed(true);
        horseA.setAge(0);
        horseB.setAge(0);

        ItemStack melonStack = new ItemStack(Items.GLISTERING_MELON_SLICE);
        helper.assertTrue(horseA.isFood(melonStack), "Glistering Melon Slice should be recognized by AbstractHorse.isFood");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLISTERING_MELON_SLICE, 2));
        player.interactOn(horseA, InteractionHand.MAIN_HAND, horseA.position());
        player.interactOn(horseB, InteractionHand.MAIN_HAND, horseB.position());

        helper.assertTrue(horseA.isInLove(), "horseA should enter love mode after eating Glistering Melon Slice");
        helper.assertTrue(horseB.isInLove(), "horseB should enter love mode after eating Glistering Melon Slice");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "both Glistering Melon Slices should be consumed");
        helper.assertTrue(
            HorseBreedingGeneticsHandler.getRecordedDietItem(horseA) == BreedingDietItem.GLISTERING_MELON,
            "horseA should record GLISTERING_MELON as its breeding diet item"
        );
        helper.assertTrue(
            HorseBreedingGeneticsHandler.getRecordedDietItem(horseB) == BreedingDietItem.GLISTERING_MELON,
            "horseB should record GLISTERING_MELON as its breeding diet item"
        );
        helper.succeed();
    }
}
