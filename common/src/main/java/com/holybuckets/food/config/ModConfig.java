package com.holybuckets.food.config;

import com.holybuckets.food.LoggerProject;
import com.holybuckets.food.enchantment.PotencyEnchantment;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Class: ModConfig
 * Description: Singleton holding runtime food groups, effects and eating speeds resolved from FoodBenefitsConfig.
 */
public class ModConfig {

    private static final String CLASS_ID = "002";
    private static ModConfig INSTANCE;

    public static final int FOOD_GROUP_COUNT = 6;
    public static final int DEF_EFFECT_LEVEL = 1;
    public static final int TICKS_PER_SECOND = 20;
    public static final float DEF_QUICK_FOOD_SPEED = 2.0f;
    public static final float DEF_SLOW_FOOD_SPEED = 0.5f;
    public static final float DEF_LINGERING_FOOD_MULTIPLIER = 2.0f;
    public static final int DEF_ROBUST_FOOD_LEVEL_INCREASE = 1;
    public static final float DEF_HURT_EXHAUSTION_MULTIPLIER_MAX = 2.0f;

    private FoodBenefitsConfig config;

    private final Map<String, Set<Item>> foodGroups = new HashMap<>();
    private final Map<String, List<BonusEffect>> foodGroupEffects = new HashMap<>();
    private final List<String> foodGroupNames = new ArrayList<>();
    private final Set<Item> quickFoods = new HashSet<>();
    private final Set<Item> slowFoods = new HashSet<>();
    private final Set<Item> lingeringFoods = new HashSet<>();
    private final Set<Item> robustFoods = new HashSet<>();
    private int hungerPointsPerHealthPoint = 4;
    private int maxHealingPotency = PotencyEnchantment.MAX_LEVEL;

    private float lingeringFoodBonusMultiplier = DEF_LINGERING_FOOD_MULTIPLIER;
    private int robustFoodEffectLevelIncrease = DEF_ROBUST_FOOD_LEVEL_INCREASE;
    private float quickFoodSpeedBonus = DEF_QUICK_FOOD_SPEED;
    private float slowFoodSpeedBonus = DEF_SLOW_FOOD_SPEED;

    private int minFoodPointForBonusEffect;
    private int bonusEffectDurationPerFoodPoint;
    private int bonusEffectDurationPerSaturationPoint;
    private int minHealthyEffectLevelForBonusEffect;
    @Nullable private BonusEffect healthyBonusEffect;
    private boolean enableFoodRegenCap;
    private float hurtExhaustionMultiplierMax = DEF_HURT_EXHAUSTION_MULTIPLIER_MAX;
    private int minHungerPointsEligibleForHeal;
    private float healthRegenPerHunger;

    public static ModConfig getInstance() {
        if (INSTANCE == null) INSTANCE = new ModConfig();
        return INSTANCE;
    }

    private ModConfig() {}

    public static void init(EventRegistrar registrar) {
        INSTANCE = ModConfig.getInstance();
        registrar.registerOnBeforeServerStarted(ModConfig::onBeforeServerStarted, EventPriority.High);
        registrar.registerOnServerStopped(ModConfig::onServerStopped, EventPriority.Low);
    }


    //** FOOD GROUPS


    public List<String> getFoodGroups(Item item) {
        List<String> groups = new ArrayList<>();
        if (item == null) return groups;
        for (String group : foodGroups.keySet()) {
            if (foodGroups.get(group).contains(item)) groups.add(group);
        }
        return groups;
    }


    public List<BonusEffect> getFoodGroupEffects(String group) {
        if (!foodGroupEffects.containsKey(group)) return Collections.emptyList();
        return Collections.unmodifiableList(foodGroupEffects.get(group));
    }

    public List<BonusEffect> getBonusEffects(Item item) {
        var listBonus = new ArrayList<BonusEffect>();
        for (String group : getFoodGroups(item)) {
            listBonus.addAll(foodGroupEffects.get(group));
        }
        return listBonus;
    }

