package net.fabricmc.fabric.impl.registry.sync;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

public interface PacketByteBufAttachment {
	@Nullable
	Set<Identifier> getSyncFallbackArgumentTypeIds();

	void setSyncFallbackArgumentTypeIds(@Nullable Set<Identifier> syncFallbackArgumentTypeIds);
}
