package com.tighug.difficulty;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.tighug.difficulty.enchantment.ModEnchantments;
import com.tighug.difficulty.potion.ModEffects;
import com.tighug.difficulty.util.Config;
import com.tighug.mklapi.entity.IEntity;
import com.tighug.mklapi.util.TypeOptional;
import com.tighug.mklapi.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameRules;
import net.minecraft.world.end.DragonFightManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.tighug.difficulty.Difficulty.MODID;

@Mod(value = MODID)
public class Difficulty {

    public static final String MODID = "difficulty";

    public Difficulty() {
        MixinBootstrap.init();
        Mixins.addConfiguration("difficulty.mixins.json");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
        ModEnchantments.ENCHANTMENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModEffects.POTIONS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class onModEventBus {

        @SubscribeEvent
        public static void onfMLCommonSetupEvent(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                Class<RangedAttribute> attackDamage = RangedAttribute.class;
                Field[] attackDamageField = attackDamage.getDeclaredFields();
                Map<Double, Field> map = Maps.newHashMap();
                for (Field field : attackDamageField) {
                    if (field.getType() == double.class) {
                        try {
                            field.setAccessible(true);
                            map.put(field.getDouble(Attributes.ATTACK_DAMAGE), field);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (!map.isEmpty()) {
                    double d = 0;
                    for (double d1 : map.keySet()) {
                        if (d1 > d) d = d1;
                    }
                    try {
                        Field max = map.get(d);
                        max.set(Attributes.ATTACK_DAMAGE, Float.MAX_VALUE);
                        max.set(Attributes.ARMOR, 200);
                        max.set(Attributes.ARMOR_TOUGHNESS, 500);
                        max.set(Attributes.MAX_HEALTH, Float.MAX_VALUE);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } finally {
                        map.values().forEach(field -> field.setAccessible(false));
                    }
                }
            });
        }

    }

    @Mod.EventBusSubscriber()
    public static class Event {
        public final static UUID uuid = UUID.fromString("0E6A83B3-3E3C-9392-A21B-B4FED20168EC");
        public final static UUID uuid1 = UUID.fromString("CEC0C635-6A99-1090-3C0F-C4ABBB38661E");
        private final static Set<Explosion> LIST_MAP = Sets.newHashSet();
        private static float day;
        private static Field radius;

        static {
            Arrays.stream(Explosion.class.getDeclaredFields()).
                    filter(field -> field.getType() == float.class)
                    .forEach(field -> radius = field);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onEntityJoinWorldEvent(EntityJoinWorldEvent event) {
            LivingEntity entity = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
            if (event.getWorld().isClientSide()) return;
            if (event.getEntity() instanceof AreaEffectCloudEntity) {
                AreaEffectCloudEntity effectCloud = (AreaEffectCloudEntity) event.getEntity();
                if (effectCloud.getOwner() instanceof EnderDragonEntity) {
                    int i = 1;
                    DragonFightManager dragonFightManager = ((ServerWorld) event.getWorld()).dragonFight();
                    if (day > 40 && dragonFightManager != null && dragonFightManager.hasPreviouslyKilledDragon()) {
                        i += (int) Utils.clamp(day / 40, 0, 10) + 1;
                        effectCloud.setDeltaMovement(effectCloud.getDeltaMovement().multiply(i, i, i));
                    }
                    effectCloud.addEffect(new EffectInstance(ModEffects.DRAGON_BREATH.get(), 1, i));
                }
            } else if (day > 20) {
                if (entity instanceof IMob) {
                    if (entity instanceof EnderDragonEntity) {
                        DragonFightManager dragonFight = ((EnderDragonEntity) entity).getDragonFight();
                        if (dragonFight == null || !dragonFight.hasPreviouslyKilledDragon()) return;
                    }
                    float f1;
                    if (entity instanceof WitherEntity && ((WitherEntity) entity).getInvulnerableTicks() > 0) f1 = 1;
                    else f1 = Utils.clamp(entity.getHealth() / entity.getMaxHealth(), 0, 1);
                    Multimap<Attribute, AttributeModifier> multimap = Multimaps.newMultimap(Maps.newHashMap(), Sets::newHashSet);
                    float i = Utils.clamp(day / 20, 0, 10);
                    multimap.put(Attributes.FOLLOW_RANGE, new AttributeModifier(uuid, MODID, i, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    i = Utils.clamp(day / 10, 0, Integer.MAX_VALUE);
                    multimap.put(Attributes.ARMOR, new AttributeModifier(uuid, MODID, i, AttributeModifier.Operation.ADDITION));
                    multimap.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid, MODID, i, AttributeModifier.Operation.ADDITION));
                    multimap.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, MODID, i, AttributeModifier.Operation.ADDITION));
                    float f = Utils.clamp(day / 10f, 0, Float.MAX_VALUE);
                    multimap.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid, MODID, f, AttributeModifier.Operation.ADDITION));
                    f = Utils.clamp(day / 5f, 0, Float.MAX_VALUE);
                    multimap.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid, MODID, f, AttributeModifier.Operation.ADDITION));
                    if (day > 40) {
                        i = Utils.clamp(day / 40, 0, 10);
                        multimap.put(Attributes.SPAWN_REINFORCEMENTS_CHANCE, new AttributeModifier(uuid1, MODID, -1, AttributeModifier.Operation.MULTIPLY_TOTAL));
                        multimap.put(Attributes.ARMOR, new AttributeModifier(uuid1, MODID, i, AttributeModifier.Operation.MULTIPLY_TOTAL));
                        multimap.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid1, MODID, i, AttributeModifier.Operation.MULTIPLY_TOTAL));
                        multimap.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(uuid1, MODID, i, AttributeModifier.Operation.MULTIPLY_TOTAL));
                        multimap.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid1, MODID, i * 2, AttributeModifier.Operation.MULTIPLY_TOTAL));
                        multimap.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid1, MODID, i * 4, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                    entity.getAttributes().addTransientAttributeModifiers(multimap);
                    entity.setHealth(entity.getMaxHealth() * f1);
                } else if (event.getEntity() instanceof AbstractArrowEntity) {
                    AbstractArrowEntity arrowEntity = (AbstractArrowEntity) event.getEntity();
                    if (arrowEntity.getOwner() instanceof IMob) {
                        float i1 = Utils.clamp(day / 5, 0, Integer.MAX_VALUE);
                        arrowEntity.setBaseDamage(arrowEntity.getBaseDamage() + i1);
                        if (day > 40) {
                            int i = (int) Utils.clamp(day / 40, 0, 10) + 1;
                            arrowEntity.setDeltaMovement(arrowEntity.getDeltaMovement().multiply(i, i, i));
                        }
                    }
                } else if (event.getEntity() instanceof PotionEntity) {
                    PotionEntity potionEntity = (PotionEntity) event.getEntity();
                    if (potionEntity.getOwner() instanceof IMob) {
                        List<EffectInstance> list = PotionUtils.getMobEffects(potionEntity.getItem());
                        for (int i = 0; i < list.size(); ++i) {
                            EffectInstance instance = list.get(i);
                            if (instance.getEffect() == Effects.HEAL) {
                                list.set(i, new EffectInstance(ModEffects.HEAL.get(), instance.getDuration(), instance.getAmplifier()));
                            } else if (instance.getEffect() == Effects.HARM) {
                                list.set(i, new EffectInstance(ModEffects.HARM.get(), instance.getDuration(), instance.getAmplifier()));
                            }
                        }
                        PotionUtils.setCustomEffects(potionEntity.getItem(), list);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onWorldTickEvent(TickEvent.WorldTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                return;
            }
            TypeOptional.of(event.world, ServerWorld.class).ifPresent(world -> day = (float) Utils.clamp(((world.getDayTime() / 24000f % 2147483647L) * Config.DOUBLE_VALUE.get() + Config.INITIAL_VALUE.get()), 0, Config.MAX_VALUE.get()));
        }

        @SubscribeEvent
        public static void onWorldLoadEvent(WorldEvent.Load event) {
            TypeOptional.of(event.getWorld(), ServerWorld.class).ifPresent(world -> day = (float) Utils.clamp(((world.getDayTime() / 24000f % 2147483647L) * Config.DOUBLE_VALUE.get() + Config.INITIAL_VALUE.get()), 0, Config.MAX_VALUE.get()));
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onLivingAttackEvent(LivingAttackEvent event) {
            if (event.getSource().getEntity() instanceof LivingEntity && !event.getEntityLiving().level.isClientSide()) {
                int enchantmentLevel = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.DAMAGE_ENCHANTMENT.get(), (LivingEntity) event.getSource().getEntity());
                if (enchantmentLevel > 0) {
                    LivingEntity entityLiving = event.getEntityLiving();
                    passArmor(enchantmentLevel, entityLiving);
                }
            }
        }

        public static void passArmor(int enchantmentLevel, @NotNull LivingEntity entityLiving) {
            if (entityLiving.getArmorCoverPercentage() > 0) {
                Map<EquipmentSlotType, ItemStack> map = Maps.newHashMap();
                Arrays.stream(EquipmentSlotType.values()).filter(equipmentSlotType -> equipmentSlotType.getType() == EquipmentSlotType.Group.ARMOR)
                        .forEach(equipmentSlotType -> {
                            map.put(equipmentSlotType, entityLiving.getItemBySlot(equipmentSlotType));
                            entityLiving.setItemSlot(equipmentSlotType, ItemStack.EMPTY);
                        });
                IEntity iEntity = (IEntity) entityLiving;
                iEntity.addQueueTask(entity -> {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    if (livingEntity instanceof PlayerEntity) {
                        map.values().forEach(itemStack -> {
                            if (!itemStack.isEmpty() && itemStack.isDamageableItem()) {
                                itemStack.setDamageValue(itemStack.getDamageValue() + Math.max(itemStack.getMaxDamage() / 10, 50) * enchantmentLevel);
                                if (itemStack.getMaxDamage() <= itemStack.getDamageValue()) {
                                    itemStack.setCount(0);
                                }
                            }
                        });
                        if (livingEntity.isAlive() || livingEntity.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
                            map.keySet().forEach(equipmentSlotType -> {
                                ItemStack p_184201_2_ = map.get(equipmentSlotType);
                                if (!p_184201_2_.isEmpty())
                                    livingEntity.setItemSlot(equipmentSlotType, p_184201_2_);
                            });
                        } else
                            map.values().forEach(itemStack -> ((PlayerEntity) livingEntity).drop(itemStack, true, false));
                    } else
                        map.keySet().forEach(equipmentSlotType -> livingEntity.setItemSlot(equipmentSlotType, map.get(equipmentSlotType)));
                });
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onPlayerHurtEvent(LivingHurtEvent event) {
            if (event.getSource().getEntity() instanceof LivingEntity && !event.getEntityLiving().level.isClientSide()) {
                if (event.getSource().isExplosion()) {
                    if (!LIST_MAP.isEmpty()) {
                        AtomicReference<Explosion> explosion1 = new AtomicReference<>();
                        LIST_MAP.forEach(explosion -> {
                            if (explosion.getDamageSource() == event.getSource()) {
                                explosion1.set(explosion);
                            }
                        });
                        if (explosion1.get() != null) {
                            float i;
                            try {
                                radius.setAccessible(true);
                                i = radius.getFloat(explosion1.get());
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            } finally {
                                radius.setAccessible(false);
                            }
                            float f = Math.min(event.getAmount() / (14 * i + 1), 1);
                            event.setAmount((float) Utils.clamp((event.getAmount() + day / 5d * f) * Math.min(1 + day / 40, 10), event.getAmount(), Float.MAX_VALUE));
                        }
                    }
                } else {
                    LivingEntity livingEntity = (LivingEntity) event.getSource().getEntity();
                    int enchantmentLevel = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.DAMAGE_ENCHANTMENT.get(), livingEntity);
                    if (enchantmentLevel > 0) {
                        event.getSource().bypassArmor();
                        if (event.getAmount() > (livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE) + 5 * enchantmentLevel) * 0.8) {
                            LivingEntity entityLiving = event.getEntityLiving();
                            double d = (entityLiving.getArmorValue() * 9 + entityLiving.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) / 10d * enchantmentLevel;
                            event.setAmount((float) ((event.getAmount() + d / 2d) * (1 + d / 100)));
                            if (entityLiving instanceof PlayerEntity) {
                                entityLiving.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 20 + entityLiving.getRandom().nextInt(20), 3));
                            } else {
                                Multimap<Attribute, AttributeModifier> multimap = Multimaps.newMultimap(Maps.newHashMap(), Sets::newHashSet);
                                if (day > 39 && entityLiving instanceof IMob)
                                    multimap.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(uuid1, MODID, 0, AttributeModifier.Operation.MULTIPLY_TOTAL));
                                else
                                    multimap.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(uuid1, MODID, -0.8, AttributeModifier.Operation.MULTIPLY_TOTAL));
                                entityLiving.getAttributes().addTransientAttributeModifiers(multimap);
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onLivingDamageEvent(LivingDamageEvent event) {
            LivingEntity entity = event.getEntityLiving();
            if (!entity.level.isClientSide()) event.setAmount(advancedProtection(entity, event.getAmount()));
        }

        public static float advancedProtection(@NotNull Entity entity1, float amount) {
            if (!(entity1 instanceof LivingEntity)) return amount;
            LivingEntity entity = (LivingEntity) entity1;
            if (!entity.level.isClientSide()) {
                int i = getAdvancedProtectionLevel(entity);
                if (i > 0) {
                    double i11 = entity instanceof PlayerEntity ? 20 : (entity.getMaxHealth() / 2);
                    if (Double.isNaN(i11)) {
                        throw new ArithmeticException("MaxHealth isNaN");
                    }
                    double i1 = i11 / (i + 1);
                    entity.invulnerableTime += i * 10;
                    double v = Utils.clamp(amount - i * 2, 0, i1) * (Utils.clamp(entity.getHealth() * 0.8d / i11, 0, 0.8d) + 0.2d);
                    TypeOptional.of(entity, ServerPlayerEntity.class).ifPresent(player -> {
                        double f = amount - v - (1 << i);
                        if (f > 1) {
                            f = f / (i / 2d);
                            Map<EquipmentSlotType, ItemStack> map = Maps.newHashMap();
                            Arrays.stream(EquipmentSlotType.values()).filter(equipmentSlotType -> equipmentSlotType.getType() == EquipmentSlotType.Group.ARMOR)
                                    .forEach(equipmentSlotType -> {
                                        ItemStack itemBySlot = player.getItemBySlot(equipmentSlotType);
                                        if (!itemBySlot.isEmpty() && itemBySlot.isDamageableItem())
                                            map.put(equipmentSlotType, itemBySlot);
                                    });
                            if (!map.isEmpty()) {
                                f = Math.min(f / map.size(), Integer.MAX_VALUE);
                                if (f > 1) {
                                    double finalF = f;
                                    map.keySet().forEach(equipmentSlotType -> {
                                        ItemStack stack = map.get(equipmentSlotType);
                                        ServerPlayerEntity entity2 = (ServerPlayerEntity) entity;
                                        stack.hurtAndBreak((int) finalF, entity2, player1 -> player1.broadcastBreakEvent(equipmentSlotType));
                                    });
                                }
                            }
                        }
                    });
                    return (float) v;
                }
            }
            return amount;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onExplosionStart(ExplosionEvent.Start event) {
            Explosion explosion = event.getExplosion();
            if (day > 100 && !event.getWorld().isClientSide() && explosion.getDamageSource().getEntity() instanceof IMob) {
                try {
                    radius.setAccessible(true);
                    radius.set(explosion, radius.getFloat(explosion) * 2f);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } finally {
                    radius.setAccessible(false);
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
            if (day > 20 && !event.getWorld().isClientSide() && event.getExplosion().getDamageSource().getEntity() instanceof IMob) {
                LIST_MAP.add(event.getExplosion());
            }
        }

        @SubscribeEvent()
        public static void onServerTickEvent(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                return;
            }
            LIST_MAP.clear();
        }

        public static float getDay() {
            return day;
        }

        private static int getAdvancedProtectionLevel(@NotNull LivingEntity entity) {
            if (entity.getArmorCoverPercentage() == 0) return 0;
            int i = 0;
            for (ItemStack stack : entity.getArmorSlots()) {
                i += EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ADVANCED_PROTECTION.get(), stack);
            }
            return Utils.clamp(i, 0, 255);
        }
    }

    @OnlyIn(value = Dist.CLIENT)
    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class KeyEvent {
        private static final Minecraft minecraft = Minecraft.getInstance();
        private static byte tick = 0;

        @SubscribeEvent
        public static void onClientTickEvent(TickEvent.ClientTickEvent event) {
            if (tick == 20) {
                MinecraftForge.EVENT_BUS.unregister(KeyEvent.class);
                return;
            }
            if (minecraft.player == null || event.phase == TickEvent.Phase.END) return;
            if (tick > 0) {
                --tick;
            }
            if (tick <= 0 && minecraft.options.keyAttack.isDown() && Objects.requireNonNull(minecraft.hitResult).getType() == RayTraceResult.Type.ENTITY) {
                if (minecraft.player.getAttackStrengthScale(0.5F) == 1) {
                    tick = Config.INT_VALUE.get().byteValue();
                    KeyBinding.click(minecraft.options.keyAttack.getKey());
                }
            }
        }

    }

}
