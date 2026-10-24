package de.korbi8674.recipeplanner.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

@Mixin(AbstractRecipeBookScreen.class)
public interface RecipeBookScreenAccessor {
	@Accessor("recipeBookComponent")
	RecipeBookComponent<?> recipeplannertest$component();
}