    public boolean isEligibleForBonusEffect(Item foodItem) {
        if(foodItem.getFoodProperties()==null) return false;
        return isEligibleForBonusEffect(foodItem.getFoodProperties().getNutrition());
    }

    public boolean isEligibleForBonusEffect(int hungerPoints) {
        return hungerPoints >= minFoodPointForBonusEffect;
    }

    public int getMinHealthyEffectLevelForBonusEffect() {
        return minHealthyEffectLevelForBonusEffect;
    }

    @Nullable
    public BonusEffect getHealthyBonusEffect() {
        return healthyBonusEffect;
    }

    public int getSaturationEffectDurationTicks(float saturationPoints) {
        if (saturationPoints <= 0f) return 0;
        return Math.round(saturationPoints * bonusEffectDurationPerSaturationPoint * TICKS_PER_SECOND);
    }

    // Vanilla Food calc
    public static float calcSatPoints(FoodProperties food) {
        if (food == null) return 0f;
        return food.getNutrition() * food.getSaturationModifier() * 2.0f;
    }

    public int getHungerPointsPerHealthPoint() {
        return hungerPointsPerHealthPoint;
    }

    public int getMaxHealingPotency() {
        return maxHealingPotency;
    }

    public boolean isLingeringFood(Item item) {
        return item != null && lingeringFoods.contains(item);
    }

    public boolean isRobustFood(Item item) {
        return item != null && robustFoods.contains(item);
    }

    // Applies lingering bonus to some foods
    public int getDuration(Item item) {
        int foodPoints = item.getFoodProperties().getNutrition();
        int duration = foodPoints * bonusEffectDurationPerFoodPoint;
        if (isLingeringFood(item)) duration *= lingeringFoodBonusMultiplier;
        return duration;
    }

    // Extra effect levels a food grants, 0 unless it is a robust food.
    public int getRobustLevelIncrease(Item item) {
        return isRobustFood(item) ? robustFoodEffectLevelIncrease : 0;
    }

    public boolean isQuickFood(Item item) {
        return item!=null && quickFoods.contains(item);
    }

    public boolean isSlowFood(Item item) {
        return item!=null && slowFoods.contains(item);
    }

    // Multiplier applied to eating speed, 1 when the food has no speed modifier.
    public float getEatSpeedModifier(Item item) {
        if (isQuickFood(item)) return quickFoodSpeedBonus;
        if (isSlowFood(item)) return slowFoodSpeedBonus;
        return 1.0f;
    }

    public int getModifiedUseDuration(Item item, int baseDuration) {
        float modifier = getEatSpeedModifier(item);
        if (modifier == 1.0f) return baseDuration;
        return Math.max(1, Math.round(baseDuration / modifier));
    }


    //** FOOD HEALTH INTERACTIONS

    public boolean isFoodRegenCapEnabled() {
        return enableFoodRegenCap;
    }

    public float getHurtExhaustionMultiplierMax() {
        return hurtExhaustionMultiplierMax;
    }

    public boolean isEligibleForHeal(int hungerPoints) {
        return hungerPoints >= minHungerPointsEligibleForHeal;
    }

    // Health rewarded for each hunger point at or above the threshold, so a food exactly at the threshold heals once
    public float getHealthRegen(int hungerPoints) {
        if (!isEligibleForHeal(hungerPoints)) return 0f;
        return (hungerPoints - minHungerPointsEligibleForHeal + 1) * healthRegenPerHunger;
    }

    public float getHealthRegen(Item item) {
        FoodProperties food = (item == null) ? null : item.getFoodProperties();
        if (food == null) return 0f;
        return getHealthRegen(food.getNutrition());
    }

    //** LOADING

