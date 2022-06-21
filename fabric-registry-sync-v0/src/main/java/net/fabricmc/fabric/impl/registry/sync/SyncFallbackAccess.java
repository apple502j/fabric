package net.fabricmc.fabric.impl.registry.sync;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface SyncFallbackAccess {
	Set<RegistryKey<? extends Registry<?>>> SYNC_FALLBACK_USABLE = Set.of(Registry.COMMAND_ARGUMENT_TYPE_KEY);
	void registerSyncFallback(Identifier key, Identifier fallback);

	@Nullable
	Identifier getSyncFallback(Identifier id);
	static boolean canUseSyncFallback(Registry<?> registry) {
		return SYNC_FALLBACK_USABLE.contains(registry.getKey());
	}
	Set<Identifier> getSyncFallbackUnsupported();
	Set<Identifier> getSyncFallbackSupported();
}
