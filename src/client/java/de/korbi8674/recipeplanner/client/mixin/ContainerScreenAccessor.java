package de.korbi8674.recipeplanner.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Gibt Zugriff auf Position und Größe des Inventarfensters, die in Minecraft "protected" sind. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int recipeplanner$leftPos();

	@Accessor("topPos")
	int recipeplanner$topPos();

	@Accessor("imageWidth")
	int recipeplanner$imageWidth();

	@Accessor("imageHeight")
	int recipeplanner$imageHeight();
}
