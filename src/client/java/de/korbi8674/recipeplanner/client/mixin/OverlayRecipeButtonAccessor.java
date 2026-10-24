package de.korbi8674.recipeplanner.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.item.crafting.display.RecipeDisplayId;

/** Zugriff auf das Rezept eines Knopfs in der Varianten-Auswahl (private Klasse in Minecraft). */
@Mixin(targets = "net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent$OverlayRecipeButton")
public interface OverlayRecipeButtonAccessor {
	@Accessor("recipe")
	RecipeDisplayId recipeplanner$recipe();
}
