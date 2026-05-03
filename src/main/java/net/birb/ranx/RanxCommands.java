package net.birb.ranx;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.birb.ranx.rank.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Arrays;

public final class RanxCommands {
	private RanxCommands() {}

	private static final SuggestionProvider<CommandSourceStack> RANK_SUGGESTIONS =
		(context, builder) -> SharedSuggestionProvider.suggest(
			Arrays.stream(Rank.values()).map(Rank::id).toList(),
			builder
		);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("rank")
			.requires(RanxCommands::canChangeRanks)
			.then(Commands.argument("player", EntityArgument.player())
				.then(Commands.argument("rank", StringArgumentType.word())
					.suggests(RANK_SUGGESTIONS)
					.executes(ctx -> {
						ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
						String rankStr = StringArgumentType.getString(ctx, "rank");
						Rank rank = Rank.fromString(rankStr);
						if (rank == null) {
							ctx.getSource().sendFailure(Component.literal("Unknown rank '" + rankStr + "'."));
							return 0;
						}

						MinecraftServer server = ctx.getSource().getServer();
						RanxService.setRank(server, target, rank);
						ctx.getSource().sendSuccess(() ->
							Component.literal("Set ")
								.append(target.getName())
								.append(" to ")
								.append(rank.styledTag())
								.withStyle(ChatFormatting.GRAY),
							true);
						return 1;
					})
				)
			)
		);

		dispatcher.register(Commands.literal("mute")
			.requires(RanxCommands::canModerate)
			.then(Commands.argument("player", EntityArgument.player())
				.executes(ctx -> {
					ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
					MinecraftServer server = ctx.getSource().getServer();
					boolean next = !RanxService.isMuted(target);
					RanxService.setMuted(server, target, next);
					ctx.getSource().sendSuccess(() ->
						Component.literal(next ? "Muted " : "Unmuted ").append(target.getName()),
						true);
					return 1;
				})
			)
		);

		dispatcher.register(Commands.literal("kick")
			.requires(RanxCommands::canModerate)
			.then(Commands.argument("player", EntityArgument.player())
				.then(Commands.argument("reason", StringArgumentType.greedyString())
					.executes(ctx -> kick(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
						StringArgumentType.getString(ctx, "reason")))
				)
				.executes(ctx -> kick(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "Kicked."))
			)
		);

		dispatcher.register(Commands.literal("ban")
			.requires(RanxCommands::canModerate)
			.then(Commands.argument("player", EntityArgument.player())
				.then(Commands.argument("reason", StringArgumentType.greedyString())
					.executes(ctx -> ban(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
						StringArgumentType.getString(ctx, "reason"), false)))
				.executes(ctx -> ban(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "Banned.", false))
			)
		);

		dispatcher.register(Commands.literal("ip-ban")
			.requires(RanxCommands::canModerate)
			.then(Commands.argument("player", EntityArgument.player())
				.then(Commands.argument("reason", StringArgumentType.greedyString())
					.executes(ctx -> ban(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
						StringArgumentType.getString(ctx, "reason"), true)))
				.executes(ctx -> ban(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "IP-banned.", true))
			)
		);
	}

	private static boolean canChangeRanks(CommandSourceStack source) {
		if (isOwnerPerm(source)) return true;
		ServerPlayer p = source.getPlayer();
		if (p == null) return false; // console can't unless permission(4)
		return RanxService.getRank(p).canChangeRanks();
	}

	private static boolean canModerate(CommandSourceStack source) {
		if (isOwnerPerm(source)) return true;
		ServerPlayer p = source.getPlayer();
		if (p == null) return false;
		return RanxService.getRank(p).canModerate();
	}

	private static int kick(CommandSourceStack source, ServerPlayer target, String reason) {
		ServerGamePacketListenerImpl conn = target.connection;
		conn.disconnect(Component.literal(reason));
		source.sendSuccess(() -> Component.literal("Kicked ").append(target.getName()), true);
		return 1;
	}

	private static int ban(CommandSourceStack source, ServerPlayer target, String reason, boolean ipBan) {
		MinecraftServer server = source.getServer();

		if (ipBan) {
			String addr = target.getIpAddress();
			if (addr == null || addr.isBlank()) {
				source.sendFailure(Component.literal("Could not determine player IP for IP-ban."));
				return 0;
			}
			IpBanList list = server.getPlayerList().getIpBans();
			list.add(new IpBanListEntry(addr, null, source.getTextName(), null, reason));
			target.connection.disconnect(Component.literal(reason));
			source.sendSuccess(() -> Component.literal("IP-banned ").append(target.getName()).append(" (" + addr + ")"), true);
			return 1;
		} else {
			UserBanList list = server.getPlayerList().getBans();
			list.add(new UserBanListEntry(new NameAndId(target.getGameProfile()), null, source.getTextName(), null, reason));
			target.connection.disconnect(Component.literal(reason));
			source.sendSuccess(() -> Component.literal("Banned ").append(target.getName()), true);
			return 1;
		}
	}

	private static boolean isOwnerPerm(CommandSourceStack source) {
		// In 26.1+, command permission checks are based on PermissionSet.
		return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS));
	}
}

