package com.holybuckets.food.mixin;

import com.holybuckets.food.client.FoodTooltipClient;
import com.holybuckets.food.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true)
    private void hbs_food$modifyEatSpeed(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.isEdible()) return;
        int duration = ModConfig.getInstance().getModifiedUseDuration(stack.getItem(), cir.getReturnValue());
        if (duration != cir.getReturnValue()) cir.setReturnValue(duration);
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void hbs_food$addFoodTags(@Nullable Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        FoodTooltipClient.addFoodTags((ItemStack) (Object) this, cir.getReturnValue());
    }
}
