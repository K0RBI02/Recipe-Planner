package de.korbi8674.craftplanner.test;

import java.util.List;
import java.util.stream.Collectors;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import de.korbi8674.craftplanner.CraftPlanner;
import de.korbi8674.craftplanner.client.mixin.OverlayRecipeComponentAccessor;
import de.korbi8674.craftplanner.client.planer.Berechnung;
import de.korbi8674.craftplanner.client.planer.PlanerListe;
import de.korbi8674.craftplanner.client.planer.PlanerPanel;
import de.korbi8674.craftplanner.client.planer.RezeptIndex;
import de.korbi8674.craftplanner.test.mixin.RecipeBookComponentAccessor;
import de.korbi8674.craftplanner.test.mixin.RecipeBookPageAccessor;
import de.korbi8674.craftplanner.test.mixin.RecipeBookScreenAccessor;

/** Spielt den Bauplaner einmal komplett durch und macht dabei Screenshots. */
@SuppressWarnings("UnstableApiUsage")
public class BauplanerClientTest implements FabricClientGameTest {
	private static final int LINKS = InputConstants.MOUSE_BUTTON_LEFT;
	private static final int RECHTS = InputConstants.MOUSE_BUTTON_RIGHT;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(mc -> PlanerListe.ziele().clear());
		context.getInput().resizeWindow(1920, 1080);

		try (TestSingleplayerContext welt = context.worldBuilder().create()) {
			welt.getConnection().waitForChunksRender();
			welt.getServer().runCommand("gamemode survival @a");
			welt.getServer().runCommand("recipe take @a *");
			for (String rezept : List.of("piston", "hopper", "chest", "oak_planks", "spruce_planks", "birch_planks")) {
				welt.getServer().runCommand("recipe give @a minecraft:" + rezept);
			}
			welt.getServer().runCommand("give @a minecraft:piston 5");
			welt.getServer().runCommand("give @a minecraft:cobblestone 100");
			welt.getServer().runCommand("give @a minecraft:oak_planks 64");
			welt.getServer().runCommand("give @a minecraft:spruce_planks 70");
			welt.getServer().runCommand("give @a minecraft:redstone 12");
			context.waitTicks(20);
			context.runOnClient(mc -> mc.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, true));

