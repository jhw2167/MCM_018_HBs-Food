package com.holybuckets.food;

import com.holybuckets.food.client.CommonClassClient;
import net.blay09.mods.balm.api.client.BalmClient;
import net.fabricmc.api.ClientModInitializer;


public class FoodWithBenefitsMainFabricClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        BalmClient.initialize(Constants.MOD_ID, CommonClassClient::initClient);
    }

}
