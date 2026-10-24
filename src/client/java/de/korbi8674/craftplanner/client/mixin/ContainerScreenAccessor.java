package de.korbi8674.craftplanner.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Gibt Zugriff auf Position und Größe des Inventarfensters, die in Minecraft "protected" sind. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int craftplanner$leftPos();

	@Accessor("topPos")
	int craftplanner$topPos();

	@Accessor("imageWidth")
	int craftplanner$imageWidth();

	@Accessor("imageHeight")
	int craftplanner$imageHeight();
}
