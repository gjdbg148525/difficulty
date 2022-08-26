package com.tighug.difficulty.entity;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

public interface IEntity {

    default int multipleHurt(@NotNull Map<DamageSource, Float> damageSourceFloatMap) {
        Multimap<DamageSource, Float> damageSourceFloatMap1 = Multimaps.newMultimap(Maps.newHashMap(), Sets::newHashSet);
        damageSourceFloatMap.forEach(damageSourceFloatMap1::put);
        return this.multipleHurt(damageSourceFloatMap1);
    }

    default int multipleHurt(@NotNull Multimap<DamageSource, Float> damageSourceFloatMap) {
        return 0;
    }

    default boolean addQueueTask(@NotNull Consumer<Entity> c) {return false;}

    default boolean hasQueue() {
        return false;
    }
}
