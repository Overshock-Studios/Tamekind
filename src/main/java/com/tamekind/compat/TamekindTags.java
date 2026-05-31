package com.tamekind.compat;

import com.tamekind.TamekindMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public final class TamekindTags {
    public static final TagKey<EntityType<?>> HERDABLE = entity("herdable");
    public static final TagKey<EntityType<?>> PREDATORS = entity("predators");
    public static final TagKey<EntityType<?>> DISABLED = entity("disabled");
    public static final TagKey<EntityType<?>> FREEZERS = entity("freezers");
    public static final TagKey<EntityType<?>> MATING_DISPLAYS = entity("mating_displays");
    public static final TagKey<EntityType<?>> HEAT_SENSITIVE = entity("heat_sensitive");
    public static final TagKey<Block> GRAZING_BLOCKS = block("grazing_blocks");
    public static final TagKey<Block> SHELTER_BLOCKS = block("shelter_blocks");
    public static final TagKey<Block> AVOID_BLOCKS = block("avoid_blocks");
    public static final TagKey<Block> WATER_BLOCKS = block("water_blocks");
    public static final TagKey<Block> COMFORT_BLOCKS = block("comfort_blocks");
    public static final TagKey<Block> SOFT_AVOID_BLOCKS = block("soft_avoid_blocks");
    public static final TagKey<Block> NEST_BLOCKS = block("nest_blocks");

    private TamekindTags() {
    }

    private static TagKey<EntityType<?>> entity(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(TamekindMod.MOD_ID, path));
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(TamekindMod.MOD_ID, path));
    }
}
