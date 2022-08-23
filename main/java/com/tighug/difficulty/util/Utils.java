package com.tighug.difficulty.util;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.stats.Stats;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import org.jetbrains.annotations.NotNull;

public class Utils {
    public static final String MODID = "difficulty";

    public static @NotNull TextComponent getTextComponent(int i) {
        if (i < 1) return new StringTextComponent(String.valueOf(i));
        else {
            Integer[] ints = {0, 0, 0, 0, 0,};
            int j = i;
            if (j >= 100){
                int i1 = j / 100;
                j -= 100 * i1;
                ints[0] += i1;
            }
            if (j >= 50){
                int i1 = j / 50;
                j -= 50 * i1;
                ints[1] += i1;
            }
            if (j >= 10){
                int i1 = j / 10;
                j -= 10 * i1;
                ints[2] += i1;
            }
            if (j >= 5){
                int i1 = j / 5;
                j -= 5 * i1;
                ints[3] += i1;
            }
            ints[4] += j;
            StringBuilder sb = new StringBuilder();
            for (int anInt = 0; anInt < 5; ++anInt){
                while (ints[anInt] > 0){
                    ints[anInt] -= 1;
                    switch(anInt){
                        case 0 : sb.append('C');
                            break;
                        case 1 : sb.append('L');
                            break;
                        case 2 : sb.append('X');
                            break;
                        case 3 : sb.append('V');
                            break;
                        case 4 : sb.append('I');
                            break;
                    }
                }
            }
            return new StringTextComponent(sb.toString());
        }
    }

    public static boolean checkTotemDeathProtection(DamageSource damageSource, LivingEntity livingEntity) {
        if (damageSource.isBypassInvul()) {
            return false;
        } else {
            ItemStack itemstack = null;
            for(Hand hand : Hand.values()) {
                ItemStack itemstack1 = livingEntity.getItemInHand(hand);
                if (itemstack1.getItem() == Items.TOTEM_OF_UNDYING) {
                    itemstack = itemstack1.copy();
                    itemstack1.shrink(1);
                    break;
                }
            }
            if (itemstack != null) {
                if (livingEntity instanceof ServerPlayerEntity) {
                    ServerPlayerEntity serverplayerentity = (ServerPlayerEntity)livingEntity;
                    serverplayerentity.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
                    CriteriaTriggers.USED_TOTEM.trigger(serverplayerentity, itemstack);
                }
                livingEntity.setHealth(1.0F);
                livingEntity.removeAllEffects();
                livingEntity.addEffect(new EffectInstance(Effects.REGENERATION, 900, 1));
                livingEntity.addEffect(new EffectInstance(Effects.ABSORPTION, 100, 1));
                livingEntity.addEffect(new EffectInstance(Effects.FIRE_RESISTANCE, 800, 0));
                livingEntity.level.broadcastEntityEvent(livingEntity, (byte)35);
            }
            return itemstack != null;
        }
    }

    // if d1 isNaN return min
    public static double clamp(double d1, double min, double max) {
        if (d1 > max) return max;
        return d1 > min ? d1 : min;
    }

    public static float clamp(float d1, float min, float max) {
        if (d1 > max) return max;
        return d1 > min ? d1 : min;
    }

    public static long clamp(long d1, long min, long max) {
        if (d1 > max) return max;
        return d1 > min ? d1 : min;
    }

    public static int clamp(int d1, int min, int max) {
        if (d1 > max) return max;
        return d1 > min ? d1 : min;
    }

    public static double add(double n1, double add) {
        if (n1 < 0) add *= -1;
        return n1 + add;
    }

}
