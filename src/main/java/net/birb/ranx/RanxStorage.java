package net.birb.ranx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RanxStorage {
	private RanxStorage() {}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("ranx.json");
	}

	public static RanxData loadOrCreateDefaults() {
		Path path = configPath();
		if (!Files.exists(path)) {
			RanxData d = defaults();
			save(d);
			return d;
		}

		try {
			String json = Files.readString(path, StandardCharsets.UTF_8);
			RanxData d = GSON.fromJson(json, RanxData.class);
			if (d == null) d = defaults();
			applyMissingDefaults(d);
			return d;
		} catch (Exception e) {
			Ranx.LOGGER.error("Failed to read ranx.json, using defaults", e);
			RanxData d = defaults();
			save(d);
			return d;
		}
	}

	public static void save(RanxData data) {
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(data), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Ranx.LOGGER.error("Failed to save ranx.json", e);
		}
	}

	private static RanxData defaults() {
		RanxData d = new RanxData();
		// Put your server owners here (lowercase player name -> rank id).
		// Example:
		// d.defaultNameRanks.put("tcmfatbird", "owner");
		return d;
	}

	private static void applyMissingDefaults(RanxData d) {
		RanxData def = defaults();
		def.defaultNameRanks.forEach(d.defaultNameRanks::putIfAbsent);
	}
}

