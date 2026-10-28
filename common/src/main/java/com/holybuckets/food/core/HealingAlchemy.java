package com.holybuckets.food.core;

import com.holybuckets.food.config.ModConfig;
import com.holybuckets.food.enchantment.ModEnchantments;
import com.holybuckets.food.enchantment.PotencyEnchantment;
import com.holybuckets.food.item.ModItems;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.BrewingUpdateEvent;
import com.holybuckets.foundation.event.custom.PlayerCraftedEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class HealingAlchemy {

    private static final String CLASS_ID = "005";

    public static void init(EventRegistrar registrar) {
        registrar.registerOnPlayerCrafted(() -> ModItems.healingPowder.get(), HealingAlchemy::onHealingPowderCrafted);
        registrar.registerOnBrewingUpdate(new BrewingUpdateEvent.PredicateDriven(HealingAlchemy::isBrewableHealingPowder,
            HealingAlchemy::isAwkwardPotion), HealingAlchemy::onBrewingUpdate);

    }


    //** CRAFTING
    private static void onHealingPowderCrafted(PlayerCraftedEvent event)
    {
        CraftingContainer grid = event.getCraftSlots();
        if (grid == null) return;

        int totalHunger = 0;
        for (int i = 0; i < grid.getContainerSize(); i++)
        {
            ItemStack stack = grid.getItem(i);
            FoodProperties food = stack.isEmpty() ? null : stack.getItem().getFoodProperties();
            if (food == null) continue;
            totalHunger += food.getNutrition();
        }

        ModConfig config = ModConfig.getInstance();
        float health = (float) totalHunger / config.getHungerPointsPerHealthPoint();
        int potency = Math.min(roundRandomly(health), config.getMaxHealingPotency());
        if (potency < 1) return;
        setPotency(event.getCraftedItem(), potency);
    }

    private static int roundRandomly(float value) {
        int whole = (int) Math.floor(value);
        float fraction = value - whole;
        if (fraction <= 0f) return whole;
        GeneralConfig C = GeneralConfig.getInstance();
        long seed = C.getWorldSeed() * C.getTotalTickCount();
        return RandomSource.create(seed).nextFloat() < fraction ? whole + 1 : whole;
    }


    //** BREWING

    private static boolean isBrewableHealingPowder(ItemStack stack) {
        return stack.is(ModItems.healingPowder.get()) && getPotency(stack) > 0;
    }

    private static boolean isAwkwardPotion(ItemStack stack) {
        return stack.is(Items.POTION) && PotionUtils.getPotion(stack) == Potions.AWKWARD;
    }

    /** Runs identically for CHECK and BREW; every awkward bottle receives the powder's full Potency. */
    private static void onBrewingUpdate(BrewingUpdateEvent event)
    {
        int potency = Math.min(getPotency(event.getIngredient()), PotencyEnchantment.MAX_LEVEL);
        if (potency < 1) return;
        event.setResultItem(withPotency(ModItems.healingPotion.get(), potency));
    }



    //** UTIL

    public static int getPotency(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.POTENCY.get(), stack);
    }

    public static ItemStack withPotency(Item item, int potency) {
        ItemStack stack = new ItemStack(item);
        setPotency(stack, potency);
        return stack;
    }


    public static void setPotency(ItemStack stack, int potency) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        enchantments.put(ModEnchantments.POTENCY.get(), potency);
        EnchantmentHelper.setEnchantments(enchantments, stack);
    }


}
