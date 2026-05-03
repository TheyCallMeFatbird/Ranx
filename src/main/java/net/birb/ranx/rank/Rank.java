package net.birb.ranx.rank;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Locale;

public enum Rank {
	OWNER("Owner", "#A020F0", 100),
	ADMIN("Admin", "#FF5555", 80),
	MOD("Mod", "#55FFFF", 60),
	HELPER("Helper", "#55FF55", 40),
	PLAYER("Player", "#AAAAAA", 0);

	private final String displayName;
	private final String hexColor;
	private final int power;

	Rank(String displayName, String hexColor, int power) {
		this.displayName = displayName;
		this.hexColor = hexColor;
		this.power = power;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String displayName() {
		return displayName;
	}

	public int power() {
		return power;
	}

	public boolean canChangeRanks() {
		return this == OWNER || this == ADMIN || this == MOD;
	}

	public boolean canModerate() {
		return this == OWNER || this == ADMIN || this == MOD;
	}

	public Component styledTag() {
		// Example: [OWNER]
		return Component.literal("[" + displayName.toUpperCase(Locale.ROOT) + "]")
			.setStyle(Style.EMPTY.withColor(parseColor(hexColor)));
	}

	public Component styledTagWithTrailingSpace() {
		return Component.empty().append(styledTag()).append(" ");
	}

	public static Rank fromString(String s) {
		if (s == null) return null;
		String key = s.trim().toUpperCase(Locale.ROOT).replace('-', '_');
		for (Rank r : values()) {
			if (r.name().equals(key)) return r;
			if (r.id().equalsIgnoreCase(s)) return r;
			if (r.displayName.equalsIgnoreCase(s)) return r;
		}
		return null;
	}

	private static TextColor parseColor(String hex) {
		try {
			return TextColor.parseColor(hex).result().orElse(TextColor.fromRgb(0xAAAAAA));
		} catch (Exception e) {
			return TextColor.fromRgb(0xAAAAAA);
		}
	}
}

