package net.fabricmc.fabric.mixin.registry.sync;

import net.fabricmc.fabric.impl.registry.sync.CommandTreePacketAccess;

import net.fabricmc.fabric.impl.registry.sync.SyncFallbackUser;

import net.minecraft.network.Packet;
import net.minecraft.server.command.CommandManager;

import net.minecraft.server.network.ServerPlayNetworkHandler;

import net.minecraft.util.registry.Registry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CommandManager.class)
public class MixinCommandManager {
	@Redirect(method = "sendCommandTree", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"))
	private void attachSyncFallbackToPacket(ServerPlayNetworkHandler handler, Packet<?> packet) {
		((CommandTreePacketAccess) packet).setSyncFallbackArgumentTypeIds(((SyncFallbackUser) handler.player).getUsedSyncFallbacks(Registry.COMMAND_ARGUMENT_TYPE_KEY));
		handler.sendPacket(packet);
	}
}
