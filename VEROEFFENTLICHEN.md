# Craft Planner veröffentlichen – Checkliste

Die Mod-Datei für den Upload ist **`build/libs/craftplanner-1.0.0+mc26.3.jar`**
(nicht die `-sources.jar`). Neu bauen mit `./gradlew build`.

## Vor dem Hochladen

- [ ] In einer **frischen** CurseForge-Instanz testen: nur Fabric Loader + Fabric API + Craft Planner
- [ ] Im Spiel auf Englisch **und** Deutsch kurz die Tooltips anschauen
- [ ] Optional: Quellcode auf GitHub hochladen und den Link unten bei „Quellcode“ eintragen

## Angaben für beide Seiten

| Feld | Wert |
|---|---|
| Name | Craft Planner |
| Kurzbeschreibung | Plan what you want to craft and see which ingredients you still need. |
| Beschreibung | Inhalt von `README.md` (Abschnitte „How it works“ und „Details“) |
| Icon | `src/main/resources/assets/craftplanner/icon.png` |
| Screenshots | `docs/screenshots/` |
| Lizenz | MIT |
| Loader | Fabric |
| Minecraft-Version | 26.3 |
| Abhängigkeit | Fabric API (**required**) |
| Client / Server | Client: **required**, Server: **unsupported** (läuft nur beim Spieler) |
| Kategorien | Utility, Storage / Management (je nach Seite) |
| Versionsnummer | 1.0.0+mc26.3 |
| Release-Kanal | Release (oder Beta, wenn du erst Feedback willst) |
| Changelog | Abschnitt „1.0.0“ aus `CHANGELOG.md` |

## Modrinth (modrinth.com)

1. Konto anlegen und einloggen
2. „Create a project“ → Typ **Mod**, Name, Kurzbeschreibung, Lizenz MIT
3. Beschreibung, Icon, Galerie (Screenshots), Kategorien, Client/Server-Angaben ausfüllen
4. „Versions“ → „Create version“ → Jar hochladen, Loader **Fabric**, Version **26.3**,
   Abhängigkeit **Fabric API** als *required* hinzufügen
5. Projekt zur Prüfung einreichen („Submit for review“). Die Prüfung dauert meist ein paar Tage.

## CurseForge (authors.curseforge.com)

1. Mit deinem CurseForge-Konto einloggen
2. „Create Project“ → Spiel **Minecraft**, Klasse **Mods**
3. Name, Kurzbeschreibung, Beschreibung, Icon, Kategorien, Lizenz MIT
4. Datei hochladen: Jar, Release-Typ, Version **26.3**, Loader **Fabric**, Umgebung **Client**,
   „Relations“ → **Fabric API** als *Required Dependency*
5. Projekt wird von CurseForge geprüft, danach ist es öffentlich

## Neue Version später

1. Änderungen machen, in `gradle.properties` `mod_version` erhöhen (z. B. `1.0.1`)
2. `CHANGELOG.md` ergänzen
3. `./gradlew runClientGameTest` (automatischer Spieltest) und `./gradlew build`
4. Neue Jar auf beiden Seiten als neue Version hochladen

## Neue Minecraft-Version

`minecraft_version`, `loader_version`, `fabric_api_version` und ggf. `loom_version` in
`gradle.properties` anpassen (aktuelle Werte: https://fabricmc.net/develop), in
`fabric.mod.json` die Minecraft-Version ändern, bauen, Spieltest laufen lassen.