    private void onBeforeServerStarted()
    {
        this.config = Balm.getConfig().getActiveConfig(FoodBenefitsConfig.class);

        FoodBenefitsConfig.FoodGroups groupsConfig = config.foodGroups;
        FoodBenefitsConfig.FoodEffects effectsConfig = config.foodEffects;

        List<Set<String>> groupIds = List.of(
            groupsConfig.foodGroup1, groupsConfig.foodGroup2, groupsConfig.foodGroup3,
            groupsConfig.foodGroup4, groupsConfig.foodGroup5, groupsConfig.foodGroup6);

        List<Set<String>> effectIds = List.of(
            effectsConfig.foodGroup1Effects, effectsConfig.foodGroup2Effects, effectsConfig.foodGroup3Effects,
            effectsConfig.foodGroup4Effects, effectsConfig.foodGroup5Effects, effectsConfig.foodGroup6Effects);

        foodGroups.clear();
        foodGroupEffects.clear();
        foodGroupNames.clear();
        int size = config.foodGroups.foodGroupNames.size();
        for (int i = 0; i < size; i++)
        {
            String name = config.foodGroups.foodGroupNames.get(i);

            foodGroups.put(name, parseFoods(groupIds.get(i), name));
            foodGroupEffects.put(name, parseEffects(effectIds.get(i)));
            foodGroupNames.add(parseGroupName(groupsConfig.foodGroupNames, i));
        }

        Set<String> healthyBonusId = effectsConfig.healthyBonusPotionEffects;
        List<BonusEffect> healthyBonus = parseEffects(List.copyOf(healthyBonusId));
        healthyBonusEffect = healthyBonus.isEmpty() ? null : healthyBonus.get(0);
        minHealthyEffectLevelForBonusEffect = Math.max(1, effectsConfig.minHealthyEffectLevelForBonusEffect);

        minFoodPointForBonusEffect = Math.max(0, effectsConfig.minFoodPointForBonusEffect);
        bonusEffectDurationPerFoodPoint = Math.max(0, effectsConfig.bonusEffectDurationPerFoodPoint);
        bonusEffectDurationPerSaturationPoint = Math.max(0, config.foodSaturationInteraction.bonusEffectDurationPerSaturationPoint);

        quickFoods.clear();
        quickFoods.addAll(parseFoods(effectsConfig.quickFoods, "quickFoods"));
        slowFoods.clear();
        slowFoods.addAll(parseFoods(effectsConfig.slowFoods, "slowFoods"));

        quickFoodSpeedBonus = effectsConfig.quickFoodSpeedBonus;
        if (quickFoodSpeedBonus <= 1.0f) {
            quickFoodSpeedBonus = DEF_QUICK_FOOD_SPEED;
        }

        FoodBenefitsConfig.HealingPowderConfig powderConfig = config.healingPowder;
        hungerPointsPerHealthPoint = Math.max(1, powderConfig.hungerPointsPerHealthPoint);
        maxHealingPotency = Math.max(1, Math.min(powderConfig.maxHealingPotency, PotencyEnchantment.MAX_LEVEL));

        lingeringFoods.clear();
        lingeringFoods.addAll(parseFoods(effectsConfig.lingeringFoods, "lingeringFoods"));
        robustFoods.clear();
        robustFoods.addAll(parseFoods(effectsConfig.robustFoods, "robustFoods"));

        lingeringFoodBonusMultiplier = effectsConfig.lingeringFoodBonusMultiplier;
        if (lingeringFoodBonusMultiplier <= 1.0f) {
            LoggerProject.logWarning(CLASS_ID + "009", "lingeringFoodBonusMultiplier must be greater than 1, found "
                + lingeringFoodBonusMultiplier + "; applying default " + DEF_LINGERING_FOOD_MULTIPLIER);
            lingeringFoodBonusMultiplier = DEF_LINGERING_FOOD_MULTIPLIER;
        }

        robustFoodEffectLevelIncrease = effectsConfig.robustFoodEffectLevelIncrease;
        if (robustFoodEffectLevelIncrease < 1) {
            LoggerProject.logWarning(CLASS_ID + "010", "robustFoodEffectLevelIncrease must be at least 1, found "
                + robustFoodEffectLevelIncrease + "; applying default " + DEF_ROBUST_FOOD_LEVEL_INCREASE);
            robustFoodEffectLevelIncrease = DEF_ROBUST_FOOD_LEVEL_INCREASE;
        }

        slowFoodSpeedBonus = effectsConfig.slowFoodSpeedBonus;
        if (slowFoodSpeedBonus <= 0f || slowFoodSpeedBonus >= 1.0f) {
            LoggerProject.logWarning(CLASS_ID + "004", "slowFoodSpeedBonus must be between 0 and 1, found "
                + slowFoodSpeedBonus + "; applying default " + DEF_SLOW_FOOD_SPEED);
            slowFoodSpeedBonus = DEF_SLOW_FOOD_SPEED;
        }

        enableFoodRegenCap = config.foodHealthInteractions.enableFoodRegenCap;
        hurtExhaustionMultiplierMax = config.foodHealthInteractions.hurtExhaustionMultiplierMax;
        if (hurtExhaustionMultiplierMax < 1.0f) {
            LoggerProject.logWarning(CLASS_ID + "011", "hurtExhaustionMultiplierMax must be at least 1, found "
                + hurtExhaustionMultiplierMax + "; applying default " + DEF_HURT_EXHAUSTION_MULTIPLIER_MAX);
            hurtExhaustionMultiplierMax = DEF_HURT_EXHAUSTION_MULTIPLIER_MAX;
        }
        minHungerPointsEligibleForHeal = Math.max(0, config.foodHealthInteractions.minHungerPointsEligibleForHeal);
        healthRegenPerHunger = Math.max(0f, config.foodHealthInteractions.healthRegenPerHunger);

        LoggerProject.logInfo(CLASS_ID + "001", "Loaded food groups with sizes " + foodGroups.size()
            + ", " + quickFoods.size() + " quick food(s), " + slowFoods.size() + " slow food(s), "
            + lingeringFoods.size() + " lingering food(s), " + robustFoods.size() + " robust food(s)");
    }

