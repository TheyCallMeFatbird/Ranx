package net.birb.ranx;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ranx implements ModInitializer {
	public static final String MOD_ID = "ranx";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> RanxService.onServerStarted(server));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> RanxService.onServerStopping());
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> RanxService.onPlayerJoin(handler.player));

		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
			if (RanxService.isMuted(sender)) {
				sender.sendSystemMessage(Component.literal("You are muted.").withStyle(ChatFormatting.RED));
				return false;
			}
			return true;
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			RanxCommands.register(dispatcher);
		});

		LOGGER.info("Ranx initialized.");
	}
}