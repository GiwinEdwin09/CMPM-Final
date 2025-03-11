package com.example.minecraftquests;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.Logger;
import net.minecraft.block.Block;

@Mod(modid = MinecraftQuests.MODID, name = MinecraftQuests.NAME, version = MinecraftQuests.VERSION)
public class MinecraftQuests {
    public static final String MODID = "minecraftquests";
    public static final String NAME = "Minecraft Quests";
    public static final String VERSION = "1.0.0";

    private static Logger logger;
    
    public static Block questBlock;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        questBlock = new QuestBlock();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("MinecraftQuests mod initialized");
    }
    
    @Mod.EventBusSubscriber
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
            event.getRegistry().register(questBlock);
        }
        
        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            event.getRegistry().register(new ItemBlock(questBlock).setRegistryName(questBlock.getRegistryName()));
        }
    }
}