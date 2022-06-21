package net.fabricmc.fabric.mixin.registry.sync;

import java.util.Set;

import net.fabricmc.fabric.impl.registry.sync.PacketByteBufAttachment;

import net.minecraft.network.PacketByteBuf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.impl.registry.sync.CommandTreePacketAccess;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandTreeS2CPacket.class)
public class MixinCommandTreeS2CPacket implements CommandTreePacketAccess {
	@Unique
	private Set<Identifier> syncFallbackArgumentTypeIds;

	@Override
	public void setSyncFallbackArgumentTypeIds(Set<Identifier> syncFallbackArgumentTypeIds) {
		this.syncFallbackArgumentTypeIds = syncFallbackArgumentTypeIds;
	}

	@Inject(method = "write", at = @At("HEAD"))
	private void attachContextToBuf(PacketByteBuf buf, CallbackInfo ci) {
		((PacketByteBufAttachment) buf).setSyncFallbackArgumentTypeIds(syncFallbackArgumentTypeIds);
	}

	@Inject(method = "write", at = @At("TAIL"))
	private void detachContextFromBuf(PacketByteBuf buf, CallbackInfo ci) {
		((PacketByteBufAttachment) buf).setSyncFallbackArgumentTypeIds(null);
	}
}
