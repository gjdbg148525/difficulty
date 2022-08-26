package com.tighug.difficulty.entity.mixin;

import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.tighug.difficulty.entity.IEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.function.Consumer;

@Mixin(Entity.class)
public abstract class onEntity implements IEntity {

    protected Queue<Consumer<Entity>> consumerQueue;

    @Shadow
    public abstract boolean hurt(DamageSource p_70097_1_, float p_70097_2_);

    @Override
    public int multipleHurt(@NotNull Multimap<DamageSource, Float> damageSourceFloatMap) {
        int i = 0;
        for (DamageSource ds : damageSourceFloatMap.keySet()) {
            for (float f : damageSourceFloatMap.get(ds)) if (this.hurt(ds, f)) i += 1;
        }
        return i;
    }

    @Override
    public boolean addQueueTask(@NotNull Consumer<Entity> c) {
        if (this.hasQueue()) return consumerQueue.add(c);
        return false;
    }

    @Override
    public boolean hasQueue() {
        return consumerQueue != null;
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    @Inject(at = @At("HEAD"), method = "tick")
    protected void onTick(CallbackInfo ci) {
        if (consumerQueue == null) consumerQueue = Queues.newArrayDeque();
        else {
            synchronized (consumerQueue) {
                while (!consumerQueue.isEmpty()) {
                    consumerQueue.poll().accept(Entity.class.cast(this));
                }
            }
        }
    }
}
