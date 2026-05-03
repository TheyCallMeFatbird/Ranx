package net.birb.ranx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stored in config as JSON. UUIDs are stored as strings for easy editing.
 */
public final class RanxData {
	public int schemaVersion = 1;
	public Map<String, String> ranksByUuid = new HashMap<>();
	public Set<String> mutedUuids = new HashSet<>();

	/**
	 * "Hardcoded" by-name ranks (name -> rank id). These are applied only when a UUID has no explicit saved rank.
	 * Keep this in the config so you can edit without rebuilding, while still shipping defaults from code.
	 */
	public Map<String, String> defaultNameRanks = new HashMap<>();
}

