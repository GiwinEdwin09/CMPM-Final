package com.example.minecraftquests;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class QuestBlock extends Block {
    
    public QuestBlock() {
        super(Material.ROCK);
        setUnlocalizedName(MinecraftQuests.MODID + ".quest_block");
        setRegistryName("quest_block");
        setCreativeTab(CreativeTabs.MISC);
        setHardness(1.5F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
    }
    
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, 
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            // This code runs on the server side
            playerIn.sendMessage(new TextComponentString("§6[Quest] §rHello " + playerIn.getName() + "! This is your quest block speaking."));
            playerIn.sendMessage(new TextComponentString("§6[Quest] §rYou can add custom quest logic here."));
            
            // Example of custom code you might want to add:
            // checkQuestProgress(playerIn);
            // offerQuest(playerIn);
            // completeQuest(playerIn);
            
            return true;
        }
        
        return true; // Return true on both client and server to prevent further processing
    }
    
    // Example method for custom quest logic
    private void checkQuestProgress(EntityPlayer player) {
        // Here you can implement custom quest logic
        // For example, check if player has certain items, or has completed certain tasks
    }
}