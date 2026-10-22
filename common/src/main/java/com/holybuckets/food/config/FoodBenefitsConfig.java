package com.holybuckets.food.config;

import com.holybuckets.food.Constants;
import net.blay09.mods.balm.api.config.reflection.Comment;
import net.blay09.mods.balm.api.config.reflection.Config;
import net.blay09.mods.balm.api.config.reflection.NestedType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Config(Constants.MOD_ID)
public class FoodBenefitsConfig {

    public static class FoodGroups {

        @NestedType(String.class)
        @Comment("Food items belonging to food group 1, by default Meat")
        public Set<String> foodGroup1 = new HashSet<>(Set.of(
            "minecraft:cooked_beef",
            "minecraft:cooked_porkchop",
            "minecraft:cooked_chicken",
            "minecraft:cooked_mutton",
            "minecraft:cooked_rabbit",
            "minecraft:cooked_cod",
            "minecraft:cooked_salmon",
            "minecraft:rabbit_stew"
        ));

        @NestedType(String.class)
        @Comment("Food items belonging to food group 2, by default Fruits/Veggies")
        public Set<String> foodGroup2 = new HashSet<>(Set.of(
            "minecraft:apple",
            "minecraft:carrot",
            "minecraft:golden_carrot",
            "minecraft:baked_potato",
            "minecraft:beetroot",
            "minecraft:beetroot_soup",
            "minecraft:melon_slice",
            "minecraft:sweet_berries",
            "minecraft:glow_berries",
            "minecraft:dried_kelp",
            "minecraft:mushroom_stew"
        ));

        @NestedType(String.class)
        @Comment("Food items belonging to food group 3, by default Grains")
        public Set<String> foodGroup3 = new HashSet<>(Set.of(
            "minecraft:bread",
            "minecraft:cookie",
            "minecraft:pumpkin_pie"
        ));

        @NestedType(String.class)
        @Comment("Food items belonging to food group 4, empty by default for player use")
        public Set<String> foodGroup4 = new HashSet<>();

        @NestedType(String.class)
        @Comment("Food items belonging to food group 5, empty by default for player use")
        public Set<String> foodGroup5 = new HashSet<>();

        @NestedType(String.class)
        @Comment("Food items belonging to food group 6, empty by default for player use")
        public Set<String> foodGroup6 = new HashSet<>();

        @NestedType(String.class)
        @Comment("Player friendly names for each food group, in order from group 1 to group 6")
        public List<String> foodGroupNames = new ArrayList<>(List.of("Meat", "Fruits/Veggies", "Grains"));
    }

    public static class FoodEffects {

        @NestedType(String.class)
        @Comment("Potion effects applied when eating a food from group 1. Format: effect_id[,amplifier] e.g. minecraft:strength,0")
        public Set<String> foodGroup1Effects = new HashSet<>(Set.of("minecraft:strength"));

        @NestedType(String.class)
        @Comment("Potion effects applied when eating a food from group 2. Format: effect_id[,amplifier]")
        public Set<String> foodGroup2Effects = new HashSet<>(Set.of("minecraft:haste"));

        @NestedType(String.class)
        @Comment("Potion effects applied when eating a food from group 3. Format: effect_id[,amplifier]")
        public Set<String> foodGroup3Effects = new HashSet<>(Set.of("minecraft:speed"));

        @NestedType(String.class)
        @Comment("Potion effects applied when eating a food from group 4. Format: effect_id[,amplifier]")
        public Set<String> foodGroup4Effects = new HashSet<>();

        @NestedType(String.class)
        @Comment("Potion effects applied when eating a food from group 5. Format: effect_id[,amplifier]")
        public Set<String> foodGroup5Effects = new HashSet<>();

        @NestedType(String.class)
        @Comment("Potion effects applied when eating a food from group 6. Format: effect_id[,amplifier]")
        public Set<String> foodGroup6Effects = new HashSet<>();

        @Comment("Minimum hunger points a food must replenish to apply its food group bonus effects")
        public int minFoodPointForBonusEffect = 1;

        @Comment("Seconds of bonus effect duration granted for each hunger point the food replenishes")
        public int bonusEffectDurationPerFoodPoint = 5;

        @NestedType(String.class)
        @Comment("Food items which are eaten faster than normal")
        public Set<String> quickFoods = new HashSet<>(Set.of(
            "minecraft:cookie",
            "minecraft:sweet_berries",
            "minecraft:glow_berries",
            "minecraft:melon_slice",
            "minecraft:apple",
            "minecraft:carrot"
        ));

        @Comment("Eating speed multiplier for quick foods, must be greater than 1. A value of 2 eats twice as fast")
        public float quickFoodSpeedBonus = 2.0f;

        @NestedType(String.class)
        @Comment("Food items which are eaten slower than normal")
        public Set<String> slowFoods = new HashSet<>(Set.of(
            "minecraft:rabbit_stew",
            "minecraft:mushroom_stew",
            "minecraft:beetroot_soup",
            "minecraft:suspicious_stew",
            "minecraft:cooked_beef",
            "minecraft:cooked_porkchop"
        ));

        @Comment("Eating speed multiplier for slow foods, must be between 0 and 1. A value of 0.5 eats half as fast")
        public float slowFoodSpeedBonus = 0.5f;
    }

    public static class FoodHealthInteractions {

        @Comment("Minimum hunger points a food must replenish to give the player passive health regen")
        public int minHungerPointsEligibleForHeal = 4;

        @Comment("Health points (half hearts) rewarded for each point of hunger at or above the minimum threshold")
        public float healthRegenPerHunger = 1.0f;
    }

    public static class FoodSaturationInteraction {

        @NestedType(String.class)
        @Comment("Potion effects applied based on the saturation a food provides. Format: effect_id[,amplifier]")
        public Set<String> playerSaturatedBonusEffect = new HashSet<>(Set.of("minecraft:resistance"));

        @Comment("Seconds of saturation bonus effect duration granted for each saturation point the food provides")
        public int bonusEffectDurationPerSaturationPoint = 5;
    }

    public FoodGroups foodGroups = new FoodGroups();
    public FoodEffects foodEffects = new FoodEffects();
    public FoodHealthInteractions foodHealthInteractions = new FoodHealthInteractions();
    public FoodSaturationInteraction foodSaturationInteraction = new FoodSaturationInteraction();
}
