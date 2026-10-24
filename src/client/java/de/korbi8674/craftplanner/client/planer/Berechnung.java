package de.korbi8674.craftplanner.client.planer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Rechnet aus, welche direkten Zutaten für die Liste gebraucht werden und was schon da ist. */
public final class Berechnung {
	public record ZielStand(PlanerListe.Ziel ziel, RezeptIndex.Rezept rezept, int rezeptAnzahl, int vorhanden) {
	}

	public record Bedarf(Ingredient zutat, int benoetigt, int vorhanden) {
		public boolean reicht() {
			return vorhanden >= benoetigt;
		}
	}

	public final List<ZielStand> ziele = new ArrayList<>();
	public final List<Bedarf> bedarf = new ArrayList<>();

	private Berechnung() {
	}

	public static Berechnung rechnen(List<PlanerListe.Ziel> liste, RezeptIndex index, List<ItemStack> vorrat) {
		Berechnung ergebnis = new Berechnung();
		Map<Ingredient, Integer> summe = new LinkedHashMap<>();

		for (PlanerListe.Ziel ziel : liste) {
			List<RezeptIndex.Rezept> rezepte = index.rezepteFuer(ziel.item);
			int vorhanden = zaehlen(vorrat, ziel.item);
			if (rezepte.isEmpty()) {
				ergebnis.ziele.add(new ZielStand(ziel, null, 0, vorhanden));
				continue;
			}

			RezeptIndex.Rezept rezept = rezepte.get(Math.floorMod(ziel.rezeptNr, rezepte.size()));
			ergebnis.ziele.add(new ZielStand(ziel, rezept, rezepte.size(), vorhanden));

			// Was schon fertig im Inventar liegt, muss nicht mehr gebaut werden.
			int fehlend = Math.max(0, ziel.anzahl - vorhanden);
			int craftVorgaenge = (fehlend + rezept.ergebnisAnzahl() - 1) / rezept.ergebnisAnzahl();
			for (Ingredient zutat : rezept.zutaten()) {
				summe.merge(zutat, craftVorgaenge, Integer::sum);
			}
		}

		summe.forEach((zutat, menge) -> {
			if (menge > 0) {
				ergebnis.bedarf.add(new Bedarf(zutat, menge, zaehlen(vorrat, zutat)));
			}
		});
		return ergebnis;
	}

	/** Alle Items aus dem eigenen Inventar und dem gerade offenen Behälter (ohne Rüstung und Ergebnis-Slot). */
	public static List<ItemStack> vorrat(AbstractContainerMenu menu) {
		List<ItemStack> stacks = new ArrayList<>();
		for (Slot slot : menu.slots) {
			if (slot instanceof ResultSlot || !slot.hasItem()) {
				continue;
			}
			if (slot.container instanceof Inventory && slot.getContainerSlot() >= Inventory.INVENTORY_SIZE) {
				continue; // Rüstung und Nebenhand
			}
			stacks.add(slot.getItem());
		}
		if (!menu.getCarried().isEmpty()) {
			stacks.add(menu.getCarried()); // Item, das man gerade mit der Maus hält
		}
		return stacks;
	}

	private static int zaehlen(List<ItemStack> vorrat, Item item) {
		int anzahl = 0;
		for (ItemStack stack : vorrat) {
			if (stack.is(item)) {
				anzahl += stack.getCount();
			}
		}
		return anzahl;
	}

	private static int zaehlen(List<ItemStack> vorrat, Ingredient zutat) {
		int anzahl = 0;
		for (ItemStack stack : vorrat) {
			if (zutat.test(stack)) {
				anzahl += stack.getCount();
			}
		}
		return anzahl;
	}
}
