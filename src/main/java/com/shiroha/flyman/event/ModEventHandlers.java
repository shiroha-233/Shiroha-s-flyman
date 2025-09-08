package com.shiroha.flyman.event;

import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.shiroha.flyman.sound.ModSounds;

@Mod.EventBusSubscriber(modid = "flyman")
public class ModEventHandlers {
    
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
        
        // 检查是否有音效重置计时器
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
    }
}