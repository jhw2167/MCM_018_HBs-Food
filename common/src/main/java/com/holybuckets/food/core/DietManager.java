package com.holybuckets.food.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.holybuckets.food.client.HealthyEffectClient;
import com.holybuckets.food.config.BonusEffect;
import com.holybuckets.food.config.ModConfig;
import com.holybuckets.food.effect.ModEffects;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.LivingDeathEvent;
import net.blay09.mods.balm.api.event.UseItemEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DietManager {

    private static final String CLASS_ID = "003";

    private static final Map<String, EatingFood> EATING_FOODS = new ConcurrentHashMap<>();
    private static final Map<String, DietStats> DIET_STATS = new ConcurrentHashMap<>();
    private static final Map<String, Map<MobEffect, Integer>> GRANTED_EFFECTS = new ConcurrentHashMap<>();

    private static ModConfig CONFIG;

    public record EatingFood(ServerPlayer player, Item item, long startTick) {}

    public static void init(EventRegistrar registrar) {
        Balm.getEvents().onEvent(UseItemEvent.class, DietManager::onPlayerUseItem);
        registrar.registerOnBeforeServerStarted(e -> CONFIG = ModConfig.getInstance());
        registrar.registerOnPlayerDeath(DietManager::onPlayerDeath);
        //registrar.registerOnPlayerLogout(DietManager::onPlayerLogout);
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


    //** FINISH EATING
    public static void onPlayerFinishEating(Player p, ItemStack stack)
    {
        if (!(p instanceof ServerPlayer player)) return;
        EATING_FOODS.remove(HBUtil.PlayerUtil.getId(player));

        Item item = stack.getItem();
        FoodProperties food = item.getFoodProperties();
        if (food == null) return;

        addRegenBudget(player, item);
        if(!CONFIG.isEligibleForBonusEffect(stack.getItem())) return;

        DietStats stats = getDietStats(player);
        Map<BonusEffect, MobEffectInstance> foodEffects = applyFoodGroupBonuses(player, item);
        Map<BonusEffect, MobEffectInstance> allEffects = applySaturationBonus(player, item,
            stats.previousFoodEffects, foodEffects);

        stats.previousFoodEffects = foodEffects.keySet().stream().toList();
        grantHealthyBonusEffect(player, allEffects);
        sendHealthStatusToClient(player, allEffects.values());
    }

    private static Map<BonusEffect, MobEffectInstance> applyFoodGroupBonuses(ServerPlayer player, Item item)
    {
        List<BonusEffect> bonusEffects = CONFIG.getBonusEffects(item);
        if (bonusEffects.isEmpty()) return Map.of();

        int duration = CONFIG.getDuration(item);
        if (duration <= 0) return Map.of();

        int levelIncrease = CONFIG.getRobustLevelIncrease(item);
        Map<BonusEffect, MobEffectInstance> applied = new HashMap<>();
        for (BonusEffect bonus : bonusEffects) {
            BonusEffect boosted = bonus.withLevelIncrease(levelIncrease);
            applied.put(boosted, applyBonus(player, boosted, duration));
        }
        return applied;
    }

    //Saturation re-grants one random effect from the previous food */
    private static Map<BonusEffect, MobEffectInstance> applySaturationBonus(ServerPlayer player, Item item, List<BonusEffect> previousEffects,
                                                                            Map<BonusEffect, MobEffectInstance> currentEffects)
    {
        if (previousEffects.isEmpty()) return currentEffects;
        int duration = CONFIG.getDuration(item);

        BonusEffect bonus = previousEffects.stream()
            .filter(b -> !currentEffects.containsKey(b))
            .findFirst().orElse(null);
        if (bonus == null) return currentEffects;

        Map<BonusEffect, MobEffectInstance> newMap = new HashMap<>(currentEffects);
        newMap.put(bonus, applyBonus(player, bonus, duration));
        return newMap;
    }

    //check if the player has the effect and take the max duration
    private static MobEffectInstance applyBonus(ServerPlayer player, BonusEffect bonus, int duration) {
        MobEffectInstance intendToApply = bonus.create(duration);
        MobEffectInstance active = player.getEffect(bonus.effect);
        if(active == null ) {
            player.addEffect(intendToApply);
            return intendToApply;
        } else if(active.getAmplifier() <= intendToApply.getAmplifier()) {
            if(active.getDuration() <= intendToApply.getDuration())
                player.addEffect(intendToApply);
            return intendToApply;
        } else {
            return intendToApply;
        }
    }


    //** HEALTHY EFFECT

    /**
     * This mod hides the true mob effects (Swiftness, Strength, etc.) and grants a Single "Healthy" effect instead.
     * For example, eating one steak grants Strength I and would visually show "Healthy I" with a note telling
     * the player they have Strength. Eating a meat and bread sandwich (from a mod) would grant Strength I and Swiftness I
     * and would be denoted as "Healthy II".
     *
     * If the player has the minimum level of Healthy, they are granted an additional bonus for being Healthy.
     */
    private static void grantHealthyBonusEffect(ServerPlayer player, Map<BonusEffect, MobEffectInstance> granted)
    {
        if(granted.isEmpty()) return;
        int healthyDuration = Integer.MAX_VALUE;
        int healthyLevel = 0;
        for (BonusEffect effect : granted.keySet()) {
            MobEffectInstance instance = granted.get(effect);
            if(instance.getDuration()<healthyDuration) healthyDuration = instance.getDuration();
            healthyLevel+= effect.effectLevel;
        }

        int minLevel = CONFIG.getMinHealthyEffectLevelForBonusEffect();
        if(healthyLevel<minLevel) return;

        MobEffect healthyEffect = ModEffects.HEALTHY.get();
        player.removeEffect(healthyEffect);
        BonusEffect bonus = CONFIG.getHealthyBonusEffect();
        MobEffectInstance healthyEffectInstance = bonus.create(healthyDuration, true);
        player.addEffect(healthyEffectInstance);
        granted.put(bonus, healthyEffectInstance);
    }

    private static void sendHealthStatusToClient(ServerPlayer player, Collection<MobEffectInstance> allEffects)
    {
        JsonArray array = new JsonArray();
        for (MobEffectInstance instance : allEffects) {
            array.add(toJson(instance.getEffect(), instance.getAmplifier()));
        }
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
        if(!ModConfig.getInstance().isFoodRegenCapEnabled()) return;
        float regen = CONFIG.getHealthRegen(item);
        if (regen <= 0f) return;

        DietStats stats = getDietStats(player);
        stats.regenBudget = Math.max(stats.regenBudget, regen);
    }

    //Cap food regen for players
    public static boolean canRegen(Player player)
    {
        if (!player.isHurt()) return false;
        if (!ModConfig.getInstance().isFoodRegenCapEnabled()) return true;
        return getDietStats(player).regenBudget > 0f;
    }

    private static final float PASSIVE_EXHAUSTION = 0.01f;
    public static void handleFoodExhaustion(Player player, DietStats stats)
    {
        if (!player.isHurt() || player.getMaxHealth() <= 0f) return;
        if(player.getFoodData().getFoodLevel()<= 0) return;

        float maxHealth = player.getMaxHealth();
        float health = player.getHealth();

        float missingPortion = Math.max(0f, maxHealth - health) / maxHealth;
        float exhaustionMultiplier = CONFIG.getHurtExhaustionMultiplierMax();

        float scaledMultiplier = (exhaustionMultiplier * missingPortion)+1f;
        player.causeFoodExhaustion( PASSIVE_EXHAUSTION * scaledMultiplier );
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

    private static void on20Ticks(ServerTickEvent event)
    {
        Iterator<EatingFood> it = EATING_FOODS.values().iterator();
        while (it.hasNext())
        {
            EatingFood eating = it.next();
            ServerPlayer player = eating.player();
            if (player.isRemoved() || !player.isUsingItem() || player.getUseItem().getItem() != eating.item()) {
                it.remove();
            }
        }

        //for all serverPlayers
        for (ServerPlayer player : HBUtil.PlayerUtil.getAllPlayers()) {
            DietStats stats = getDietStats(player);
            handleFoodExhaustion(player, stats);
        }

    }


    //** Classes
    public static class DietStats {
        float regenBudget;
        List<BonusEffect> previousFoodEffects = new ArrayList<>();
        Player p;
        public DietStats(Player player) {
            this.regenBudget = 0f;
            this.p = player;
        }
    }

}
