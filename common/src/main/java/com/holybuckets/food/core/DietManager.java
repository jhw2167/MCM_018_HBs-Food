package com.holybuckets.food.core;

import com.holybuckets.food.config.ModConfig;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.PlayerLogoutEvent;
import net.blay09.mods.balm.api.event.UseItemEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class: DietManager
 * Description: Tracks when players start and finish eating and applies food group and saturation bonuses.
 */
public class DietManager {

    private static final String CLASS_ID = "003";

    private static final Map<UUID, EatingFood> EATING = new ConcurrentHashMap<>();

    public record EatingFood(ServerPlayer player, Item item, long startTick) {}

    public static void init(EventRegistrar registrar) {
        Balm.getEvents().onEvent(UseItemEvent.class, DietManager::onPlayerUseItem);
        registrar.registerOnPlayerLogout(DietManager::onPlayerLogout);
        registrar.registerOnServerTick(TickType.ON_20_TICKS, DietManager::on20Ticks);
        registrar.registerOnServerStopped(e -> EATING.clear());
    }


    //** START EATING

    private static void onPlayerUseItem(UseItemEvent event)
    {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getItemInHand(event.getHand());
        FoodProperties food = stack.getItem().getFoodProperties();
        if (food == null || !player.canEat(food.canAlwaysEat())) return;

        EATING.put(player.getUUID(), new EatingFood(player, stack.getItem(), player.level().getGameTime()));
    }

    @Nullable
    public static EatingFood getEatingFood(Player player) {
        return EATING.get(player.getUUID());
    }

    public static boolean isEating(Player player) {
        return EATING.containsKey(player.getUUID());
    }


    //** FINISH EATING

    /** Called from MixinPlayer when a food is fully consumed, before the stack shrinks. */
    public static void onPlayerFinishEating(Player p, ItemStack stack)
    {
        if (!(p instanceof ServerPlayer player)) return;
        EATING.remove(player.getUUID());

        Item item = stack.getItem();
        FoodProperties food = item.getFoodProperties();
        if (food == null) return;

        applyFoodGroupBonuses(player, item, food);
        applySaturationBonuses(player, food);
    }

    private static void applyFoodGroupBonuses(ServerPlayer player, Item item, FoodProperties food)
    {
        ModConfig config = ModConfig.getInstance();
        int duration = config.getBonusEffectDurationTicks(food.getNutrition());
        if (duration <= 0) return;

        Collection<ModConfig.BonusEffect> effects = config.getFoodEffects(item);
        for (ModConfig.BonusEffect bonus : effects) {
            player.addEffect(bonus.create(duration));
        }
    }

    private static void applySaturationBonuses(ServerPlayer player, FoodProperties food)
    {
        ModConfig config = ModConfig.getInstance();
        int duration = config.getSaturationEffectDurationTicks(ModConfig.getSaturationPoints(food));
        if (duration <= 0) return;

        for (ModConfig.BonusEffect bonus : config.getSaturationEffects()) {
            player.addEffect(bonus.create(duration));
        }
    }


    //** CLEANUP

    /** Drops players who started eating but cancelled or switched items. */
    private static void on20Ticks(ServerTickEvent event)
    {
        if (EATING.isEmpty()) return;
        Iterator<EatingFood> it = EATING.values().iterator();
        while (it.hasNext())
        {
            EatingFood eating = it.next();
            ServerPlayer player = eating.player();
            if (player.isRemoved() || !player.isUsingItem() || player.getUseItem().getItem() != eating.item()) {
                it.remove();
            }
        }
    }

    private static void onPlayerLogout(PlayerLogoutEvent event) {
        EATING.remove(event.getPlayer().getUUID());
    }

}
