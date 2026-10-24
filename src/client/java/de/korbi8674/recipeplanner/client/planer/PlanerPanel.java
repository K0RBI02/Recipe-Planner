package de.korbi8674.recipeplanner.client.planer;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

import de.korbi8674.recipeplanner.RecipePlanner;
import de.korbi8674.recipeplanner.client.mixin.ContainerScreenAccessor;
import de.korbi8674.recipeplanner.client.mixin.RecipeBookScreenAccessor;

/**
 * Die schmale Bauplaner-Liste rechts neben dem Inventar: oben was man bauen will, darunter was dafür fehlt.
 * Neue Einträge kommen per Rechtsklick im Rezeptbuch dazu (siehe RecipeBookPageMixin).
 */
public final class PlanerPanel extends AbstractWidget {
	/** Nur eine Spalte Item-Slots, die Zahl steht wie die Stapelgröße auf dem Icon. */
	private static final int BREITE = 26;
	private static final int RAND = 5;
	private static final int ZEILE = 18;
	private static final int TRENNER = 7;
	/** Platz, den wir neben dem Fenster nach Knöpfen anderer Mods (z. B. Sortieren) absuchen. */
	private static final int FREMDE_KNOEPFE_SUCHBEREICH = 40;

	private static final int FARBE_GRAU = 0xFF8B8B8B;
	private static final int FARBE_OK = 0xFF207A20;
	private static final int FARBE_FEHLT = 0xFFB02020;

	private record Zeile(Object daten, int hoehe) {
	}

	private final AbstractContainerScreen<?> screen;
	private final ContainerScreenAccessor accessor;
	private final Minecraft minecraft;
	private final Font font;
	private final boolean hatRezeptbuch;

	private RezeptIndex index;
	private int rezeptAnzahlBeiIndex = -1;
	private int scroll;
	private int letzteInhaltHoehe;
	private String letzteDiagnose = "";

	private PlanerPanel(AbstractContainerScreen<?> screen, Minecraft minecraft) {
		super(0, 0, 0, 0, Component.translatable("recipeplanner.title"));
		this.screen = screen;
		this.accessor = (ContainerScreenAccessor) screen;
		this.minecraft = minecraft;
		this.font = minecraft.font;
		this.hatRezeptbuch = screen instanceof AbstractRecipeBookScreen<?>;
	}

