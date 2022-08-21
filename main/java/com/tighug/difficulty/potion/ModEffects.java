package com.tighug.difficulty.potion;

import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static com.tighug.difficulty.util.Utils.MODID;

public class ModEffects {
    public static final DeferredRegister<Effect> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, MODID);
    public static final RegistryObject<Effect> HEAL = POTIONS.register("health", () -> new HealEffect(EffectType.BENEFICIAL, 16262179));
    public static final RegistryObject<Effect> HARM = POTIONS.register("damage", () -> new HealEffect(EffectType.HARMFUL, 16262179));
}
