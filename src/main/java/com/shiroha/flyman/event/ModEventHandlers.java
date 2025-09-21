package com.shiroha.flyman.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.shiroha.flyman.sound.ModSounds;
import com.shiroha.flyman.enchantment.ModEnchantments;

@Mod.EventBusSubscriber(modid = "flyman")
public class ModEventHandlers {
    
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        
        // 检查受害者是否穿戴了神鹰附魔的盔甲
        if (!victim.level().isClientSide() && event.getSource().getEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
            
            // 若攻击者是铁傀儡：按“神鹰武器模式Lv2”为受害者施加击飞与落地伤害
            if (attacker instanceof IronGolem) {
                LivingEntity livingTarget = victim;
                int heldLevel = 2; // 固定2级
                float upwardVelocity = 0.75F + (heldLevel * 0.5F); // 1.75
                
                livingTarget.setDeltaMovement(new Vec3(
                    livingTarget.getDeltaMovement().x * 0.5,
                    upwardVelocity,
                    livingTarget.getDeltaMovement().z * 0.5
                ));
                livingTarget.hurtMarked = true;
                
                // 音效播放（沿用武器模式的标记键，挂在攻击者=铁傀儡上）
                if (!attacker.getPersistentData().getBoolean("weapon_fly_up_played")) {
                    livingTarget.level().playSound(
                        null,
                        livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                        ModSounds.FLY_UP.get(),
                        SoundSource.PLAYERS,
                        0.5F,
                        1.0F
                    );
                    attacker.getPersistentData().putBoolean("weapon_fly_up_played", true);
                    attacker.getPersistentData().putInt("weapon_fly_up_reset_timer", 20);
                }
                
                // 落地附加伤害：2级->10%
                livingTarget.getPersistentData().putBoolean("shen_ying_falling", true);
                livingTarget.getPersistentData().putFloat("shen_ying_health_percentage", 0.10F);
            }
            
            // 检查所有盔甲部位的神鹰附魔等级
            int totalEnchantmentLevel = 0;
            
            // 检查头盔
            ItemStack helmet = victim.getItemBySlot(EquipmentSlot.HEAD);
            totalEnchantmentLevel += EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SHEN_YING.get(), helmet);
            
