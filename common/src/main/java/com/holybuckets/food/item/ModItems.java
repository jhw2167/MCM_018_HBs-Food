package com.holybuckets.food.item;


import com.holybuckets.food.Constants;
import com.holybuckets.food.block.ModBlocks;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.item.BalmItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModItems {
    public static DeferredObject<CreativeModeTab> creativeModeTab;
    public static Item emptyBlockItem;
    public static DeferredObject<Item> healingPowder;
    public static DeferredObject<Item> healingPotion;

    public static void initialize(BalmItems items) {
        healingPowder = items.registerItem(rl -> new HealingPowderItem(items.itemProperties()), id("healing_powder"), id(Constants.MOD_ID));
        healingPotion = items.registerItem(rl -> new HealingPotionItem(items.itemProperties().stacksTo(1)), id("healing_potion"), id(Constants.MOD_ID));
        //items.registerItem(() -> emptyBlockItem = new EmptyBlockItem(items.itemProperties()), id("empty_block"));
        creativeModeTab = items.registerCreativeModeTab(id(Constants.MOD_ID), () -> new ItemStack(ModBlocks.templateBlock));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

}
