package net.fabricmc.fabric.mixin.registry.sync;

import net.fabricmc.fabric.impl.registry.sync.PacketByteBufAttachment;

import net.fabricmc.fabric.impl.registry.sync.SyncFallbackAccess;

import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(targets = "net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket$ArgumentNode")
public class MixinArgumentNode {
	@Redirect(method = "write(Lnet/minecraft/network/PacketByteBuf;Lnet/minecraft/command/argument/serialize/ArgumentSerializer;Lnet/minecraft/command/argument/serialize/ArgumentSerializer$ArgumentTypeProperties;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/registry/Registry;getRawId(Ljava/lang/Object;)I"))
	private static int useRawIdOfSyncFallback(Registry instance, @Coerce Object coerced, PacketByteBuf buf) {
		ArgumentSerializer<?, ?> serializer = (ArgumentSerializer<?, ?>) coerced;
		Registry<ArgumentSerializer<?, ?>> registry = Registry.COMMAND_ARGUMENT_TYPE;
		Identifier serializerId = registry.getId(serializer);
		Set<Identifier> syncFallbackIds;
		if ((syncFallbackIds = ((PacketByteBufAttachment) buf).getSyncFallbackArgumentTypeIds()) != null && syncFallbackIds.contains(serializerId)) {
			return registry.getRawId(registry.get(((SyncFallbackAccess) registry).getSyncFallback(serializerId)));
		}
		return registry.getRawId(serializer);
	}
}
