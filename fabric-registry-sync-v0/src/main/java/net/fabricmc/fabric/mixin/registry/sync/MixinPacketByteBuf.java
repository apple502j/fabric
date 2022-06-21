package net.fabricmc.fabric.mixin.registry.sync;

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.impl.registry.sync.PacketByteBufAttachment;

@Mixin(PacketByteBuf.class)
public class MixinPacketByteBuf implements PacketByteBufAttachment {
	@Unique
	@Nullable
	Set<Identifier> syncFallbackArgumentTypeIds;

	@Override
	@Nullable
	public Set<Identifier> getSyncFallbackArgumentTypeIds() {
		return syncFallbackArgumentTypeIds;
	}

	@Override
	public void setSyncFallbackArgumentTypeIds(@Nullable Set<Identifier> syncFallbackArgumentTypeIds) {
		this.syncFallbackArgumentTypeIds = syncFallbackArgumentTypeIds;
	}
}
