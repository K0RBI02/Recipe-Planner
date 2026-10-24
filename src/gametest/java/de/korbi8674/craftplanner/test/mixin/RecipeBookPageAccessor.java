package de.korbi8674.craftplanner.test.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;

@Mixin(RecipeBookPage.class)
public interface RecipeBookPageAccessor {
	@Accessor("buttons")
	List<RecipeButton> craftplannertest$buttons();

	@Accessor("overlay")
	OverlayRecipeComponent craftplannertest$overlay();
}
