package com.tighug.difficulty.potion;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tighug.difficulty.Difficulty;
import com.tighug.mklapi.entity.DamageAbsorb;
import com.tighug.mklapi.entity.IEntity;
import com.tighug.mklapi.util.Utils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.InstantEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.end.DragonFightManager;
import net.minecraft.world.server.ServerWorld;
import org.jetbrains.annotations.NotNull;

public class DragonBreath extends InstantEffect {

    public DragonBreath() {
        super(EffectType.HARMFUL, 16262179);
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity p_76394_1_, int p_76394_2_) {
        ServerWorld world = p_76394_1_.level instanceof ServerWorld ? (ServerWorld) p_76394_1_.level : null;
        if (world == null) return;
        float f = 480 << p_76394_2_;
        float d = Difficulty.Event.getDay();
        DragonFightManager dragonFightManager = world.dragonFight();
        if (d > 20 && dragonFightManager != null && dragonFightManager.hasPreviouslyKilledDragon()) {
            f = (float) Utils.add(f, d / 10d);
            if (d > 40) {
                f *= (int) Math.min(10, d / 40);
            }
        }
        float finalF = f;
        Sets.newHashSet(p_76394_1_.getActiveEffectsMap().keySet()).stream().filter(Effect::isBeneficial).forEach(p_76394_1_::removeEffect);
        ((IEntity) p_76394_1_).multipleHurt(Maps.asMap(Sets.newHashSet(DamageSource.MAGIC, DamageSource.DRAGON_BREATH), ds -> finalF), DamageAbsorb.of(Difficulty.Event::advancedProtection, p_76394_1_));
    }
}
