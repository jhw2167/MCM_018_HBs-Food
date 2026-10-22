package com.holybuckets.food.mixin;

import com.holybuckets.food.client.HealthyEffectClient;
import com.holybuckets.food.effect.ModEffects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mixin(EffectRenderingInventoryScreen.class)
public abstract class MixinEffectRenderingInventoryScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    public MixinEffectRenderingInventoryScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** Hides effects granted without an icon from the inventory list, matching the hud. */
    @Redirect(method = "renderEffects", require = 0,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getActiveEffects()Ljava/util/Collection;"))
    private Collection<MobEffectInstance> hbs_food$filterHiddenEffects(LocalPlayer player) {
        return hbs_food$visibleEffects(player);
    }

    @Inject(method = "renderEffects", at = @At("TAIL"))
    private void hbs_food$renderHealthyTooltip(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci)
    {
        if (this.minecraft == null || this.minecraft.player == null) return;
        List<MobEffectInstance> effects = hbs_food$visibleEffects(this.minecraft.player);
        int x = this.leftPos + this.imageWidth + 2;
        int available = this.width - x;
        if (effects.isEmpty() || available < 32) return;

        int rowWidth = (available >= 120) ? 120 : 32;
        if (mouseX < x || mouseX > x + rowWidth) return;

        int step = (effects.size() > 5) ? 132 / (effects.size() - 1) : 33;
        int y = this.topPos;
        for (MobEffectInstance effect : effects)
        {
            if (mouseY >= y && mouseY <= y + step) {
                if (effect.getEffect() == ModEffects.HEALTHY.get()) hbs_food$renderTooltip(graphics, effect, mouseX, mouseY);
                return;
            }
            y += step;
        }
    }

    @Unique
    private void hbs_food$renderTooltip(GuiGraphics graphics, MobEffectInstance healthy, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        MutableComponent name = Component.translatable(healthy.getDescriptionId());
        if (healthy.getAmplifier() > 0) {
            name.append(" ").append(Component.translatable("enchantment.level." + (healthy.getAmplifier() + 1)));
        }
        lines.add(name.append(" (").append(MobEffectUtil.formatDuration(healthy, 1.0f)).append(")"));
        lines.addAll(HealthyEffectClient.getDescription(this.minecraft.player));
        graphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
    }

    @Unique
    private static List<MobEffectInstance> hbs_food$visibleEffects(LocalPlayer player) {
        return player.getActiveEffects().stream().filter(MobEffectInstance::showIcon).sorted().toList();
    }
}
