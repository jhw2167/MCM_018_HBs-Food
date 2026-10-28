package com.holybuckets.food.enchantment;

import com.holybuckets.food.Constants;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.DeferredObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {

    public static DeferredObject<Enchantment> POTENCY;

    public static void register() {
        POTENCY = Balm.getRegistries().register(
            BuiltInRegistries.ENCHANTMENT,
            id -> new PotencyEnchantment(),
            id("potency")
        );
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }
}
