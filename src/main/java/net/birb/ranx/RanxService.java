package net.birb.ranx;

import com.mojang.authlib.GameProfile;
import net.birb.ranx.rank.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class RanxService {
	private RanxService() {}

	private static volatile RanxData data = null;

	public static void onServerStarted(MinecraftServer server) {
		data = RanxStorage.loadOrCreateDefaults();
		ensureTeams(server.getScoreboard());
	}

	public static void onServerStopping() {
		if (data != null) RanxStorage.save(data);
	}

	public static void onPlayerJoin(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) return;

		Rank rank = getRank(player);

		// If player has no explicit saved rank beyond default PLAYER, allow default hardcoded-by-name.
		if (rank == Rank.PLAYER) {
			String byName = safeData().defaultNameRanks.get(normalizeName(player.getGameProfile()));
			Rank hardcoded = Rank.fromString(byName);
			if (hardcoded != null && hardcoded != Rank.PLAYER) setRank(server, player, hardcoded);
		}

		applyRankTeam(server.getScoreboard(), player, rank);
	}

	public static Rank getRank(ServerPlayer player) {
		String id = safeData().ranksByUuid.get(player.getUUID().toString());
		Rank r = Rank.fromString(id);
		return r == null ? Rank.PLAYER : r;
	}

	public static void setRank(MinecraftServer server, ServerPlayer target, Rank rank) {
		safeData().ranksByUuid.put(target.getUUID().toString(), rank.id());
		RanxStorage.save(safeData());
		applyRankTeam(server.getScoreboard(), target, rank);
	}

	public static boolean isMuted(ServerPlayer player) {
		return safeData().mutedUuids.contains(player.getUUID().toString());
	}

	public static void setMuted(MinecraftServer server, ServerPlayer target, boolean muted) {
		if (muted) safeData().mutedUuids.add(target.getUUID().toString());
		else safeData().mutedUuids.remove(target.getUUID().toString());
		RanxStorage.save(safeData());
	}

	public static Component formatNameWithRank(ServerPlayer player) {
		Rank rank = getRank(player);
		return Component.empty().append(rank.styledTagWithTrailingSpace()).append(player.getName());
	}

	private static void ensureTeams(Scoreboard scoreboard) {
		for (Rank rank : Rank.values()) {
			String teamName = teamName(rank);
			PlayerTeam team = scoreboard.getPlayerTeam(teamName);
			if (team == null) team = scoreboard.addPlayerTeam(teamName);

			team.setPlayerPrefix(rank.styledTagWithTrailingSpace());

			// Also set the team color (used for name coloring in some UIs).
			TextColor color = safeParse(rank);
			team.setColor(colorToNearestFormatting(color));
		}
	}

	private static void applyRankTeam(Scoreboard scoreboard, ServerPlayer player, Rank rank) {
		ensureTeams(scoreboard);

		// 26.1 scoreboard is strict: removing a player who isn't on a team throws.
		// Only remove from the team the player is currently on, if it’s one of ours.
		PlayerTeam current = scoreboard.getPlayersTeam(player.getScoreboardName());
		if (current != null && current.getName().startsWith("ranx_")) {
			scoreboard.removePlayerFromTeam(player.getScoreboardName(), current);
		}

		PlayerTeam targetTeam = scoreboard.getPlayerTeam(teamName(rank));
		if (targetTeam != null) scoreboard.addPlayerToTeam(player.getScoreboardName(), targetTeam);
	}

	private static String teamName(Rank rank) {
		return "ranx_" + rank.id();
	}

	private static String normalizeName(GameProfile profile) {
		return Optional.ofNullable(profile.name()).orElse("").trim().toLowerCase(Locale.ROOT);
	}

	private static TextColor safeParse(Rank rank) {
		try {
			// Parse from the styled tag’s style, keeping source-of-truth in Rank.
			Style s = rank.styledTag().getStyle();
			TextColor c = s.getColor();
			return c != null ? c : TextColor.fromRgb(0xAAAAAA);
		} catch (Exception e) {
			return TextColor.fromRgb(0xAAAAAA);
		}
	}

	/**
	 * Scoreboard Team API still uses vanilla Formatting for its "color", not hex.
	 * We still keep hex for the actual prefix text color; this just approximates for places that only read team color.
	 */
	private static ChatFormatting colorToNearestFormatting(TextColor color) {
		int rgb = color.getValue();
		// Extremely simple approximation. You can refine later.
		if (rgb == 0xA020F0) return ChatFormatting.DARK_PURPLE;
		if (rgb == 0xFF5555) return ChatFormatting.RED;
		if (rgb == 0x55FFFF) return ChatFormatting.AQUA;
		if (rgb == 0x55FF55) return ChatFormatting.GREEN;
		return ChatFormatting.GRAY;
	}

	private static RanxData safeData() {
		RanxData d = data;
		if (d != null) return d;
		d = RanxStorage.loadOrCreateDefaults();
		data = d;
		return d;
	}
}

