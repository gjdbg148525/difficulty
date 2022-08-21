package com.tighug.difficulty.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class onLivingEntity extends Entity {

    private onLivingEntity(World p_i48580_2_) {
        super(EntityType.AREA_EFFECT_CLOUD, p_i48580_2_);
    }

    @Shadow
    protected abstract void hurtArmor(DamageSource p_230294_1_, float p_230294_2_);

    @Shadow
    public abstract int getArmorValue();

    @Shadow
    public abstract double getAttributeValue(Attribute p_233637_1_);

    @Shadow
    public abstract float getArmorCoverPercentage();

    /**
     * @author tighug
     * @reason getDamageAfterArmorAbsorb
     */
    @Overwrite
    protected float getDamageAfterArmorAbsorb(@NotNull DamageSource damageSource, float damage) {
        if (!damageSource.isExplosion() && damageSource.getEntity() instanceof LivingEntity) {
            int i = this.random.nextInt(16);
            if (i == 0) return 0;
            else if (damageSource.isBypassArmor()) {
                if (i == 15) damage *= 2;
            }
            else {
                double d = this.getArmorValue() - damage / (2d + this.getAttributeValue(Attributes.ARMOR_TOUGHNESS) / 4d);
                d = Math.max(0, d) + this.getArmorCoverPercentage() * 4d;
                if (Math.min(14, i) > d) {
                    if (i == 15) damage *= 2;
                }
                else {
                    this.hurtArmor(damageSource, damage);
                    if (i != 15) return 0;
                }
            }
        }
        else if (!damageSource.isBypassArmor()) {
            float damageAfterAbsorb = CombatRules.getDamageAfterAbsorb(damage, (float) this.getArmorValue(), (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            this.hurtArmor(damageSource, damage - damageAfterAbsorb);
            damage = damageAfterAbsorb;
        }
        return damage;
    }
}
