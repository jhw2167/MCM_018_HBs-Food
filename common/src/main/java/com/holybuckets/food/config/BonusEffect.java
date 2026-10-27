package com.holybuckets.food.config;


import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

//** FOOD EFFECTS
public class BonusEffect {
    public final MobEffect effect;
    public final int effectLevel;

    public BonusEffect(MobEffect effect, int effectLevel) {
        this.effect = effect;
        this.effectLevel = effectLevel;
    }


    public MobEffectInstance create(int durationTicks) {
        return create(durationTicks, false);
    }

    public MobEffectInstance create(int durationTicks, boolean visible) {
        return new MobEffectInstance(effect, durationTicks, effectLevel, false, visible, visible);
    }

    public BonusEffect withLevelIncrease(int levels) {
        if (levels <= 0) return this;
        return new BonusEffect(effect, effectLevel + levels);
    }
}

