package de.korbi8674.craftplanner.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
	@Accessor("recipeBookPage")
	RecipeBookPage craftplannertest$page();

	@Accessor("searchBox")
	net.minecraft.client.gui.components.EditBox craftplannertest$searchBox();
}
