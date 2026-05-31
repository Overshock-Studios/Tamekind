package com.tamekind.datagen;

import com.tamekind.compat.TamekindTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public final class TamekindBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
    public TamekindBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        addBlocks(TamekindTags.GRAZING_BLOCKS,
                Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN);
        addBlocks(TamekindTags.AVOID_BLOCKS,
                Blocks.LAVA, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.MAGMA_BLOCK,
                Blocks.CACTUS, Blocks.SWEET_BERRY_BUSH, Blocks.POWDER_SNOW);
        addBlocks(TamekindTags.SOFT_AVOID_BLOCKS,
                Blocks.SNOW, Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
                Blocks.PUMPKIN_STEM, Blocks.MELON_STEM, Blocks.TORCHFLOWER_CROP,
                Blocks.PITCHER_CROP, Blocks.FARMLAND);
        addBlocks(TamekindTags.WATER_BLOCKS, Blocks.WATER);
        addBlocks(TamekindTags.NEST_BLOCKS, Blocks.HAY_BLOCK);
        addBlocks(TamekindTags.COMFORT_BLOCKS,
                Blocks.HAY_BLOCK, Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.PALE_MOSS_CARPET,
                Blocks.WHITE_WOOL, Blocks.LIGHT_GRAY_WOOL, Blocks.GRAY_WOOL, Blocks.BLACK_WOOL,
                Blocks.BROWN_WOOL, Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL,
                Blocks.LIME_WOOL, Blocks.GREEN_WOOL, Blocks.CYAN_WOOL, Blocks.LIGHT_BLUE_WOOL,
                Blocks.BLUE_WOOL, Blocks.PURPLE_WOOL, Blocks.MAGENTA_WOOL, Blocks.PINK_WOOL);
        addBlocks(TamekindTags.SHELTER_BLOCKS);
    }

    private void addBlocks(net.minecraft.tags.TagKey<Block> tag, Block... blocks) {
        var b = builder(tag);
        for (Block block : blocks) {
            b.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow());
        }
    }
}
