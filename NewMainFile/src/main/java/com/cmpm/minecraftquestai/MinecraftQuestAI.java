package com.cmpm.minecraftquestai;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(MinecraftQuestAI.MODID)
public class MinecraftQuestAI {
    public static final String MODID = "minecraft_quest_ai";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Create Deferred Registers
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Register Quest Block
    public static final RegistryObject<Block> QUEST_BLOCK = BLOCKS.register("quest_block",
            () -> new QuestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(3.0f)));

    // Register Quest Block Item
    public static final RegistryObject<Item> QUEST_BLOCK_ITEM = ITEMS.register("quest_block",
            () -> new BlockItem(QUEST_BLOCK.get(), new Item.Properties()));

    // Register Quest Token Item
    public static final RegistryObject<Item> QUEST_TOKEN = ITEMS.register("quest_token",
            () -> new Item(new Item.Properties()));

    // Create a Creative Tab
    public static final RegistryObject<CreativeModeTab> QUEST_TAB = CREATIVE_MODE_TABS.register("quest_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> QUEST_TOKEN.get().getDefaultInstance())
                    .title(Component.translatable("creativemodetab.quest_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(QUEST_BLOCK_ITEM.get());
                        output.accept(QUEST_TOKEN.get());
                    }).build());

    // Quest Manager
    public static QuestManager questManager;

    public MinecraftQuestAI() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Registers
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        // Register item to creative tabs
        modEventBus.addListener(this::addCreative);

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Initialize quest manager
        questManager = new QuestManager();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM MINECRAFT QUESTS MOD");

        // Register quests
        event.enqueueWork(() -> {
            questManager.registerQuest(new ItemCollectionQuest("collect_dirt", "Collect Dirt",
                     "minecraft:dirt", 10));
            questManager.registerQuest(new EnemyKillQuest("kill_zombie", "Kill Zombies",
                     "minecraft:zombie", 5));
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(QUEST_BLOCK_ITEM.get());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Quest system initializing on server");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("MINECRAFT QUESTS CLIENT SETUP COMPLETE");
        }
    }
}
