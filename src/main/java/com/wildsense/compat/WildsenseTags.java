package com.wildsense.compat;

import com.wildsense.WildsenseMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public final class WildsenseTags {
    public static final TagKey<EntityType<?>> HERDABLE = entity("herdable");
    public static final TagKey<EntityType<?>> PREDATORS = entity("predators");
    public static final TagKey<Block> GRAZING_BLOCKS = block("grazing_blocks");
    public static final TagKey<Block> SHELTER_BLOCKS = block("shelter_blocks");
    public static final TagKey<Block> AVOID_BLOCKS = block("avoid_blocks");
    public static final TagKey<Block> WATER_BLOCKS = block("water_blocks");
    public static final TagKey<Block> COMFORT_BLOCKS = block("comfort_blocks");
    public static final TagKey<Block> SOFT_AVOID_BLOCKS = block("soft_avoid_blocks");

    private WildsenseTags() {
    }

    private static TagKey<EntityType<?>> entity(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(WildsenseMod.MOD_ID, path));
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(WildsenseMod.MOD_ID, path));
    }
}