	public static void anbringen(AbstractContainerScreen<?> screen, Minecraft minecraft) {
		PlanerPanel panel = new PlanerPanel(screen, minecraft);
		Screens.getWidgets(screen).add(panel);
		ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> panel.layout());
		ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, h, v) -> !panel.scrollen(mouseX, mouseY, v));
		panel.layout();
	}

	// ---------------------------------------------------------------- Layout

	private void layout() {
		int fensterRechts = accessor.recipeplanner$leftPos() + accessor.recipeplanner$imageWidth();
		int y = accessor.recipeplanner$topPos();
		int rechts = fensterRechts + 3;

		// Knöpfe anderer Mods direkt neben dem Fenster (z. B. Sortieren) nicht verdecken.
		int fensterUnten = y + accessor.recipeplanner$imageHeight();
		String fremdeKnoepfe = "";
		for (GuiEventListener kind : screen.children()) {
			if (kind != this && kind instanceof AbstractWidget w && w.visible
					&& w.getX() >= fensterRechts - 4 && w.getX() < fensterRechts + FREMDE_KNOEPFE_SUCHBEREICH
					&& w.getBottom() > y && w.getY() < fensterUnten) {
				rechts = Math.max(rechts, w.getRight() + 3);
				fremdeKnoepfe += " " + w.getClass().getSimpleName() + "@" + w.getX() + "-" + w.getRight();
			}
		}

		// Linke Kante: das Fenster, bei offenem Rezeptbuch dessen Reiter ganz links.
		int fensterLinks = accessor.recipeplanner$leftPos();
		if (screen instanceof AbstractRecipeBookScreen<?> && ((RecipeBookScreenAccessor) screen).recipeplanner$recipeBook().isVisible()) {
			fensterLinks = Math.min(fensterLinks, (screen.width - RecipeBookComponent.IMAGE_WIDTH) / 2 - 86 - 32);
		}

		// Wohin? Rechts neben das Fenster, sonst links daneben,
		// zur Not an den rechten Bildschirmrand (dann über fremden Knöpfen).
		String platz;
		int x;
		if (rechts + BREITE <= screen.width - 1) {
			x = rechts;
			platz = "rechts";
		} else if (fensterLinks - 3 - BREITE >= 1) {
			x = fensterLinks - 3 - BREITE;
			platz = "links";
		} else if (screen.width - 1 - BREITE >= fensterRechts) {
			x = screen.width - 1 - BREITE;
			platz = "rechter Rand";
		} else {
			x = rechts;
			platz = "kein Platz";
		}

		// Höchstens so tief wie das Fenster selbst, darüber hinaus wird gescrollt.
		int hoehe = Math.min(2 * RAND + Math.max(ZEILE, letzteInhaltHoehe), Math.min(fensterUnten, screen.height - 4) - y);
		// Leer und ohne Rezeptbuch (z. B. Kiste) braucht man das Panel nicht.
		boolean leer = PlanerListe.ziele().isEmpty();
		visible = !platz.equals("kein Platz") && hoehe >= ZEILE + 2 * RAND && (!leer || hatRezeptbuch);
		setRectangle(BREITE, hoehe, x, y);

		// Diagnose fürs Log, nur wenn sich etwas ändert
		String diagnose = screen.getClass().getSimpleName() + " " + screen.width + "x" + screen.height
				+ " (GUI-Skalierung " + minecraft.getWindow().getGuiScale() + "), Fenster " + fensterLinks + "-" + fensterRechts
				+ ", Platz: " + platz + " x=" + x + ", sichtbar=" + visible + (leer ? " (Liste leer)" : "")
				+ (fremdeKnoepfe.isEmpty() ? "" : ", fremde Knöpfe:" + fremdeKnoepfe);
		if (!diagnose.equals(letzteDiagnose)) {
			letzteDiagnose = diagnose;
			RecipePlanner.LOGGER.debug("Panel layout: {}", diagnose);
		}
	}

	private int inhaltOben() {
		return getY() + RAND;
	}

	private int inhaltUnten() {
		return getBottom() - RAND;
	}

	// ---------------------------------------------------------------- Zeilen

	private Berechnung rechnen() {
		// Neu einlesen, wenn im Rezeptbuch etwas freigeschaltet wurde.
		int anzahl = minecraft.player == null ? 0 : minecraft.player.getRecipeBook().getCollections().stream()
				.mapToInt(sammlung -> sammlung.getRecipes().size()).sum();
		if (index == null || anzahl != rezeptAnzahlBeiIndex) {
			index = RezeptIndex.ausRezeptbuch(minecraft);
			rezeptAnzahlBeiIndex = anzahl;
		}
		return Berechnung.rechnen(PlanerListe.ziele(), index, Berechnung.vorrat(screen.getMenu()));
	}

	private List<Zeile> zeilen(Berechnung rechnung) {
		List<Zeile> zeilen = new ArrayList<>();
		for (Berechnung.ZielStand stand : rechnung.ziele) {
			zeilen.add(new Zeile(stand, ZEILE));
		}
		if (!rechnung.bedarf.isEmpty()) {
			zeilen.add(new Zeile(null, TRENNER));
			for (Berechnung.Bedarf bedarf : rechnung.bedarf) {
				zeilen.add(new Zeile(bedarf, ZEILE));
			}
		}
		return zeilen;
	}

	// ---------------------------------------------------------------- Zeichnen

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		List<Zeile> zeilen = zeilen(rechnen());
		letzteInhaltHoehe = zeilen.stream().mapToInt(Zeile::hoehe).sum();
		int maxScroll = Math.max(0, letzteInhaltHoehe - (inhaltUnten() - inhaltOben()));
		scroll = Math.clamp(scroll, 0, maxScroll);

		hintergrund(g);

		if (zeilen.isEmpty()) {
			g.centeredText(font, "?", getX() + getWidth() / 2, getY() + getHeight() / 2 - 4, FARBE_GRAU);
			if (isHovered()) {
				g.setComponentTooltipForNextFrame(font, List.of(
						Component.translatable("recipeplanner.title"),
						grau("hint.line1"),
						grau("hint.line2"),
						grau("hint.line3")), mouseX, mouseY);
			}
			return;
		}

		g.enableScissor(getX() + 2, inhaltOben(), getRight() - 2, inhaltUnten());
		int y = inhaltOben() - scroll;
		boolean mausImInhalt = isHovered() && mouseY >= inhaltOben() && mouseY < inhaltUnten();
		for (Zeile zeile : zeilen) {
			if (y + zeile.hoehe() > inhaltOben() && y < inhaltUnten()) {
				boolean hover = mausImInhalt && mouseY >= y && mouseY < y + zeile.hoehe();
				if (zeile.daten() instanceof Berechnung.ZielStand stand) {
					zeichneZiel(g, stand, y, hover, mouseX, mouseY);
				} else if (zeile.daten() instanceof Berechnung.Bedarf bedarf) {
					zeichneBedarf(g, bedarf, y, hover, mouseX, mouseY);
				} else {
					g.horizontalLine(getX() + RAND, getRight() - RAND - 1, y + TRENNER / 2, FARBE_GRAU);
				}
			}
			y += zeile.hoehe();
		}
		g.disableScissor();

		if (maxScroll > 0) {
			int bereich = inhaltUnten() - inhaltOben();
			int balken = Math.max(8, bereich * bereich / (bereich + maxScroll));
			int balkenY = inhaltOben() + (bereich - balken) * scroll / maxScroll;
			g.fill(getRight() - 3, balkenY, getRight() - 1, balkenY + balken, FARBE_GRAU);
		}
	}

	private void hintergrund(GuiGraphicsExtractor g) {
		int x0 = getX();
		int y0 = getY();
		int x1 = getRight();
		int y1 = getBottom();
		// Im Stil der Minecraft-Fenster: hellgrau mit hellem und dunklem Rand
		g.fill(x0, y0, x1, y1, 0xFF000000);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFFC6C6C6);
		g.fill(x0 + 1, y0 + 1, x1 - 2, y0 + 3, 0xFFFFFFFF);
		g.fill(x0 + 1, y0 + 1, x0 + 3, y1 - 2, 0xFFFFFFFF);
		g.fill(x0 + 2, y1 - 3, x1 - 1, y1 - 1, 0xFF555555);
		g.fill(x1 - 3, y0 + 2, x1 - 1, y1 - 1, 0xFF555555);
	}

	private void zeichneZiel(GuiGraphicsExtractor g, Berechnung.ZielStand stand, int y, boolean hover, int mouseX, int mouseY) {
		PlanerListe.Ziel ziel = stand.ziel();
		ItemStack stack = new ItemStack(ziel.item);
		int x = getX() + RAND;
		if (hover) {
			g.fill(x - 1, y, getRight() - RAND, y + ZEILE, 0x50FFFFFF);
		}
		g.fakeItem(stack, x, y + 1);
		boolean fertig = stand.vorhanden() >= ziel.anzahl;
		zahl(g, stack, x, y, fertig ? "✔" : Integer.toString(ziel.anzahl), fertig ? "§a" : "§f");

		if (hover) {
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(stack.getHoverName());
			tooltip.add(grau("target.amount", ziel.anzahl, Math.min(stand.vorhanden(), ziel.anzahl)));
			if (stand.rezept() == null) {
				tooltip.add(Component.translatable("recipeplanner.target.no_recipe").withColor(FARBE_FEHLT));
			}
			tooltip.add(grau("target.controls"));
			tooltip.add(grau("target.controls_shift"));
			g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	private void zeichneBedarf(GuiGraphicsExtractor g, Berechnung.Bedarf bedarf, int y, boolean hover, int mouseX, int mouseY) {
		List<Item> moeglich = bedarf.zutat().items().map(Holder::value).toList();
		if (moeglich.isEmpty()) {
			return;
		}
		// Bei "beliebige Bretter" usw. wechselt das Bild jede Sekunde.
		ItemStack stack = new ItemStack(moeglich.get((int) (System.currentTimeMillis() / 1000 % moeglich.size())));
		int x = getX() + RAND;
		if (hover) {
			g.fill(x - 1, y, getRight() - RAND, y + ZEILE, 0x50FFFFFF);
		}
		g.fakeItem(stack, x, y + 1);
		int fehlt = bedarf.benoetigt() - bedarf.vorhanden();
		zahl(g, stack, x, y, bedarf.reicht() ? "✔" : Integer.toString(fehlt), bedarf.reicht() ? "§a" : "§c");

		if (hover) {
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(moeglich.size() > 1 ? Component.literal(gemeinsamerName(moeglich)) : stack.getHoverName());
			tooltip.add(grau("ingredient.needed", bedarf.benoetigt(), stacks(bedarf.benoetigt())));
			tooltip.add(grau("ingredient.have", bedarf.vorhanden()));
			if (bedarf.reicht()) {
				tooltip.add(Component.translatable("recipeplanner.ingredient.enough").withColor(FARBE_OK));
			} else {
				tooltip.add(Component.translatable("recipeplanner.ingredient.missing", fehlt, stacks(fehlt)).withColor(FARBE_FEHLT));
			}
			if (moeglich.size() > 1) {
				tooltip.add(grau("ingredient.any", moeglich.size()));
			}
			g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	/** Zahl wie eine Stapelgröße aufs Icon, mit Farbcode (z. B. "§c" für rot). */
	private void zahl(GuiGraphicsExtractor g, ItemStack stack, int x, int y, String text, String farbcode) {
		g.itemDecorations(font, stack, x, y + 1, farbcode + text);
	}

	/**
	 * Name für "beliebige Sorte": das gemeinsame Ende aller Namen,
	 * z. B. "Eichenholzbretter" + "Fichtenholzbretter" → "Holzbretter", "Oak Planks" + "Spruce Planks" → "Planks".
	 */
	private static String gemeinsamerName(List<Item> items) {
		List<String> namen = items.stream().map(item -> new ItemStack(item).getHoverName().getString()).toList();
		String ende = namen.get(0);
		for (String name : namen) {
			int i = 0;
			while (i < ende.length() && i < name.length()
					&& ende.charAt(ende.length() - 1 - i) == name.charAt(name.length() - 1 - i)) {
				i++;
			}
			ende = ende.substring(ende.length() - i);
		}
		ende = ende.strip();
		if (ende.length() < 4) {
			return namen.get(0) + " …";
		}
		return Character.toUpperCase(ende.charAt(0)) + ende.substring(1);
	}

	/** " (2 Stacks + 7)" bzw. leer unter 64. */
	private static Component stacks(int menge) {
		if (menge < 64) {
			return Component.empty();
		}
		int stapel = menge / 64;
		int rest = menge % 64;
		String schluessel = stapel == 1 ? "recipeplanner.stack" : "recipeplanner.stacks";
		return rest > 0
				? Component.translatable(schluessel + "_rest", stapel, rest)
				: Component.translatable(schluessel, stapel);
	}

	private static Component grau(String schluessel, Object... werte) {
		return Component.translatable("recipeplanner." + schluessel, werte).withColor(0xFFA0A0A0);
	}

	// ---------------------------------------------------------------- Eingabe

	@Override
	public boolean shouldTakeFocusAfterInteraction() {
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!isMouseOver(event.x(), event.y())) {
			return false;
		}
		if (zeileBei(event.y()) instanceof Berechnung.ZielStand stand) {
			int schritt = event.hasShiftDown() || minecraft.hasShiftDown() ? 10 : 1;
			if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
				PlanerListe.aendern(stand.ziel(), schritt);
			} else if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
				PlanerListe.aendern(stand.ziel(), -schritt);
			}
			AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
		}
		// Klicks im Panel immer abfangen, sonst wirft Minecraft das gehaltene Item weg.
		return true;
	}

	private Object zeileBei(double mouseY) {
		if (mouseY < inhaltOben() || mouseY >= inhaltUnten()) {
			return null;
		}
		int y = inhaltOben() - scroll;
		for (Zeile zeile : zeilen(rechnen())) {
			if (mouseY >= y && mouseY < y + zeile.hoehe()) {
				return zeile.daten();
			}
			y += zeile.hoehe();
		}
		return null;
	}

	/** Gibt {@code true} zurück, wenn das Panel den Scroll benutzt hat. */
	private boolean scrollen(double mouseX, double mouseY, double menge) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}
		scroll -= (int) (menge * ZEILE);
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}
}
