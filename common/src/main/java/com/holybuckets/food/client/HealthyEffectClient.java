package com.holybuckets.food.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.food.LoggerProject;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Client side cache of the hidden effects behind the Healthy effect, sent by DietManager as JSON.
 */
public class HealthyEffectClient {

    public static final String MSG_ID_HEALTHY_EFFECTS = "hbs_food_healthy_effects";
    private static final String CLASS_ID = "004";

    public record GrantedEffect(MobEffect effect, int amplifier) {}

    private static final List<GrantedEffect> GRANTED = new ArrayList<>();

    private HealthyEffectClient() {}

    public static void init(ClientEventRegistrar registrar) {
        registrar.registerOnSimpleMessage(MSG_ID_HEALTHY_EFFECTS, HealthyEffectClient::onHealthyEffectsMessage);
    }

    //Payload is [{"id":"minecraft:strength","amp":0}, ...]
    private static void onHealthyEffectsMessage(SimpleMessageEvent event)
    {
        List<GrantedEffect> parsed = new ArrayList<>();
        try {
            for (JsonElement element : JsonParser.parseString(event.getContent()).getAsJsonArray()) {
                JsonObject obj = element.getAsJsonObject();
                ResourceLocation id = ResourceLocation.tryParse(obj.get("id").getAsString());
                MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null);
                if (effect == null) continue;
                parsed.add(new GrantedEffect(effect, obj.get("amp").getAsInt()));
            }
        } catch (RuntimeException e) {
            LoggerProject.logError(CLASS_ID + "001", "Could not read healthy effects payload: " + event.getContent());
            return;
        }

        GRANTED.clear();
        GRANTED.addAll(parsed);
    }

    public static List<GrantedEffect> getGrantedEffects() {
        return GRANTED;
    }

    /** Tooltip lines describing the effects the Healthy effect is currently granting, skipping any that expired. */
    public static List<Component> getDescription(Player player)
    {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("effect.hbs_food.healthy.desc").withStyle(ChatFormatting.GRAY));
        for (GrantedEffect granted : GRANTED) {
            if (!player.hasEffect(granted.effect())) continue;
            MutableComponent line = Component.literal(" - ").append(Component.translatable(granted.effect().getDescriptionId()));
            if (granted.amplifier() > 0) {
                line.append(" ").append(Component.translatable("enchantment.level." + (granted.amplifier() + 1)));
            }
            lines.add(line.withStyle(ChatFormatting.BLUE));
        }
        return lines;
    }

}
