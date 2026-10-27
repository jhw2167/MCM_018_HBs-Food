package com.holybuckets.food.mixin;

import com.holybuckets.food.core.DietManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayer {

    //Add food effects when player finishes eating... duh
    @Inject(method = "eat", at = @At("HEAD"))
    private void hbs_food$onFinishEating(Level level, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide) return;
        DietManager.onPlayerFinishEating((Player) (Object) this, stack);
    }

    /*
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float hbs_food$scaleExhaustion(float exhaustion) {
        return DietManager.scaleExhaustion((Player) (Object) this, exhaustion);
    }
    */

}
