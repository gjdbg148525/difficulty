package com.tighug.difficulty.enchantment;

import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import org.jetbrains.annotations.NotNull;

public class PassArmorDamageEnchantment extends DamageEnchantment {

    public PassArmorDamageEnchantment() {
        super(Rarity.VERY_RARE, 0, EquipmentSlotType.MAINHAND);
    }

    @Override
    public int getMinCost(int p_77321_1_) {
        return 25;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public float getDamageBonus(int p_152376_1_, @NotNull CreatureAttribute p_152376_2_) {
        return p_152376_1_ * 5;
    }

    @Override
    public void doPostAttack(@NotNull LivingEntity p_151368_1_, @NotNull Entity p_151368_2_, int p_151368_3_) {
    }
}
