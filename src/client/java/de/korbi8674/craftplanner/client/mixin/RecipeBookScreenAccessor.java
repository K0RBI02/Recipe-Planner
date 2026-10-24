package de.korbi8674.craftplanner.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

/** Zugriff aufs Rezeptbuch, um zu wissen, ob es links neben dem Fenster offen ist. */
@Mixin(AbstractRecipeBookScreen.class)
public interface RecipeBookScreenAccessor {
	@Accessor("recipeBookComponent")
	RecipeBookComponent<?> craftplanner$recipeBook();
}
