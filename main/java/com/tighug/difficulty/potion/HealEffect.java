package com.tighug.difficulty.potion;

import com.tighug.difficulty.Difficulty;
import com.tighug.difficulty.util.Utils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Effects;
import net.minecraft.potion.InstantEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

public class HealEffect extends InstantEffect {
    private final EffectType effectType;

    public HealEffect(EffectType p_i50392_1_, int p_i50392_2_) {
        super(p_i50392_1_, p_i50392_2_);
        effectType = p_i50392_1_;
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity p_76394_1_, int p_76394_2_) {
        long l = effectType == EffectType.BENEFICIAL ? -4 : 6;
        l = l << Math.min(p_76394_2_, 40);
        float f = l;
        float d = Difficulty.Event.getDay();
        if (d > 20) {
            f = (float) Utils.add(f, d / 10d);
            if (d > 40) {
                f *= (int) Math.min(10, d / 40);
            }
            f = Utils.clamp(f, -Float.MAX_VALUE, Float.MAX_VALUE);
            if (f > 0) p_76394_1_.hurt(DamageSource.MAGIC, f);
            else p_76394_1_.heal(-f);
        }
    }

    @Override
    public @NotNull ITextComponent getDisplayName() {
        if (effectType == EffectType.BENEFICIAL) return Effects.HEAL.getDisplayName();
        else return Effects.HARM.getDisplayName();
    }

}
