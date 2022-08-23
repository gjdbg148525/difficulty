package com.tighug.difficulty.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static com.tighug.difficulty.util.Utils.MODID;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MODID);
    public static final RegistryObject<AdvancedProtection> ADVANCED_PROTECTION = ENCHANTMENTS.register("advanced_protection", AdvancedProtection::new);
    public static final RegistryObject<PassArmorDamageEnchantment> DAMAGE_ENCHANTMENT = ENCHANTMENTS.register("damage_enchantment", PassArmorDamageEnchantment::new);
    public static final RegistryObject<Precision> PRECISION = ENCHANTMENTS.register("precision", Precision::new);
}
