package com.holybuckets.food.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * Health points restored by a Healing Powder and the Healing Potion brewed from it.
 * Only ever applied by HealingAlchemy, never by tables, anvils, trades or loot.
 */
public class PotencyEnchantment extends Enchantment {

    public static final int MAX_LEVEL = 20;

    public PotencyEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.VANISHABLE, new EquipmentSlot[]{});
    }

    @Override
    public int getMaxLevel() { return MAX_LEVEL; }

    @Override
    public boolean canEnchant(ItemStack stack) { return false; }

    @Override
    public boolean isTreasureOnly() { return true; }

    @Override
    public boolean isTradeable() { return false; }

    @Override
    public boolean isDiscoverable() { return false; }
}
