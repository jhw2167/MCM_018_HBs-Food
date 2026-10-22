package com.holybuckets.food.effect;

import com.holybuckets.food.Constants;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.DeferredObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ModEffects {

    public static DeferredObject<MobEffect> HEALTHY;

    public static void register() {
        HEALTHY = Balm.getRegistries().register(
            BuiltInRegistries.MOB_EFFECT,
            id -> new HealthyEffect(),
            id("healthy")
        );
    }

    /** Display only effect standing in for the hidden food bonuses a player was granted. */
    public static class HealthyEffect extends MobEffect {
        public static final int COLOR = 0x7CCB5B;
        protected HealthyEffect() {
            super(MobEffectCategory.BENEFICIAL, COLOR);
        }
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }
}
