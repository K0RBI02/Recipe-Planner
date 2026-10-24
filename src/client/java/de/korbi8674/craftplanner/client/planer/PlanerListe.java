package de.korbi8674.craftplanner.client.planer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import de.korbi8674.craftplanner.CraftPlanner;

/** Die Liste der Dinge, die man bauen will. Wird in config/craftplanner.json gespeichert. */
public final class PlanerListe {
	public static final class Ziel {
		public final Item item;
		public int anzahl;
		/** Welches Rezept benutzt wird, falls es mehrere gibt. */
		public int rezeptNr;

		Ziel(Item item, int anzahl, int rezeptNr) {
			this.item = item;
			this.anzahl = anzahl;
			this.rezeptNr = rezeptNr;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path DATEI = FabricLoader.getInstance().getConfigDir().resolve("craftplanner.json");
	/** Dateiname der Vorab-Version, wird beim ersten Start übernommen. */
	private static final Path ALTE_DATEI = FabricLoader.getInstance().getConfigDir().resolve("meinemod-bauplaner.json");
	private static List<Ziel> ziele;

	private PlanerListe() {
	}

	public static List<Ziel> ziele() {
		if (ziele == null) {
			ziele = laden();
		}
		return ziele;
	}

	public static Ziel hinzufuegen(Item item, int anzahl) {
		for (Ziel ziel : ziele()) {
			if (ziel.item == item) {
				ziel.anzahl += anzahl;
				speichern();
				return ziel;
			}
		}
		Ziel ziel = new Ziel(item, anzahl, 0);
		ziele().add(ziel);
		speichern();
		return ziel;
	}

	/** Ändert die Anzahl; bei 0 oder weniger fliegt das Ziel raus. */
	public static void aendern(Ziel ziel, int um) {
		ziel.anzahl += um;
		if (ziel.anzahl <= 0) {
			ziele().remove(ziel);
		}
		speichern();
	}

	public static void entfernen(Ziel ziel) {
		ziele().remove(ziel);
		speichern();
	}

	public static void speichern() {
		JsonArray array = new JsonArray();
		for (Ziel ziel : ziele()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("item", BuiltInRegistries.ITEM.getKey(ziel.item).toString());
			obj.addProperty("anzahl", ziel.anzahl);
			obj.addProperty("rezept", ziel.rezeptNr);
			array.add(obj);
		}
		try (Writer writer = Files.newBufferedWriter(DATEI)) {
			GSON.toJson(array, writer);
		} catch (IOException e) {
			CraftPlanner.LOGGER.error("Could not save the Craft Planner list", e);
		}
	}

	private static List<Ziel> laden() {
		List<Ziel> liste = new ArrayList<>();
		Path datei = Files.exists(DATEI) ? DATEI : ALTE_DATEI;
		if (!Files.exists(datei)) {
			return liste;
		}
		try (Reader reader = Files.newBufferedReader(datei)) {
			for (JsonElement element : GSON.fromJson(reader, JsonArray.class)) {
				JsonObject obj = element.getAsJsonObject();
				Identifier id = Identifier.tryParse(obj.get("item").getAsString());
				if (id == null) {
					continue;
				}
				BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> liste.add(new Ziel(
						item,
						Math.max(1, obj.get("anzahl").getAsInt()),
						obj.has("rezept") ? obj.get("rezept").getAsInt() : 0)));
			}
		} catch (Exception e) {
			CraftPlanner.LOGGER.error("Could not load the Craft Planner list", e);
		}
		return liste;
	}
}
