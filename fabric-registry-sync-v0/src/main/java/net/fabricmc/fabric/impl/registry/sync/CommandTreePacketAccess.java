package net.fabricmc.fabric.impl.registry.sync;

import net.minecraft.util.Identifier;

import java.util.Set;

public interface CommandTreePacketAccess {
	void setSyncFallbackArgumentTypeIds(Set<Identifier> syncFallbackArgumentTypeIds);
}
