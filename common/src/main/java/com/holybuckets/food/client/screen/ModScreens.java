package com.holybuckets.food.client.screen;

import com.holybuckets.food.menu.ModMenus;
import net.blay09.mods.balm.api.client.screen.BalmScreens;

public class ModScreens {
    public static void clientInitialize(BalmScreens screens) {
        screens.registerScreen(
            ModMenus.countingChestMenu::get,
            CountingChestScreen::new
        );
    }

}