    private static Set<Item> parseFoods(Collection<String> ids, String configName)
    {
        Set<Item> foods = new HashSet<>();
        for (String id : ids) {
            Item item = HBUtil.ItemUtil.itemNameToItem(id);
            if (item==null || item.getFoodProperties()==null) {
                LoggerProject.logWarning(CLASS_ID + "006", configName + ": item '" + id + "' is not a food, skipping");
                continue;
            }
            foods.add(item);
        }
        return foods;
    }

    private static List<BonusEffect> parseEffects(Collection<String> entries)
    {
        List<BonusEffect> effects = new ArrayList<>();
        for (String entry : entries)
        {
            String[] parts = entry.split(",");
            ResourceLocation loc = ResourceLocation.tryParse(parts[0].trim());
            MobEffect effect = HBUtil.EffectUtil.mobEffectNameToEffect(loc);
            if (effect == null) continue;

            int effectLevel = DEF_EFFECT_LEVEL;
            try {
                if (parts.length > 1) effectLevel = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                LoggerProject.logWarning(CLASS_ID + "008", "invalid effect level in '"
                    + entry + "'; applying default");
            }

            effects.add(new BonusEffect(effect, Math.max(DEF_EFFECT_LEVEL, effectLevel)));
        }
        return effects;
    }

    private static String parseGroupName(List<String> names, int index) {
        if (names == null || index >= names.size()) return "Food Group " + (index + 1);
        String name = names.get(index);
        if (name == null || name.isBlank()) return "Food Group " + (index + 1);
        return name.trim();
    }

    @Nullable
    private static Item getItem(Registry<Item> registry, String id) {
        if (id == null || id.isBlank()) return null;
        ResourceLocation loc = ResourceLocation.tryParse(id.trim());
        if (loc == null) return null;
        return registry.getOptional(loc).orElse(null);
    }

    private void onServerStopped() {
        INSTANCE = null;
    }


    public static void onConnectedToServer() {
        if (GeneralConfig.getInstance().isIntegrated()) return;
        getInstance().onBeforeServerStarted();
    }

    private static void onBeforeServerStarted(ServerStartingEvent event) {
        getInstance().onBeforeServerStarted();
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        getInstance().onServerStopped();
    }

}
