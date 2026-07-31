package com.tamekind.datagen;

import com.tamekind.TamekindMod;
import com.tamekind.compat.TamekindTags;
import com.tamekind.config.TamekindConfig;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EntityType;

import java.util.concurrent.CompletableFuture;

public final class TamekindEntityTagProvider extends FabricTagsProvider.EntityTypeTagsProvider {
    public TamekindEntityTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        add(TamekindTags.FREEZERS, EntityType.RABBIT, EntityType.CHICKEN, EntityType.PARROT);
        add(TamekindTags.MATING_DISPLAYS, EntityType.SHEEP, EntityType.GOAT);
        add(TamekindTags.HEAT_SENSITIVE,
                EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.WOLF, EntityType.POLAR_BEAR);
        add(TamekindTags.PREDATORS,
                EntityType.WOLF, EntityType.FOX, EntityType.OCELOT, EntityType.CAT,
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                EntityType.HUSK, EntityType.STRAY, EntityType.PILLAGER, EntityType.VINDICATOR);
        add(TamekindTags.HERDABLE,
                EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.GOAT,
                EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.LLAMA,
                EntityType.RABBIT, EntityType.CHICKEN, EntityType.WOLF, EntityType.FOX);
        add(TamekindTags.DISABLED);
        // Predator turf conflict: wolves contest foxes. The prey-keyed tag drives both
        // halves at once: foxes flee wolves via ThreatScanner, wolves hunt foxes via
        // TagHunting, so rivalry needs no separate system, just an entry.
        if (TamekindConfig.predatorTurfConflictEnabled) {
            add(predatorsOf("fox"), EntityType.WOLF);
        }
    }

    private static net.minecraft.tags.TagKey<EntityType<?>> predatorsOf(String preyPath) {
        return net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        TamekindMod.MOD_ID, "predators_of/minecraft/" + preyPath));
    }

    private void add(net.minecraft.tags.TagKey<EntityType<?>> tag, EntityType<?>... types) {
        var b = builder(tag);
        for (EntityType<?> t : types) {
            b.add(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getResourceKey(t).orElseThrow());
        }
    }
}
