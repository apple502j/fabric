package net.fabricmc.fabric.impl.registry.sync;

import com.google.common.collect.Multimap;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.Set;

public interface SyncFallbackUser {
	Set<Identifier> getUsedSyncFallbacks(RegistryKey<? extends Registry<?>> registry);

	void setUsedSyncFallbacks(Multimap<Identifier, Identifier> syncFallbacks);
}
