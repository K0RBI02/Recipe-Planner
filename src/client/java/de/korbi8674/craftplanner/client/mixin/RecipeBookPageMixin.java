package de.korbi8674.craftplanner.client.mixin;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import de.korbi8674.craftplanner.client.planer.PlanerListe;

/** Rechtsklick auf ein Rezept im Rezeptbuch setzt das Item auf die Bauplaner-Liste. */
@Mixin(RecipeBookPage.class)
public abstract class RecipeBookPageMixin {
	@Shadow
	@Final
	private List<RecipeButton> buttons;

	@Shadow
	@Final
	private OverlayRecipeComponent overlay;

	@Shadow
	private Minecraft minecraft;

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void craftplanner$rechtsklick(MouseButtonEvent event, int xo, int yo, int imageWidth, int imageHeight, boolean doubleClick,
			CallbackInfoReturnable<Boolean> cir) {
		if (event.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
			return;
		}

		Item item = null;
		if (overlay.isVisible()) {
			// Varianten-Auswahl ist offen: die angeklickte Variante nehmen
			for (Object knopf : ((OverlayRecipeComponentAccessor) overlay).craftplanner$recipeButtons()) {
				if (((AbstractWidget) knopf).isMouseOver(event.x(), event.y())) {
					item = ergebnis(((OverlayRecipeButtonAccessor) knopf).craftplanner$recipe());
					break;
				}
			}
		} else {
			for (RecipeButton knopf : buttons) {
				// Bei mehreren Varianten öffnet Vanilla mit Rechtsklick die Auswahl, das lassen wir so.
				if (knopf.visible && knopf.isMouseOver(event.x(), event.y()) && knopf.isOnlyOption()) {
					item = knopf.getDisplayStack().getItem();
					break;
				}
			}
		}

		if (item != null) {
			PlanerListe.hinzufuegen(item, event.hasShiftDown() || minecraft.hasShiftDown() ? 10 : 1);
			AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
			cir.setReturnValue(true);
		}
	}

	private Item ergebnis(RecipeDisplayId id) {
		for (RecipeDisplayEntry eintrag : overlay.getRecipeCollection().getRecipes()) {
			if (eintrag.id().equals(id)) {
				ItemStack stack = eintrag.display().result().resolveForFirstStack(SlotDisplayContext.fromLevel(minecraft.level));
				return stack.isEmpty() ? null : stack.getItem();
			}
		}
		return null;
	}
}
