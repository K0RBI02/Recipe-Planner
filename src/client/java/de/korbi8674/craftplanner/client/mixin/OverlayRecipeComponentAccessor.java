package de.korbi8674.craftplanner.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;

/** Zugriff auf die Knöpfe der Varianten-Auswahl im Rezeptbuch. */
@Mixin(OverlayRecipeComponent.class)
public interface OverlayRecipeComponentAccessor {
	@Accessor("recipeButtons")
	List<?> craftplanner$recipeButtons();
}
