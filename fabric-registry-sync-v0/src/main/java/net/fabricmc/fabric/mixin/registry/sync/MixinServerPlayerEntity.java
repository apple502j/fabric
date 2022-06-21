package net.fabricmc.fabric.mixin.registry.sync;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;

import net.minecraft.util.registry.RegistryKey;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import net.fabricmc.fabric.impl.registry.sync.SyncFallbackUser;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements SyncFallbackUser {
	@Unique
	private Multimap<Identifier, Identifier> syncFallbacks;

	@Override
	public void setUsedSyncFallbacks(Multimap<Identifier, Identifier> syncFallbacks) {
		this.syncFallbacks = syncFallbacks;
	}

	@Override
	public Set<Identifier> getUsedSyncFallbacks(RegistryKey<? extends Registry<?>> registry) {
		return new HashSet<>(this.syncFallbacks.get(registry.getValue()));
	}
}
