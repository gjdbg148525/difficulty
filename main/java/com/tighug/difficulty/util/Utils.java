package com.tighug.difficulty.util;

import com.google.common.collect.Lists;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;

import java.util.List;
import java.util.function.Consumer;

public class Utils {
    public static final String MODID = "difficulty";
    public static final Consumer<ModifiableAttributeInstance> REMOVE_ADVERSE_MODIFIER = modifiableAttributeInstance -> {
        List<AttributeModifier> list = Lists.newArrayList();
        modifiableAttributeInstance.getModifiers().forEach(attributeModifier -> {
            if (!(attributeModifier.getAmount() >= 0))
                list.add(attributeModifier);
        });
        list.forEach(modifiableAttributeInstance::removeModifier);
    };

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
