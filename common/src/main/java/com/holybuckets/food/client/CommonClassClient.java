package com.holybuckets.food.client;

import com.holybuckets.food.client.screen.ModScreens;
import com.holybuckets.food.config.ModConfig;
import com.holybuckets.food.item.ModItems;
import com.holybuckets.foundation.client.ClientBalmEventRegister;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.client.ConnectedToServerEvent;


public class CommonClassClient {

    public static void initClient() {
        ClientEventRegistrar registrar = ClientEventRegistrar.getInstance();
        registrar.registerOnConnectedToServer(CommonClassClient::onConnectedToServer, EventPriority.Highest);
        ClientBalmEventRegister.registerEvents();
        HealthyEffectClient.init(registrar);
        ModRenderers.clientInitialize(BalmClient.getRenderers());
        ModScreens.clientInitialize(BalmClient.getScreens());
        //ModItems.clientInitialize();
    }

    //** Events
    private static void onConnectedToServer(ConnectedToServerEvent event) {
        ModConfig.onConnectedToServer();
    }

    /**
     * Description: Run sample tests methods
     */
    public static void sample()
    {

    }


}