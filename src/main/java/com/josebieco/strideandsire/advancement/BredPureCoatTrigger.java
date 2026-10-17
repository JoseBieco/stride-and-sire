package com.josebieco.strideandsire.advancement;

import com.josebieco.strideandsire.genetics.CoatGene;
import com.josebieco.strideandsire.registry.ModCriteriaTriggers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Custom advancement criterion trigger ({@code strideandsire:bred_pure_coat}) fired for the owning player when a foal
 * is born with {@link com.josebieco.strideandsire.genetics.Zygosity#HOMOZYGOUS_RECESSIVE} on a {@link CoatGene}.
 */
public final class BredPureCoatTrigger extends SimpleCriterionTrigger<BredPureCoatTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public static void trigger(ServerPlayer player, CoatGene gene) {
        ModCriteriaTriggers.BRED_PURE_COAT.get().trigger(player, instance -> instance.matches(gene));
    }

    public record TriggerInstance(
        Optional<Holder<LootItemCondition>> player,
        CoatGene gene
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            CoatGene.CODEC.fieldOf("gene").forGetter(TriggerInstance::gene)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(CoatGene candidate) {
            return this.gene == candidate;
        }
    }
}
