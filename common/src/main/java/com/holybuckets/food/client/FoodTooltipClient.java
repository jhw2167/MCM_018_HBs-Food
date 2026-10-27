package com.holybuckets.food.client;

import com.holybuckets.food.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;


public class FoodTooltipClient {

    private FoodTooltipClient() {}

    public static void addFoodTags(ItemStack stack, List<Component> tooltip)
    {
        if (stack.isEmpty() || !stack.isEdible() || tooltip.isEmpty()) return;
        List<Component> tags = getFoodTags(stack.getItem());
        if (tags.isEmpty()) return;

        MutableComponent line = Component.empty();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) line.append(", ");
            line.append(tags.get(i));
        }
        tooltip.add(1, line.withStyle(ChatFormatting.GRAY));
    }

    public static List<Component> getFoodTags(Item item)
    {
        ModConfig config = ModConfig.getInstance();
        List<Component> tags = new ArrayList<>();
        for (String group : config.getFoodGroups(item)) {
            tags.add(Component.literal(group).withStyle(ChatFormatting.BLUE));
        }

        if (config.isRobustFood(item)) tags.add(tag("robust"));
        if (config.isLingeringFood(item)) tags.add(tag("lingering"));
        if (config.isFoodRegenCapEnabled() && config.getHealthRegen(item) > 0f) tags.add(tag("healthful"));
        if (config.isQuickFood(item)) tags.add(tag("quick"));
        if (config.isSlowFood(item)) tags.add(tag("slow"));
        return tags;
    }

    private static Component tag(String name) {
        return Component.translatable("tooltip.hbs_food.tag." + name).withStyle(ChatFormatting.BLUE);
    }
}
