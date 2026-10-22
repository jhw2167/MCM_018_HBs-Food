package com.holybuckets.food.mixin;

import com.holybuckets.food.core.DietManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayer {

    @Inject(method = "eat", at = @At("HEAD"))
    private void hbs_food$onFinishEating(Level level, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide) return;
        DietManager.onPlayerFinishEating((Player) (Object) this, stack);
    }
}
