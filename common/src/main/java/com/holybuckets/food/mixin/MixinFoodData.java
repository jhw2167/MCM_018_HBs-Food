package com.holybuckets.food.mixin;

import com.holybuckets.food.core.DietManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
public abstract class MixinFoodData {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isHurt()Z"))
    private boolean hbs_food$canRegen(Player player) {
        return DietManager.canRegen(player);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void hbs_food$capHeal(Player player, float amount) {
        DietManager.capHeal(player, amount);
    }
}
