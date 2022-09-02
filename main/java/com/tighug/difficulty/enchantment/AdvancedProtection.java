package com.tighug.difficulty.enchantment;

import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.DamageSource;
import org.jetbrains.annotations.NotNull;

public class AdvancedProtection extends ProtectionEnchantment {

    public AdvancedProtection() {
        super(Rarity.VERY_RARE, ProtectionEnchantment.Type.ALL, EquipmentSlotType.HEAD, EquipmentSlotType.CHEST, EquipmentSlotType.LEGS, EquipmentSlotType.FEET);
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getDamageProtection(int p_77318_1_, @NotNull DamageSource p_77318_2_) {
        return 10 * p_77318_1_;
    }

    @Override
    public int getMinCost(int p_77321_1_) {
        return 25;
    }

}