            // 检查胸甲
            ItemStack chestplate = victim.getItemBySlot(EquipmentSlot.CHEST);
            totalEnchantmentLevel += EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SHEN_YING.get(), chestplate);
            
            // 检查护腿
            ItemStack leggings = victim.getItemBySlot(EquipmentSlot.LEGS);
            totalEnchantmentLevel += EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SHEN_YING.get(), leggings);
            
            // 检查靴子
            ItemStack boots = victim.getItemBySlot(EquipmentSlot.FEET);
            totalEnchantmentLevel += EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SHEN_YING.get(), boots);
            
            // 若受害者是铁傀儡，则至少视为神鹰Lv2（等效盔甲被动）
            if (victim instanceof IronGolem) {
                totalEnchantmentLevel = Math.max(totalEnchantmentLevel, 2);
            }
            
            // 如果有神鹰附魔，则对攻击者施加飞行效果
            if (totalEnchantmentLevel > 0) {
                // 根据总附魔等级计算击飞高度（最高等级限制为10，因为现在单件可以5级）
                int effectiveLevel = Math.min(totalEnchantmentLevel, 10);
                float upwardVelocity = 0.5F + (effectiveLevel * 0.2F);
                
                // 给攻击者一个向上的速度，使其飞起来
                attacker.setDeltaMovement(new Vec3(
                    attacker.getDeltaMovement().x * 0.3, // 减少水平速度
                    upwardVelocity, // 向上的速度
                    attacker.getDeltaMovement().z * 0.3  // 减少水平速度
                ));
                attacker.hurtMarked = true; // 标记实体需要更新速度
                
                // 检查是否已播放起飞音效，防止重复播放
                if (!victim.getPersistentData().getBoolean("shen_ying_fly_up_played")) {
                    // 播放飞起来的音效
                    victim.level().playSound(
                        null, // 不指定特定玩家
                        attacker.getX(), attacker.getY(), attacker.getZ(), // 攻击者位置
                        ModSounds.FLY_UP.get(), // 声音事件
                        SoundSource.PLAYERS, // 声音类别
                        0.5F, // 音量
                        1.0F // 音调
                    );
                    
                    // 标记音效已播放
                    victim.getPersistentData().putBoolean("shen_ying_fly_up_played", true);
                    
                    // 设置延迟重置标记，避免影响后续触发
                    victim.getPersistentData().putInt("shen_ying_fly_up_reset_timer", 40); // 2秒后重置(40 ticks)
                }
                
                // 计算落地伤害百分比 - 统一伤害计算
                // 按照有效等级计算：每级5%生命值伤害
                float healthPercentage = effectiveLevel * 0.05F; // 每级5%生命值伤害
                
                // 标记攻击者将受到落地伤害
                attacker.getPersistentData().putBoolean("shen_ying_falling", true);
                attacker.getPersistentData().putFloat("shen_ying_health_percentage", healthPercentage);
            }
        }
    }
    
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        
        // 检查实体是否被神鹰附魔击中并正在坠落
        if (entity.getPersistentData().getBoolean("shen_ying_falling")) {
            float healthPercentage = entity.getPersistentData().getFloat("shen_ying_health_percentage");
            
            // 计算额外伤害，基于目标生物最大生命值的百分比
            float maxHealth = entity.getMaxHealth();
            float extraDamage = maxHealth * healthPercentage;
            
            // 造成额外伤害
            entity.hurt(entity.level().damageSources().fall(), extraDamage);
            
            // 检查生物是否存活，只有存活时才播放落地音效
            if (!entity.isDeadOrDying()) {
                // 播放落地音效
                entity.level().playSound(
                    null, // 不指定特定玩家
                    entity.getX(), entity.getY(), entity.getZ(), // 位置
                    ModSounds.MAN.get(), // 声音事件
                    SoundSource.HOSTILE, // 声音类别
                    0.3F, // 音量
                    1.0F // 音调
                );
            }
            
            // 重置标记
            entity.getPersistentData().remove("shen_ying_falling");
            entity.getPersistentData().remove("shen_ying_health_percentage");
            
            // 增加击退效果，模拟坠落冲击
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.1, 1.0));
        }
    }
    
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // 检查实体是否被神鹰附魔击中
        if (entity.getPersistentData().getBoolean("shen_ying_falling")) {
            // 播放死亡音效
            entity.level().playSound(
                null, // 不指定特定玩家
                entity.getX(), entity.getY(), entity.getZ(), // 位置
                ModSounds.HAHAHA.get(), // 声音事件
                SoundSource.HOSTILE, // 声音类别
                1.5F, // 音量
                1.0F // 音调
            );
            
            // 重置标记
            entity.getPersistentData().remove("shen_ying_falling");
            entity.getPersistentData().remove("shen_ying_health_percentage");
        }
    }
    
    // 添加tick事件监听器，用于重置音效播放标记
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        
        // 仅在服务端执行行为逻辑
        if (!entity.level().isClientSide()) {
            // 铁傀儡增强：速度V与跳跃搭方块
            if (entity instanceof IronGolem golem) {
                Level level = golem.level();
                
                // 确保速度V（放大4 => 速度5）
                MobEffectInstance speed = golem.getEffect(MobEffects.MOVEMENT_SPEED);
                if (speed == null || speed.getAmplifier() < 4 || speed.getDuration() < 200) {
                    golem.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 4, true, false)); // 20秒，隐形无粒子
                }
                
                // 跳跃搭方块（反制玩家垫高）
                {
                    Mob mob = (Mob) golem;
                    LivingEntity target = mob.getTarget();
                    if (target != null) {
                        double dy = target.getY() - golem.getY();
                        // 目标高差≥3格才尝试堆塔
                        if (dy >= 3.0) {
                            var nbt = golem.getPersistentData();
                            
                            // 冷却：每10tick尝试一次
                            int cooldown = nbt.getInt("golem_stack_cooldown");
                            if (cooldown > 0) {
                                nbt.putInt("golem_stack_cooldown", cooldown - 1);
                            }
                            
                            // 放置计时器
                            int placeTimer = nbt.getInt("golem_place_timer");
                            if (placeTimer > 0) {
                                nbt.putInt("golem_place_timer", placeTimer - 1);
                                // 到期放置：避免与实体占位冲突，放置在记录的地面位置
                                if (placeTimer == 1) {
                                    int gx = nbt.getInt("golem_last_ground_x");
                                    int gy = nbt.getInt("golem_last_ground_y");
                                    int gz = nbt.getInt("golem_last_ground_z");
                                    BlockPos placePos = new BlockPos(gx, gy, gz);
                                    // 仅在该位置为空气时放置方块
                                    if (level.isEmptyBlock(placePos)) {
                                        level.setBlock(placePos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                                    }
                                }
                            }
                            
                            // 触发一次“跳跃-记录地面-准备放置”
                            if (cooldown <= 0 && golem.onGround()) {
                                // 记录当前地面位置（脚下）
                                BlockPos ground = golem.blockPosition();
                                nbt.putInt("golem_last_ground_x", ground.getX());
                                nbt.putInt("golem_last_ground_y", ground.getY());
                                nbt.putInt("golem_last_ground_z", ground.getZ());
                                
                                // 起跳
                                golem.setDeltaMovement(golem.getDeltaMovement().x, 0.42D, golem.getDeltaMovement().z);
                                golem.hurtMarked = true;
                                
                                // 2tick后在记录位置落块
                                nbt.putInt("golem_place_timer", 2);
                                // 重置冷却
                                nbt.putInt("golem_stack_cooldown", 10);
                            }
                        }
                        
                        // 同高前进搭桥：当高度差很小(≤0.5格)时，向目标方向在脚前方/其下方铺路
                        if (Math.abs(dy) <= 0.5) {
                            var nbt = golem.getPersistentData();
                            int bcd = nbt.getInt("golem_bridge_cooldown");
                            if (bcd > 0) {
                                nbt.putInt("golem_bridge_cooldown", bcd - 1);
                            }
                            if (bcd <= 0) {
                                // 选取朝向目标的主轴方向
                                double vx = target.getX() - golem.getX();
                                double vz = target.getZ() - golem.getZ();
                                int stepX = 0, stepZ = 0;
                                if (Math.abs(vx) > Math.abs(vz)) {
                                    stepX = vx >= 0 ? 1 : -1;
                                } else {
                                    stepZ = vz >= 0 ? 1 : -1;
                                }
                                
                                BlockPos base = golem.blockPosition();
                                BlockPos front = base.offset(stepX, 0, stepZ);
                                BlockPos frontBelow = front.below();
                                boolean placed = false;
                                
                                // 若前方下方是空气，先放支撑
                                if (level.isEmptyBlock(frontBelow)) {
                                    level.setBlock(frontBelow, Blocks.COBBLESTONE.defaultBlockState(), 3);
                                    placed = true;
                                }
                                // 否则在前方脚下位置放块
                                if (!placed && level.isEmptyBlock(front)) {
                                    level.setBlock(front, Blocks.COBBLESTONE.defaultBlockState(), 3);
                                    placed = true;
                                }
                                
                                if (placed) {
                                    nbt.putInt("golem_bridge_cooldown", 5); // 5tick冷却，避免刷块过快
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 检查是否有旧的音效重置计时器（兼容性）
        if (entity.getPersistentData().contains("fly_up_reset_timer")) {
            int timer = entity.getPersistentData().getInt("fly_up_reset_timer");
            if (timer <= 0) {
                // 重置音效播放标记
                entity.getPersistentData().remove("fly_up_played");
                entity.getPersistentData().remove("fly_up_reset_timer");
            } else {
                // 减少计时器
                entity.getPersistentData().putInt("fly_up_reset_timer", timer - 1);
            }
        }
        
        // 检查武器攻击音效重置计时器
        if (entity.getPersistentData().contains("weapon_fly_up_reset_timer")) {
            int timer = entity.getPersistentData().getInt("weapon_fly_up_reset_timer");
            if (timer <= 0) {
                // 重置武器音效播放标记
                entity.getPersistentData().remove("weapon_fly_up_played");
                entity.getPersistentData().remove("weapon_fly_up_reset_timer");
            } else {
                // 减少计时器
                entity.getPersistentData().putInt("weapon_fly_up_reset_timer", timer - 1);
            }
        }
        
        // 检查盔甲被动防护音效重置计时器
        if (entity.getPersistentData().contains("shen_ying_fly_up_reset_timer")) {
            int timer = entity.getPersistentData().getInt("shen_ying_fly_up_reset_timer");
            if (timer <= 0) {
                // 重置盔甲音效播放标记
                entity.getPersistentData().remove("shen_ying_fly_up_played");
                entity.getPersistentData().remove("shen_ying_fly_up_reset_timer");
            } else {
                // 减少计时器
                entity.getPersistentData().putInt("shen_ying_fly_up_reset_timer", timer - 1);
            }
        }
    }
}