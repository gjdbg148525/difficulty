package com.tighug.difficulty.enchantment;

import com.google.common.collect.Sets;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class Precision extends Enchantment{
    private static final Set<Class<? extends Item>> CLASSES;

    static {
        //noinspection unchecked
        CLASSES = Sets.newHashSet(TridentItem.class, CrossbowItem.class, BowItem.class);
    }

    public Precision() {
        super(Rarity.UNCOMMON, EnchantmentType.WEAPON, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean canEnchant(@NotNull ItemStack p_92089_1_) {
        for (Class<? extends Item> c : CLASSES) {
            if (c.isInstance(p_92089_1_.getItem())) return true;
        }
        return super.canEnchant(p_92089_1_);
    }

}