			// 1) Werkbank mit offenem Rezeptbuch: leeres Panel mit "?"
			BlockPos werkbank = context.computeOnClient(mc -> mc.player.blockPosition().west(2));
			welt.getServer().runCommand("setblock " + werkbank.getX() + " " + werkbank.getY() + " " + werkbank.getZ() + " minecraft:crafting_table");
			context.waitTicks(10);
			context.getInput().lookAt(werkbank);
			context.waitTicks(2);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(CraftingScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("bauplaner-1-leer");
			String knoepfe = context.computeOnClient(mc -> seite(mc).stream()
					.filter(b -> b.visible).map(b -> b.getDisplayStack().getItem() + (b.isOnlyOption() ? "" : "*"))
					.collect(Collectors.joining(", ")));
			CraftPlanner.LOGGER.info("[TEST] Rezeptbuch-Knöpfe: {}", knoepfe);

			// 2) Rechtsklick auf Kolben im Rezeptbuch: +1, mit Shift +10 → 1 + 4×10 + 4 = 45
			sucheImRezeptbuch(context, "piston");
			klickeAuf(context, rezeptKnopf(context, Items.PISTON), RECHTS);
			context.getInput().holdShift();
			for (int i = 0; i < 4; i++) {
				klickeAuf(context, rezeptKnopf(context, Items.PISTON), RECHTS);
			}
			context.getInput().releaseShift();
			for (int i = 0; i < 4; i++) {
				klickeAuf(context, rezeptKnopf(context, Items.PISTON), RECHTS);
			}
			pruefe(anzahl(context, Items.PISTON) == 45, "Kolben sollten 45 sein: " + context.computeOnClient(BauplanerClientTest::beschreibe));

			// 3) 20 Trichter per Shift + Rechtsklick
			sucheImRezeptbuch(context, "hopper");
			context.getInput().holdShift();
			klickeAuf(context, rezeptKnopf(context, Items.HOPPER), RECHTS);
			klickeAuf(context, rezeptKnopf(context, Items.HOPPER), RECHTS);
			context.getInput().releaseShift();
			pruefe(anzahl(context, Items.HOPPER) == 20, "Trichter sollten 20 sein");
			context.takeScreenshot("bauplaner-2-liste");
			CraftPlanner.LOGGER.info("[TEST] Liste: {}", context.computeOnClient(BauplanerClientTest::beschreibe));

			// 4) In der Liste: Linksklick +1, Rechtsklick -1
			klickeAufZeile(context, 0, LINKS);
			pruefe(anzahl(context, Items.PISTON) == 46, "Linksklick in der Liste sollte +1 geben");
			klickeAufZeile(context, 0, RECHTS);
			pruefe(anzahl(context, Items.PISTON) == 45, "Rechtsklick in der Liste sollte -1 geben");

			// 5) Bretter haben Varianten: Rechtsklick öffnet die Auswahl, Rechtsklick auf eine Variante nimmt sie
			sucheImRezeptbuch(context, "planks");
			klickeAuf(context, rezeptKnopf(context, Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS), RECHTS);
			pruefe(context.computeOnClient(mc -> overlay(mc).isEmpty() == false), "Varianten-Auswahl ging nicht auf");
			context.takeScreenshot("bauplaner-3-varianten");
			AbstractWidget variante = context.computeOnClient(mc -> (AbstractWidget) overlay(mc).get(0));
			klickeAuf(context, variante, RECHTS);
			int bretterZiele = context.computeOnClient(mc -> (int) PlanerListe.ziele().stream()
					.filter(z -> z.item == Items.OAK_PLANKS || z.item == Items.SPRUCE_PLANKS || z.item == Items.BIRCH_PLANKS).count());
			pruefe(bretterZiele == 1, "Variante wurde nicht hinzugefügt: " + context.computeOnClient(BauplanerClientTest::beschreibe));
			// Der nächste Klick schließt erst die Varianten-Auswahl (wie in Vanilla) ...
			klickeAufZeile(context, 2, RECHTS);
			pruefe(context.computeOnClient(mc -> overlay(mc).isEmpty()), "Varianten-Auswahl sollte zu sein");
			// ... danach ist die Liste wieder normal bedienbar: Rechtsklick auf die dritte Zeile (Anzahl 1 → 0)
			klickeAufZeile(context, 2, RECHTS);
			pruefe(context.computeOnClient(mc -> PlanerListe.ziele().size()) == 2, "Bretter sollten wieder weg sein");

			// 5b) Wenig Platz (schmaleres Fenster, GUI-Skalierung 4, Rezeptbuch offen): Liste muss trotzdem sichtbar sein
			context.getInput().resizeWindow(1640, 1080);
			context.runOnClient(mc -> {
				mc.options.guiScale().set(4);
				mc.resizeGui();
			});
			context.waitTicks(5);
			context.takeScreenshot("bauplaner-5-wenig-platz");
			pruefe(context.computeOnClient(mc -> Screens.getWidgets(mc.gui.screen()).stream()
					.anyMatch(w -> w instanceof PlanerPanel && w.visible)), "Liste ist bei wenig Platz unsichtbar");
			context.getInput().resizeWindow(1920, 1080);
			context.runOnClient(mc -> {
				mc.options.guiScale().set(0);
				mc.resizeGui();
			});
			context.waitTicks(5);

			// 6) Kiste mit Eisen daneben stellen und öffnen: Kisteninhalt zählt mit
			context.setScreen(() -> null);
			BlockPos kiste = context.computeOnClient(mc -> mc.player.blockPosition().east(2));
			String pos = kiste.getX() + " " + kiste.getY() + " " + kiste.getZ();
			welt.getServer().runCommand("setblock " + pos + " minecraft:chest");
			welt.getServer().runCommand("item replace block " + pos + " container.0 with minecraft:iron_ingot 64");
			welt.getServer().runCommand("item replace block " + pos + " container.1 with minecraft:redstone 30");
			context.waitTicks(10);
			context.getInput().lookAt(kiste);
			context.waitTicks(2);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(ContainerScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("bauplaner-4-kiste");
			CraftPlanner.LOGGER.info("[TEST] Mit Kiste: {}", context.computeOnClient(BauplanerClientTest::beschreibe));

			context.setScreen(() -> null);
		}
	}

