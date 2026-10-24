package de.korbi8674.recipeplanner.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
	@Accessor("recipeBookPage")
	RecipeBookPage recipeplannertest$page();

	@Accessor("searchBox")
	net.minecraft.client.gui.components.EditBox recipeplannertest$searchBox();
}
