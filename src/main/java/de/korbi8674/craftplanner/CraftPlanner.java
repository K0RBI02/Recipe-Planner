package de.korbi8674.craftplanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Gemeinsame Konstanten. Die eigentliche Mod läuft nur auf dem Client, siehe CraftPlannerClient. */
public final class CraftPlanner {
	public static final String MOD_ID = "craftplanner";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private CraftPlanner() {
	}
}
