package com.tighug.difficulty.entity;

import net.minecraft.util.DamageSource;

public interface ILivingEntity extends IEntity{

    boolean publicCheckTotemDeathProtection(DamageSource damageSource);
}
