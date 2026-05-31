package com.tamekind.ai;

import com.tamekind.TamekindMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;

import java.util.HashMap;
import java.util.Map;

public final class TagHunting {
    private static final Map<Identifier, TagKey<EntityType<?>>> CACHE = new HashMap<>();

    private TagHunting() {
    }

    private static TagKey<EntityType<?>> predatorsOf(Animal prey) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(prey.getType());
        return CACHE.computeIfAbsent(id, k ->
                TagKey.create(Registries.ENTITY_TYPE,
                        Identifier.fromNamespaceAndPath(TamekindMod.MOD_ID,
                                "predators_of/" + k.getNamespace() + "/" + k.getPath())));
    }

    public static boolean shouldHunt(Mob predator, Animal prey) {
        if (prey == null || prey == predator || !prey.isAlive()) return false;
        if (prey instanceof net.minecraft.world.entity.TamableAnimal tame
                && (tame.isTame() || tame.isOrderedToSit())) return false;
        var predHolder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(predator.getType());
        return predHolder.is(predatorsOf(prey));
    }
}
