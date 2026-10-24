package de.korbi8674.recipeplanner.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;

import de.korbi8674.recipeplanner.client.planer.PlanerPanel;

public class RecipePlannerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Bei jedem Fenster, das das eigene Inventar zeigt (Inventar, Kisten, Werkbank …), den Bauplaner anzeigen.
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (screen instanceof AbstractContainerScreen<?> containerScreen
					&& !(screen instanceof CreativeModeInventoryScreen)
					&& containerScreen.getMenu().slots.stream().anyMatch(slot -> slot.container instanceof Inventory)) {
				PlanerPanel.anbringen(containerScreen, minecraft);
			}
		});
	}
}
