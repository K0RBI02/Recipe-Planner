package de.korbi8674.recipeplanner.client.planer;

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

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.LevelResource;

import de.korbi8674.recipeplanner.RecipePlanner;

/**
 * Die Liste der Dinge, die man bauen will. Jede Einzelspielerwelt und jeder Server hat eine eigene Liste,
 * gespeichert in config/recipeplanner/.
 */
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
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir();
	private static final Path ORDNER = CONFIG.resolve(RecipePlanner.MOD_ID);
	/**
	 * Gemeinsame Liste aus früheren Versionen (vor 1.1.0 galt eine Liste für alle Welten).
	 * Sie wird in die erste Welt übernommen, die man danach öffnet.
	 */
	private static final List<Path> ALTE_DATEIEN = List.of(
			CONFIG.resolve("recipeplanner.json"),
			CONFIG.resolve("craftplanner.json"),
			CONFIG.resolve("meinemod-bauplaner.json"));

	private static List<Ziel> ziele;
	private static String geladenFuer;

	private PlanerListe() {
	}

	/** Die Liste der Welt bzw. des Servers, in dem man gerade ist. */
	public static List<Ziel> ziele() {
		String welt = aktuelleWelt();
		if (ziele == null || !welt.equals(geladenFuer)) {
			ziele = laden(welt);
			geladenFuer = welt;
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
		List<Ziel> liste = ziele();
		schreiben(datei(geladenFuer), liste);
	}

	private static void schreiben(Path datei, List<Ziel> liste) {
		JsonArray array = new JsonArray();
		for (Ziel ziel : liste) {
			JsonObject obj = new JsonObject();
			obj.addProperty("item", BuiltInRegistries.ITEM.getKey(ziel.item).toString());
			obj.addProperty("anzahl", ziel.anzahl);
			obj.addProperty("rezept", ziel.rezeptNr);
			array.add(obj);
		}
		try {
			Files.createDirectories(ORDNER);
			try (Writer writer = Files.newBufferedWriter(datei)) {
				GSON.toJson(array, writer);
			}
		} catch (IOException e) {
			RecipePlanner.LOGGER.error("Could not save the Recipe Planner list", e);
		}
	}

	/**
	 * Schlüssel für die aktuelle Welt: bei Einzelspieler der Ordnername des Spielstands
	 * (bleibt gleich, wenn man die Welt umbenennt), bei Servern die Adresse.
	 */
	static String aktuelleWelt() {
		Minecraft minecraft = Minecraft.getInstance();
		IntegratedServer einzelspieler = minecraft.getSingleplayerServer();
		if (einzelspieler != null) {
			Path ordner = einzelspieler.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
			return "world_" + ordner;
		}
		ServerData server = minecraft.getCurrentServer();
		if (server != null) {
			return "server_" + server.ip;
		}
		return "none";
	}

	private static Path datei(String welt) {
		// Lesbarer Dateiname plus kurzer Hash, damit sich ähnliche Namen (z. B. mit Umlauten) nicht überschneiden.
		String lesbar = welt.replaceAll("[^A-Za-z0-9._-]", "_");
		return ORDNER.resolve(lesbar + "_" + Integer.toHexString(welt.hashCode()) + ".json");
	}

	private static List<Ziel> laden(String welt) {
		Path datei = datei(welt);
		if (Files.exists(datei)) {
			return lesen(datei);
		}
		if (welt.equals("none")) {
			return new ArrayList<>();
		}
		// Neue Welt: einmalig die alte gemeinsame Liste übernehmen, falls es eine gibt.
		for (Path alt : ALTE_DATEIEN) {
			if (Files.exists(alt)) {
				List<Ziel> liste = lesen(alt);
				// Sofort in die Welt-Datei schreiben, erst dann die alte Datei wegräumen.
				schreiben(datei, liste);
				try {
					Files.move(alt, alt.resolveSibling(alt.getFileName() + ".migrated"));
				} catch (IOException e) {
					RecipePlanner.LOGGER.error("Could not move the old Recipe Planner list", e);
				}
				RecipePlanner.LOGGER.info("Moved the old shared Recipe Planner list into {}", welt);
				return liste;
			}
		}
		return new ArrayList<>();
	}

	private static List<Ziel> lesen(Path datei) {
		List<Ziel> liste = new ArrayList<>();
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
			RecipePlanner.LOGGER.error("Could not load the Recipe Planner list", e);
		}
		return liste;
	}
}
