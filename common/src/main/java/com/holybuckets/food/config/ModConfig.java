package com.holybuckets.food.config;

import com.holybuckets.food.LoggerProject;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
    public static final int DEF_EFFECT_AMPLIFIER = 0;
    public static final int TICKS_PER_SECOND = 20;
    public static final float DEF_QUICK_FOOD_SPEED = 2.0f;
    public static final float DEF_SLOW_FOOD_SPEED = 0.5f;
    public static final float DEF_LINGERING_FOOD_MULTIPLIER = 2.0f;
    public static final int DEF_ROBUST_FOOD_LEVEL_INCREASE = 1;

    private FoodBenefitsConfig config;

    private final List<Set<Item>> foodGroups = new ArrayList<>();
    private final List<List<BonusEffect>> foodGroupEffects = new ArrayList<>();
    private final List<String> foodGroupNames = new ArrayList<>();
    private final Set<Item> quickFoods = new HashSet<>();
    private final Set<Item> slowFoods = new HashSet<>();
    private final Set<Item> lingeringFoods = new HashSet<>();
    private final Set<Item> robustFoods = new HashSet<>();
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

    /** Groups are numbered 1 to FOOD_GROUP_COUNT to match the config names. */
    public boolean isFoodInGroup(Item item, int group) {
        if (item == null || !isValidGroup(group)) return false;
        return foodGroups.get(group - 1).contains(item);
    }

    public List<Integer> getFoodGroups(Item item) {
        List<Integer> groups = new ArrayList<>();
        if (item == null) return groups;
        for (int i = 0; i < foodGroups.size(); i++) {
            if (foodGroups.get(i).contains(item)) groups.add(i + 1);
        }
        return groups;
    }

    public boolean hasFoodGroup(Item item) {
        if (item == null) return false;
        for (Set<Item> group : foodGroups) {
            if (group.contains(item)) return true;
        }
        return false;
    }

    public Set<Item> getFoodGroupItems(int group) {
        if (!isValidGroup(group)) return Collections.emptySet();
        return Collections.unmodifiableSet(foodGroups.get(group - 1));
    }

    public String getFoodGroupName(int group) {
        if (!isValidGroup(group)) return "";
        return foodGroupNames.get(group - 1);
    }

    private static boolean isValidGroup(int group) {
        return group >= 1 && group <= FOOD_GROUP_COUNT;
    }


    //** FOOD EFFECTS

    /** An effect and amplifier from config; duration is decided by the food eaten. */
    public record BonusEffect(MobEffect effect, int amplifier) {

        /** Hidden from particles and icons; the Healthy effect displays these instead. */
        public MobEffectInstance create(int durationTicks) {
            return new MobEffectInstance(effect, durationTicks, amplifier, false, false, false);
        }

        public BonusEffect withLevelIncrease(int levels) {
            if (levels <= 0) return this;
            return new BonusEffect(effect, amplifier + levels);
        }
    }

    public List<BonusEffect> getFoodGroupEffects(int group) {
        if (!isValidGroup(group)) return Collections.emptyList();
        return Collections.unmodifiableList(foodGroupEffects.get(group - 1));
    }

    /** Merges effects across every group the food belongs to, keeping the highest amplifier per effect. */
    public Collection<BonusEffect> getFoodEffects(Item item) {
        Map<MobEffect, BonusEffect> merged = new LinkedHashMap<>();
        for (int group : getFoodGroups(item)) {
            for (BonusEffect bonus : foodGroupEffects.get(group - 1)) {
                merged.merge(bonus.effect(), bonus, (a, b) -> a.amplifier() >= b.amplifier() ? a : b);
            }
        }
        return merged.values();
    }

    public boolean isEligibleForBonusEffect(int hungerPoints) {
        return hungerPoints >= minFoodPointForBonusEffect;
    }

    public int getBonusEffectDurationTicks(int hungerPoints) {
        if (!isEligibleForBonusEffect(hungerPoints)) return 0;
        return hungerPoints * bonusEffectDurationPerFoodPoint * TICKS_PER_SECOND;
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

    /** Vanilla FoodData adds nutrition * saturationModifier * 2 saturation. */
    public static float getSaturationPoints(FoodProperties food) {
        if (food == null) return 0f;
        return food.getNutrition() * food.getSaturationModifier() * 2.0f;
    }

    public boolean isLingeringFood(Item item) {
        return item != null && lingeringFoods.contains(item);
    }

    public boolean isRobustFood(Item item) {
        return item != null && robustFoods.contains(item);
    }

    // Applies lingering bonus to some foods
    public int getLingeringDuration(Item item, int durationTicks) {
        if (!isLingeringFood(item)) return durationTicks;
        return Math.round(durationTicks * lingeringFoodBonusMultiplier);
    }

    // Extra effect levels a food grants, 0 unless it is a robust food.
    public int getRobustLevelIncrease(Item item) {
        return isRobustFood(item) ? robustFoodEffectLevelIncrease : 0;
    }

    public boolean isQuickFood(Item item) {
        return item != null && quickFoods.contains(item);
    }

    public boolean isSlowFood(Item item) {
        return item != null && slowFoods.contains(item);
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

    public float getHealthRegen(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0f;
        return getHealthRegen(stack.getItem());
    }

    public FoodBenefitsConfig getConfig() {
        return config;
    }


    //** LOADING

    private void onBeforeServerStarted(RegistryAccess registryAccess)
    {
        this.config = Balm.getConfig().getActiveConfig(FoodBenefitsConfig.class);
        Registry<Item> itemRegistry = registryAccess.registryOrThrow(Registries.ITEM);
        Registry<MobEffect> effectRegistry = registryAccess.registryOrThrow(Registries.MOB_EFFECT);

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
        for (int i = 0; i < FOOD_GROUP_COUNT; i++)
        {
            foodGroups.add(parseFoods(itemRegistry, groupIds.get(i), "foodGroup" + (i + 1)));
            foodGroupEffects.add(parseEffects(effectRegistry, effectIds.get(i), "foodGroup" + (i + 1) + "Effects"));
            foodGroupNames.add(parseGroupName(groupsConfig.foodGroupNames, i));
        }

        String healthyBonusId = effectsConfig.minHealthyBonusEffect;
        List<BonusEffect> healthyBonus = (healthyBonusId == null || healthyBonusId.isBlank()) ? List.of()
            : parseEffects(effectRegistry, List.of(healthyBonusId), "minHealthyBonusEffect");
        healthyBonusEffect = healthyBonus.isEmpty() ? null : healthyBonus.get(0);
        minHealthyEffectLevelForBonusEffect = Math.max(1, effectsConfig.minHealthyEffectLevelForBonusEffect);

        minFoodPointForBonusEffect = Math.max(0, effectsConfig.minFoodPointForBonusEffect);
        bonusEffectDurationPerFoodPoint = Math.max(0, effectsConfig.bonusEffectDurationPerFoodPoint);
        bonusEffectDurationPerSaturationPoint = Math.max(0, config.foodSaturationInteraction.bonusEffectDurationPerSaturationPoint);

        quickFoods.clear();
        quickFoods.addAll(parseFoods(itemRegistry, effectsConfig.quickFoods, "quickFoods"));
        slowFoods.clear();
        slowFoods.addAll(parseFoods(itemRegistry, effectsConfig.slowFoods, "slowFoods"));

        quickFoodSpeedBonus = effectsConfig.quickFoodSpeedBonus;
        if (quickFoodSpeedBonus <= 1.0f) {
            LoggerProject.logWarning(CLASS_ID + "003", "quickFoodSpeedBonus must be greater than 1, found "
                + quickFoodSpeedBonus + "; applying default " + DEF_QUICK_FOOD_SPEED);
            quickFoodSpeedBonus = DEF_QUICK_FOOD_SPEED;
        }

        lingeringFoods.clear();
        lingeringFoods.addAll(parseFoods(itemRegistry, effectsConfig.lingeringFoods, "lingeringFoods"));
        robustFoods.clear();
        robustFoods.addAll(parseFoods(itemRegistry, effectsConfig.robustFoods, "robustFoods"));

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
        minHungerPointsEligibleForHeal = Math.max(0, config.foodHealthInteractions.minHungerPointsEligibleForHeal);
        healthRegenPerHunger = Math.max(0f, config.foodHealthInteractions.healthRegenPerHunger);

        LoggerProject.logInfo(CLASS_ID + "001", "Loaded food groups with sizes " + foodGroups.stream().map(Set::size).toList()
            + ", " + quickFoods.size() + " quick food(s), " + slowFoods.size() + " slow food(s), "
            + lingeringFoods.size() + " lingering food(s), " + robustFoods.size() + " robust food(s)");
    }

    private static Set<Item> parseFoods(Registry<Item> registry, Collection<String> ids, String configName)
    {
        Set<Item> foods = new HashSet<>();
        for (String id : ids)
        {
            Item item = getItem(registry, id);
            if (item == null) {
                LoggerProject.logWarning(CLASS_ID + "005", configName + ": could not find item for id '" + id + "'");
                continue;
            }
            if (item.getFoodProperties() == null) {
                LoggerProject.logWarning(CLASS_ID + "006", configName + ": item '" + id + "' is not a food, skipping");
                continue;
            }
            foods.add(item);
        }
        return foods;
    }

    /** Parses entries of the form effect_id[,amplifier]. */
    private static List<BonusEffect> parseEffects(Registry<MobEffect> registry, Collection<String> entries, String configName)
    {
        List<BonusEffect> effects = new ArrayList<>();
        for (String entry : entries)
        {
            String[] parts = entry.split(",");
            ResourceLocation loc = ResourceLocation.tryParse(parts[0].trim());
            MobEffect effect = (loc == null) ? null : registry.getOptional(loc).orElse(null);
            if (effect == null) {
                LoggerProject.logWarning(CLASS_ID + "007", configName + ": could not find effect for '" + entry + "'");
                continue;
            }

            int amplifier = DEF_EFFECT_AMPLIFIER;
            try {
                if (parts.length > 1) amplifier = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                LoggerProject.logWarning(CLASS_ID + "008", configName + ": invalid amplifier in '"
                    + entry + "'; applying default");
            }

            effects.add(new BonusEffect(effect, Math.max(0, amplifier)));
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


    private static void onBeforeServerStarted(ServerStartingEvent event) {
        getInstance().onBeforeServerStarted(event.getServer().registryAccess());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        getInstance().onServerStopped();
    }

}
