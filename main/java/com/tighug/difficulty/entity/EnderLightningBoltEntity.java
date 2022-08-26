package com.tighug.difficulty.entity;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.tighug.difficulty.util.Utils;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static com.tighug.difficulty.Difficulty.Event.uuid1;
import static com.tighug.difficulty.util.Utils.MODID;

public class EnderLightningBoltEntity extends LightningBoltEntity {
    private static final DamageSource DAMAGE_SOURCE = new DamageSource("enderLightningBolt").bypassArmor().bypassMagic().bypassInvul();
    private int life;
    private int flashes;


    public EnderLightningBoltEntity(EntityType<? extends LightningBoltEntity> p_i231491_1_, World p_i231491_2_) {
        super(p_i231491_1_, p_i231491_2_);
        this.life = 2;
        this.flashes = this.random.nextInt(3) + 1;
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide) {
            this.setSharedFlag(6, this.isGlowing());
        }
        this.baseTick();

        if (this.life == 2) {
            Difficulty difficulty = this.level.getDifficulty();
            if (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) {
                this.spawnFire(4);
            }

            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 10000.0F, 0.8F + this.random.nextFloat() * 0.2F);
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER, 2.0F, 0.5F + this.random.nextFloat() * 0.2F);
        }

        --this.life;
        if (this.life < 0) {
            if (this.flashes == 0) {
                this.remove();
            } else if (this.life < -this.random.nextInt(10)) {
                --this.flashes;
                this.life = 1;
                this.seed = this.random.nextLong();
                this.spawnFire(0);
            }
        }

        if (this.life >= 0) {
            if (!(this.level instanceof ServerWorld)) {
                this.level.setSkyFlashTime(2);
            } else {
                List<Entity> list = this.level.getEntities(this, new AxisAlignedBB(this.getX() - 3.0D, this.getY() - 3.0D, this.getZ() - 3.0D, this.getX() + 3.0D, this.getY() + 6.0D + 3.0D, this.getZ() + 3.0D), Entity::isAlive);

                for(Entity entity : list) {
                    if (entity instanceof EnderDragonEntity) continue;
                    double damage = this.getDamage();
                    if (entity instanceof LivingEntity) {
                        LivingEntity entity1 = (LivingEntity) entity;
                        Sets.newHashSet(entity1.getActiveEffectsMap().keySet()).stream().filter(Effect::isBeneficial).forEach(entity1::removeEffect);
                        com.tighug.difficulty.Difficulty.Event.passArmor(1, entity1);
                        double d = (entity1.getArmorValue() * 9 + entity1.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) / 5d;
                        damage = (damage + d / 2d) * (1 + d / 100);
                        if (entity1 instanceof PlayerEntity) {
                            PlayerEntity player = (PlayerEntity) entity1;
                            if (player.isCreative() || player.isSpectator()) continue;
                            entity1.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 40 + entity1.getRandom().nextInt(40), 6));
                        }
                        else {
                            float f1 = Utils.clamp(entity1.getHealth() / entity1.getMaxHealth(), 0, 1);
                            Multimap<Attribute, AttributeModifier> multimap = Multimaps.newMultimap(Maps.newHashMap(), Sets::newHashSet);
                            ForgeRegistries.ATTRIBUTES.getValues().forEach(attribute -> multimap.put(attribute, new AttributeModifier(uuid1, MODID, -1, AttributeModifier.Operation.MULTIPLY_TOTAL)));
                            entity1.getAttributes().addTransientAttributeModifiers(multimap);
                            entity1.setHealth(entity1.getMaxHealth() * f1);
                        }
                    }
                    float finalDamage = (float) damage;
                    ((IEntity) entity).multipleHurt(Maps.asMap(Sets.newHashSet(DAMAGE_SOURCE, DamageSource.IN_FIRE, DamageSource.MAGIC), ds -> finalDamage));
                }
            }
        }
    }

    private void spawnFire(int p_195053_1_) {
        if (!this.level.isClientSide && this.level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            BlockPos blockpos = this.blockPosition();
            BlockState blockstate = AbstractFireBlock.getState(this.level, blockpos);
            if (this.level.getBlockState(blockpos).is(Blocks.AIR) && blockstate.canSurvive(this.level, blockpos)) {
                this.level.setBlockAndUpdate(blockpos, blockstate);
            }

            for(int i = 0; i < p_195053_1_; ++i) {
                BlockPos blockpos1 = blockpos.offset(this.random.nextInt(3) - 1, this.random.nextInt(3) - 1, this.random.nextInt(3) - 1);
                blockstate = AbstractFireBlock.getState(this.level, blockpos1);
                if (this.level.getBlockState(blockpos1).is(Blocks.AIR) && blockstate.canSurvive(this.level, blockpos1)) {
                    this.level.setBlockAndUpdate(blockpos1, blockstate);
                }
            }

        }
    }
}
