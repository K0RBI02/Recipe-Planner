package de.korbi8674.craftplanner.client.planer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

/**
 * Sammelt alle Rezepte, die der Spieler im Rezeptbuch freigeschaltet hat.
 * Der Server schickt dem Client nur freigeschaltete Rezepte, deshalb sieht man hier nichts "Geheimes".
 */
public final class RezeptIndex {
	/** Ein Rezept: so viele {@code ergebnisAnzahl} Items entstehen aus diesen Zutaten (eine Zutat pro Slot). */
	public record Rezept(Item ergebnis, int ergebnisAnzahl, List<Ingredient> zutaten, int art) {
	}

	private final Map<Item, List<Rezept>> rezepte = new HashMap<>();

	private RezeptIndex() {
	}

	public static RezeptIndex ausRezeptbuch(Minecraft minecraft) {
		RezeptIndex index = new RezeptIndex();
		if (minecraft.player == null || minecraft.level == null) {
			return index;
		}

		ContextMap kontext = SlotDisplayContext.fromLevel(minecraft.level);
		minecraft.player.getRecipeBook().getCollections().forEach(sammlung -> {
			for (RecipeDisplayEntry eintrag : sammlung.getRecipes()) {
				index.hinzufuegen(eintrag, kontext);
			}
		});

		// Werkbank-Rezepte zuerst, danach Steinsäge und Ofen.
		index.rezepte.values().forEach(liste -> liste.sort(Comparator.comparingInt(Rezept::art)));
		return index;
	}

	private void hinzufuegen(RecipeDisplayEntry eintrag, ContextMap kontext) {
		if (eintrag.craftingRequirements().isEmpty()) {
			return; // Spezialrezepte (z. B. Rüstung färben) haben keine festen Zutaten.
		}

		int art = art(eintrag.display());
		if (art < 0) {
			return;
		}

		ItemStack ergebnis = eintrag.display().result().resolveForFirstStack(kontext);
		if (ergebnis.isEmpty()) {
			return;
		}

		List<Ingredient> zutaten = eintrag.craftingRequirements().get();
		List<Rezept> liste = rezepte.computeIfAbsent(ergebnis.getItem(), item -> new ArrayList<>());
		Rezept rezept = new Rezept(ergebnis.getItem(), ergebnis.getCount(), List.copyOf(zutaten), art);
		// Ofen, Räucherofen und Schmelzofen liefern dasselbe Rezept mehrfach.
		if (liste.stream().noneMatch(r -> r.ergebnisAnzahl() == rezept.ergebnisAnzahl() && r.zutaten().equals(rezept.zutaten()))) {
			liste.add(rezept);
		}
	}

	private static int art(RecipeDisplay display) {
		if (display instanceof ShapedCraftingRecipeDisplay || display instanceof ShapelessCraftingRecipeDisplay) {
			return 0;
		} else if (display instanceof StonecutterRecipeDisplay) {
			return 1;
		} else if (display instanceof FurnaceRecipeDisplay) {
			return 2;
		}
		return -1; // Schmiedetisch u. Ä. lassen wir weg.
	}

	public List<Rezept> rezepteFuer(Item item) {
		return rezepte.getOrDefault(item, List.of());
	}

	public Iterable<Item> herstellbareItems() {
		return rezepte.keySet();
	}
}
