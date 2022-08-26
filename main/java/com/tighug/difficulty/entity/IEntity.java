package com.tighug.difficulty.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;

import java.util.Map;
import java.util.function.Consumer;

public interface IEntity {

    default int multipleHurt(Map<DamageSource, Float> damageSourceFloatMap) {
        return 0;
    }

    default void addQueueTask(Consumer<Entity> c) {}

    default boolean hasQueue() {
        return false;
    }
}
