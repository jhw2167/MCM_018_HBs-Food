package com.holybuckets.food.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.holybuckets.food.client.HealthyEffectClient;
import com.holybuckets.food.config.ModConfig;
import com.holybuckets.food.effect.ModEffects;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.LivingDeathEvent;
import net.blay09.mods.balm.api.event.PlayerLogoutEvent;
import net.blay09.mods.balm.api.event.UseItemEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DietManager {

    private static final String CLASS_ID = "003";

    private static final Map<String, EatingFood> EATING_FOODS = new ConcurrentHashMap<>();
    private static final Map<String, DietStats> DIET_STATS = new ConcurrentHashMap<>();
    private static final Map<String, Map<MobEffect, Integer>> GRANTED_EFFECTS = new ConcurrentHashMap<>();

    public record EatingFood(ServerPlayer player, Item item, long startTick) {}

    public static void init(EventRegistrar registrar) {
        Balm.getEvents().onEvent(UseItemEvent.class, DietManager::onPlayerUseItem);
        registrar.registerOnPlayerDeath(DietManager::onPlayerDeath);
        registrar.registerOnPlayerLogout(DietManager::onPlayerLogout);
        registrar.registerOnServerTick(TickType.ON_20_TICKS, DietManager::on20Ticks);
        registrar.registerOnServerStopped(e -> { EATING_FOODS.clear(); GRANTED_EFFECTS.clear(); DIET_STATS.clear(); });
    }


    //** START EATING

    private static void onPlayerUseItem(UseItemEvent event)
    {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getItemInHand(event.getHand());
        FoodProperties food = stack.getItem().getFoodProperties();
        if (food == null || !player.canEat(food.canAlwaysEat())) return;

        EATING_FOODS.put(HBUtil.PlayerUtil.getId(player), new EatingFood(player, stack.getItem(), player.level().getGameTime()));
    }

    @Nullable
    public static EatingFood getEatingFood(Player player) {
        return EATING_FOODS.get(HBUtil.PlayerUtil.getId(player));
    }

    public static boolean isEating(Player player) {
        return EATING_FOODS.containsKey(HBUtil.PlayerUtil.getId(player));
    }


    //** FINISH EATING
    public static void onPlayerFinishEating(Player p, ItemStack stack)
    {
        if (!(p instanceof ServerPlayer player)) return;
        EATING_FOODS.remove(HBUtil.PlayerUtil.getId(player));

        Item item = stack.getItem();
        FoodProperties food = item.getFoodProperties();
        if (food == null) return;

        addRegenBudget(player, item);

        DietStats stats = getDietStats(player);
        Map<MobEffect, Integer> granted = getGrantedEffects(player);
        List<ModConfig.BonusEffect> foodEffects = applyFoodGroupBonuses(player, item, food, granted);
        boolean applied = !foodEffects.isEmpty();
        applied |= applySaturationBonus(player, item, food, stats.previousFoodEffects, foodEffects, granted);

        if (!foodEffects.isEmpty()) stats.previousFoodEffects = new ArrayList<>(foodEffects);
        if (applied) grantHealthy(player, granted);
    }

    private static List<ModConfig.BonusEffect> applyFoodGroupBonuses(ServerPlayer player, Item item, FoodProperties food, Map<MobEffect, Integer> granted)
    {
        ModConfig config = ModConfig.getInstance();
        int duration = config.getLingeringDuration(item, config.getBonusEffectDurationTicks(food.getNutrition()));
        if (duration <= 0) return List.of();

        int levelIncrease = config.getRobustLevelIncrease(item);
        List<ModConfig.BonusEffect> applied = new ArrayList<>();
        for (ModConfig.BonusEffect bonus : config.getFoodEffects(item)) {
            ModConfig.BonusEffect boosted = bonus.withLevelIncrease(levelIncrease);
            applyBonus(player, boosted, duration, granted);
            applied.add(boosted);
        }
        return applied;
    }

    /** Saturation re-grants one random effect from the previous food, preferring one this food did not just grant. */
    private static boolean applySaturationBonus(ServerPlayer player, Item item, FoodProperties food,
        List<ModConfig.BonusEffect> previousEffects, List<ModConfig.BonusEffect> foodEffects, Map<MobEffect, Integer> granted)
    {
        if (previousEffects.isEmpty()) return false;
        ModConfig config = ModConfig.getInstance();
        int duration = config.getLingeringDuration(item, config.getSaturationEffectDurationTicks(ModConfig.getSaturationPoints(food)));
        if (duration <= 0) return false;

        List<ModConfig.BonusEffect> options = new ArrayList<>();
        for (ModConfig.BonusEffect previous : previousEffects) {
            boolean alreadyGranted = foodEffects.stream().anyMatch(e -> e.effect() == previous.effect());
            if (!alreadyGranted) options.add(previous);
        }
        if (options.isEmpty()) options = previousEffects;

        ModConfig.BonusEffect bonus = options.get(player.getRandom().nextInt(options.size()));
        applyBonus(player, bonus, duration, granted);
        return true;
    }

    private static void applyBonus(ServerPlayer player, ModConfig.BonusEffect bonus, int duration, Map<MobEffect, Integer> granted) {
        player.addEffect(bonus.create(duration));
        granted.merge(bonus.effect(), bonus.amplifier(), Math::max);
    }


    //** HEALTHY EFFECT

    /** Returns the player's granted effects, dropping any that expired or were replaced by a visible effect. */
    private static Map<MobEffect, Integer> getGrantedEffects(ServerPlayer player) {
        Map<MobEffect, Integer> granted = GRANTED_EFFECTS.computeIfAbsent(HBUtil.PlayerUtil.getId(player), k -> new LinkedHashMap<>());
        granted.keySet().removeIf(effect -> {
            MobEffectInstance active = player.getEffect(effect);
            return active == null || active.showIcon();
        });
        return granted;
    }

    /**
     * Healthy gains one level per active food effect. Levels are layered so each higher level lasts as long as
     * the effect that supports it, then vanilla's hidden effect handling drops Healthy back a level.
     */
    private static void grantHealthy(ServerPlayer player, Map<MobEffect, Integer> granted)
    {
        List<Integer> durations = new ArrayList<>();
        for (MobEffect effect : granted.keySet()) {
            MobEffectInstance active = player.getEffect(effect);
            if (active != null && active.getDuration() > 0) durations.add(active.getDuration());
        }
        if (durations.isEmpty()) return;
        durations.sort(Comparator.reverseOrder());

        MobEffect healthy = ModEffects.HEALTHY.get();
        player.removeEffect(healthy);
        for (int level = 0; level < durations.size(); level++) {
            player.addEffect(new MobEffectInstance(healthy, durations.get(level), level, false, false, true));
        }

        ModConfig.BonusEffect healthyBonus = applyHealthyBonus(player, durations);
        sendGrantedEffects(player, granted, healthyBonus);
    }

    /** Applies the healthy bonus effect for as long as Healthy stays at or above the minimum level. */
    @Nullable
    private static ModConfig.BonusEffect applyHealthyBonus(ServerPlayer player, List<Integer> sortedDurations)
    {
        ModConfig config = ModConfig.getInstance();
        ModConfig.BonusEffect bonus = config.getHealthyBonusEffect();
        int minLevel = config.getMinHealthyEffectLevelForBonusEffect();
        if (bonus == null || sortedDurations.size() < minLevel) return null;

        player.addEffect(bonus.create(sortedDurations.get(minLevel - 1)));
        return bonus;
    }

    //Payload is [{"id":"minecraft:strength","amp":0}, ...]
    private static void sendGrantedEffects(ServerPlayer player, Map<MobEffect, Integer> granted, @Nullable ModConfig.BonusEffect healthyBonus)
    {
        JsonArray array = new JsonArray();
        for (Map.Entry<MobEffect, Integer> entry : granted.entrySet()) {
            array.add(toJson(entry.getKey(), entry.getValue()));
        }
        if (healthyBonus != null) array.add(toJson(healthyBonus.effect(), healthyBonus.amplifier()));
        SimpleStringMessage.createAndFire(player, HealthyEffectClient.MSG_ID_HEALTHY_EFFECTS, array.toString());
    }

    private static JsonObject toJson(MobEffect effect, int amplifier) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", BuiltInRegistries.MOB_EFFECT.getKey(effect).toString());
        obj.addProperty("amp", amplifier);
        return obj;
    }


    //** REGEN CAP

    private static void addRegenBudget(ServerPlayer player, Item item)
    {
        ModConfig config = ModConfig.getInstance();
        if (!config.isFoodRegenCapEnabled()) return;
        float regen = config.getHealthRegen(item);
        if (regen <= 0f) return;

        DietStats stats = getDietStats(player);
        stats.regenBudget = Math.max(stats.regenBudget, regen);
    }

    // Mixin to minecraft regen hook
    public static boolean canRegen(Player player)
    {
        if (!player.isHurt()) return false;
        if (!ModConfig.getInstance().isFoodRegenCapEnabled()) return true;
        return getDietStats(player).regenBudget > 0f;
    }

    //Caps the heal from food regen to the players remaining budget
    public static void capHeal(Player player, float amount)
    {
        if (!ModConfig.getInstance().isFoodRegenCapEnabled()) {
            player.heal(amount);
            return;
        }

        DietStats stats = getDietStats(player);
        float healed = Math.min(amount, stats.regenBudget);
        if (healed <= 0f) return;
        stats.regenBudget = Math.max(0f, stats.regenBudget - healed);
        player.heal(healed);
    }

    public static float getRegenBudget(Player player) {
        return getDietStats(player).regenBudget;
    }

    /** Player instances change on respawn, so the stored reference is refreshed on every lookup. */
    public static DietStats getDietStats(Player player) {
        DietStats stats = DIET_STATS.computeIfAbsent(HBUtil.PlayerUtil.getId(player), k -> new DietStats(player));
        stats.p = player;
        return stats;
    }

    private static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DietStats stats = getDietStats(player);
        stats.regenBudget = 0f;
        stats.previousFoodEffects.clear();
    }


    //** CLEANUP

    /** Drops players who started eating but cancelled or switched items. */
    private static void on20Ticks(ServerTickEvent event)
    {
        if (EATING_FOODS.isEmpty()) return;
        Iterator<EatingFood> it = EATING_FOODS.values().iterator();
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
        String id = HBUtil.PlayerUtil.getId(event.getPlayer());
        EATING_FOODS.remove(id);
        GRANTED_EFFECTS.remove(id);
    }


    //** Classes

    /** Per player diet state, kept in memory only for the server session. */
    public static class DietStats {
        float regenBudget;
        List<ModConfig.BonusEffect> previousFoodEffects = new ArrayList<>();
        Player p;
        public DietStats(Player player) {
            this.regenBudget = 0f;
            this.p = player;
        }
    }

}
