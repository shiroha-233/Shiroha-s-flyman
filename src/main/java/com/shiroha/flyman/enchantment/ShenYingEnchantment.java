package com.shiroha.flyman.enchantment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import com.shiroha.flyman.sound.ModSounds;
import net.minecraft.world.item.Items;

public class ShenYingEnchantment extends Enchantment {
    
    public ShenYingEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }
    
    @Override
    public int getMinLevel() {
        return 1;
    }
    
    @Override
    public int getMaxLevel() {
        return 3; // 附魔最高等级为3
    }
    
    @Override
    public int getMinCost(int level) {
        return 15 + (level - 1) * 9;
    }
    
    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }
    
    @Override
    public boolean canApplyAtEnchantingTable(net.minecraft.world.item.ItemStack stack) {
        // 可以附魔在武器、盔甲和木棍上
        return EnchantmentCategory.WEAPON.canEnchant(stack.getItem()) ||
               EnchantmentCategory.ARMOR.canEnchant(stack.getItem()) ||
               EnchantmentCategory.BREAKABLE.canEnchant(stack.getItem()) ||
               stack.getItem() == Items.STICK;
    }
    
    @Override
    public boolean canEnchant(net.minecraft.world.item.ItemStack stack) {
        // 可以附魔在武器、盔甲和木棍上
        return EnchantmentCategory.WEAPON.canEnchant(stack.getItem()) ||
               EnchantmentCategory.ARMOR.canEnchant(stack.getItem()) ||
               EnchantmentCategory.BREAKABLE.canEnchant(stack.getItem()) ||
               stack.getItem() == Items.STICK;
    }
    
    @Override
    public void doPostAttack(LivingEntity attacker, Entity target, int level) {
        // 当攻击目标时触发效果
        if (!attacker.level().isClientSide() && target instanceof LivingEntity) {
            LivingEntity livingTarget = (LivingEntity) target;
            
            // 检查是否是木棍，如果是则不造成伤害
            boolean isStick = attacker.getMainHandItem().getItem() == Items.STICK;
            
            // 根据附魔等级计算击飞高度
            // 调整后：
            // 1级：飞行高度约10格
            // 2级：飞行高度约15格
            // 3级：飞行高度约20格
            // 调整飞行高度值以适应Minecraft的物理系统
            float upwardVelocity = 0.75F + (level * 0.5F); // 调整后的向上速度
            
            // 给目标一个向上的速度，使其飞起来
            livingTarget.setDeltaMovement(new Vec3(
                livingTarget.getDeltaMovement().x * 0.5, // 减少水平速度
                upwardVelocity, // 向上的速度
                livingTarget.getDeltaMovement().z * 0.5  // 减少水平速度
            ));
            livingTarget.hurtMarked = true; // 标记实体需要更新速度
            
            // 检查是否已播放起飞音效，防止重复播放
            if (!attacker.getPersistentData().getBoolean("fly_up_played")) {
                // 播放飞起来的音效
                livingTarget.level().playSound(
                    null, // 不指定特定玩家
                    livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(), // 位置
                    ModSounds.FLY_UP.get(), // 声音事件
                    SoundSource.PLAYERS, // 声音类别
                    0.5F, // 音量
                    1.0F // 音调
                );
                
                // 标记音效已播放
                attacker.getPersistentData().putBoolean("fly_up_played", true);
                
                // 设置延迟重置标记，避免影响后续攻击
                attacker.getPersistentData().putInt("fly_up_reset_timer", 20); // 1秒后重置(20 ticks)
            }
            
            // 如果不是木棍，则在目标落地时造成额外伤害
            if (!isStick) {
                float healthPercentage = 0.3F + (level * 0.3F); // 生命值百分比伤害
                
                // 创建一个任务，在目标落地时造成额外伤害
                livingTarget.getPersistentData().putBoolean("shen_ying_falling", true);
                livingTarget.getPersistentData().putFloat("shen_ying_health_percentage", healthPercentage);
            }
        }
    }
}