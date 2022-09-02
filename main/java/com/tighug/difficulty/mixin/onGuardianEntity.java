package com.tighug.difficulty.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.RandomWalkingGoal;
import net.minecraft.entity.monster.GuardianEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuardianEntity.class)
public abstract class onGuardianEntity extends MonsterEntity {

    @Shadow
    protected RandomWalkingGoal randomStrollGoal;

    private onGuardianEntity(World p_i48553_2_) {
        super(EntityType.GUARDIAN, p_i48553_2_);
    }

    @Shadow
    public abstract boolean isMoving();

    @Override
    public boolean hurt(@NotNull DamageSource p_70097_1_, float p_70097_2_) {
        if (!this.isMoving() && !p_70097_1_.isMagic() && p_70097_1_.getDirectEntity() instanceof LivingEntity) {
            if (!p_70097_1_.isExplosion()) {
                p_70097_1_.getDirectEntity().hurt(DamageSource.thorns(this), Math.max(2f, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) / 2f));
            }
        }

        if (this.randomStrollGoal != null) {
            this.randomStrollGoal.trigger();
        }

        return super.hurt(p_70097_1_, p_70097_2_);
    }
}