	private static void sucheImRezeptbuch(ClientGameTestContext context, String text) {
		AbstractWidget suche = context.computeOnClient(mc -> ((RecipeBookComponentAccessor) ((RecipeBookScreenAccessor) mc.gui.screen()).craftplannertest$component()).craftplannertest$searchBox());
		klickeAuf(context, suche, LINKS);
		for (int i = 0; i < 15; i++) {
			context.getInput().pressKey(InputConstants.KEY_BACKSPACE);
		}
		context.getInput().typeChars(text);
		context.waitTicks(3);
	}

	private static List<RecipeButton> seite(Minecraft mc) {
		RecipeBookPage seite = ((RecipeBookComponentAccessor) ((RecipeBookScreenAccessor) mc.gui.screen()).craftplannertest$component()).craftplannertest$page();
		return ((RecipeBookPageAccessor) seite).craftplannertest$buttons();
	}

	private static List<?> overlay(Minecraft mc) {
		RecipeBookPage seite = ((RecipeBookComponentAccessor) ((RecipeBookScreenAccessor) mc.gui.screen()).craftplannertest$component()).craftplannertest$page();
		var overlay = ((RecipeBookPageAccessor) seite).craftplannertest$overlay();
		return overlay.isVisible() ? ((OverlayRecipeComponentAccessor) overlay).craftplanner$recipeButtons() : List.of();
	}

	private static AbstractWidget rezeptKnopf(ClientGameTestContext context, Item... items) {
		return context.computeOnClient(mc -> seite(mc).stream()
				.filter(b -> b.visible && List.of(items).contains(b.getDisplayStack().getItem()))
				.findFirst().orElseThrow(() -> new AssertionError("Kein Rezeptknopf für " + List.of(items))));
	}

	private static void klickeAufZeile(ClientGameTestContext context, int zeile, int taste) {
		PlanerPanel panel = context.computeOnClient(mc -> (PlanerPanel) Screens.getWidgets(mc.gui.screen()).stream()
				.filter(w -> w instanceof PlanerPanel).findFirst().orElseThrow());
		klickeAuf(context, panel.getX() + 10, panel.getY() + 5 + zeile * 18 + 9, taste);
	}

	private static void klickeAuf(ClientGameTestContext context, AbstractWidget widget, int taste) {
		klickeAuf(context, widget.getX() + widget.getWidth() / 2, widget.getY() + widget.getHeight() / 2, taste);
	}

	private static void klickeAuf(ClientGameTestContext context, int guiX, int guiY, int taste) {
		double skala = context.computeOnClient(mc -> (double) mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(guiX * skala, guiY * skala);
		context.waitTick();
		context.getInput().pressMouse(taste);
		context.waitTicks(2);
	}

	private static int anzahl(ClientGameTestContext context, Item item) {
		return context.computeOnClient(mc -> PlanerListe.ziele().stream().filter(z -> z.item == item).mapToInt(z -> z.anzahl).sum());
	}

	private static String beschreibe(Minecraft mc) {
		if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
			return "kein Inventar offen";
		}
		Berechnung r = Berechnung.rechnen(PlanerListe.ziele(), RezeptIndex.ausRezeptbuch(mc), Berechnung.vorrat(screen.getMenu()));
		String ziele = r.ziele.stream()
				.map(z -> z.ziel().item + " " + z.vorhanden() + "/" + z.ziel().anzahl)
				.collect(Collectors.joining(", "));
		String bedarf = r.bedarf.stream()
				.map(b -> b.zutat().items().findFirst().map(h -> h.value().toString()).orElse("?") + " " + b.vorhanden() + "/" + b.benoetigt())
				.collect(Collectors.joining(", "));
		return "Ziele [" + ziele + "] Bedarf [" + bedarf + "]";
	}

	private static void pruefe(boolean bedingung, String fehler) {
		if (!bedingung) {
			throw new AssertionError(fehler);
		}
	}
}
