package com.shiroha.flyman;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("flyman")
public class Flyman {
    public static final String MODID = "flyman";

    public Flyman() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // 注册附魔
        com.shiroha.flyman.enchantment.ModEnchantments.register(modEventBus);
        
        // 注册声音
        com.shiroha.flyman.sound.ModSounds.register(modEventBus);
    }
}