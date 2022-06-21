/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.registry.sync;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.event.registry.RegistryEntryRemovedCallback;
import net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback;
import net.fabricmc.fabric.impl.registry.sync.ListenableRegistry;
import net.fabricmc.fabric.impl.registry.sync.RemapException;
import net.fabricmc.fabric.impl.registry.sync.RemapStateImpl;
import net.fabricmc.fabric.impl.registry.sync.RemappableRegistry;
import net.fabricmc.fabric.impl.registry.sync.SyncFallbackAccess;

@Mixin(SimpleRegistry.class)
public abstract class MixinIdRegistry<T> extends Registry<T> implements RemappableRegistry, ListenableRegistry<T>, SyncFallbackAccess {
	@Shadow
	@Final
	private ObjectList<RegistryEntry.Reference<T>> rawIdToEntry;
	@Shadow
	@Final
	private Object2IntMap<T> entryToRawId;
	@Shadow
	@Final
	private Map<Identifier, RegistryEntry.Reference<T>> idToEntry;
	@Shadow
	@Final
	private Map<RegistryKey<T>, RegistryEntry.Reference<T>> keyToEntry;
	@Shadow
	private int nextId;

	@Shadow
	public abstract Optional<RegistryKey<T>> getKey(T entry);

	@Shadow
	public abstract @Nullable T get(@Nullable Identifier id);

	@Unique
	private static Logger FABRIC_LOGGER = LoggerFactory.getLogger(MixinIdRegistry.class);

	public MixinIdRegistry(RegistryKey<? extends Registry<T>> key, Lifecycle lifecycle) {
		super(key, lifecycle);
	}

	@Unique
	private final Event<RegistryEntryAddedCallback<T>> fabric_addObjectEvent = EventFactory.createArrayBacked(RegistryEntryAddedCallback.class,
			(callbacks) -> (rawId, id, object) -> {
				for (RegistryEntryAddedCallback<T> callback : callbacks) {
					callback.onEntryAdded(rawId, id, object);
				}
			}
	);

	@Unique
	private final Event<RegistryEntryRemovedCallback<T>> fabric_removeObjectEvent = EventFactory.createArrayBacked(RegistryEntryRemovedCallback.class,
			(callbacks) -> (rawId, id, object) -> {
				for (RegistryEntryRemovedCallback<T> callback : callbacks) {
					callback.onEntryRemoved(rawId, id, object);
				}
			}
	);

	@Unique
	private final Event<RegistryIdRemapCallback<T>> fabric_postRemapEvent = EventFactory.createArrayBacked(RegistryIdRemapCallback.class,
			(callbacks) -> (a) -> {
				for (RegistryIdRemapCallback<T> callback : callbacks) {
					callback.onRemap(a);
				}
			}
	);

	@Unique
	private Object2IntMap<Identifier> fabric_prevIndexedEntries;
	@Unique
	private BiMap<Identifier, RegistryEntry.Reference<T>> fabric_prevEntries;

	@Override
	public Event<RegistryEntryAddedCallback<T>> fabric_getAddObjectEvent() {
		return fabric_addObjectEvent;
	}

	@Override
	public Event<RegistryEntryRemovedCallback<T>> fabric_getRemoveObjectEvent() {
		return fabric_removeObjectEvent;
	}

	@Override
	public Event<RegistryIdRemapCallback<T>> fabric_getRemapEvent() {
		return fabric_postRemapEvent;
	}

	// The rest of the registry isn't thread-safe, so this one need not be either.
	@Unique
	private boolean fabric_isObjectNew = false;

	@Unique
	private final Map<Identifier, Identifier> syncFallbacks = new Object2ObjectOpenHashMap<>();

	@Unique
	private final Set<Identifier> syncFallbackAttempted = new ObjectOpenHashSet<>();

	@Unique
	private final Set<Identifier> syncFallbackUnsupported = new ObjectOpenHashSet<>();

	@Inject(method = "set(ILnet/minecraft/util/registry/RegistryKey;Ljava/lang/Object;Lcom/mojang/serialization/Lifecycle;Z)Lnet/minecraft/util/registry/RegistryEntry;", at = @At("HEAD"))
	public void setPre(int id, RegistryKey<T> registryId, T object, Lifecycle lifecycle, boolean checkDuplicateKeys, CallbackInfoReturnable<T> info) {
		int indexedEntriesId = entryToRawId.getInt(object);

		if (indexedEntriesId >= 0) {
			throw new RuntimeException("Attempted to register object " + object + " twice! (at raw IDs " + indexedEntriesId + " and " + id + " )");
		}

		if (!idToEntry.containsKey(registryId.getValue())) {
			fabric_isObjectNew = true;
		} else {
			RegistryEntry.Reference<T> oldObject = idToEntry.get(registryId.getValue());

			if (oldObject != null && oldObject != object) {
				int oldId = entryToRawId.getInt(oldObject);

				if (oldId != id && checkDuplicateKeys) {
					throw new RuntimeException("Attempted to register ID " + registryId + " at different raw IDs (" + oldId + ", " + id + ")! If you're trying to override an item, use .set(), not .register()!");
				}

				fabric_removeObjectEvent.invoker().onEntryRemoved(oldId, registryId.getValue(), oldObject.value());
				fabric_isObjectNew = true;
			} else {
				fabric_isObjectNew = false;
			}
		}
	}

	@Inject(method = "set(ILnet/minecraft/util/registry/RegistryKey;Ljava/lang/Object;Lcom/mojang/serialization/Lifecycle;Z)Lnet/minecraft/util/registry/RegistryEntry;", at = @At("RETURN"))
	public void setPost(int id, RegistryKey<T> registryId, T object, Lifecycle lifecycle, boolean checkDuplicateKeys, CallbackInfoReturnable<T> info) {
		if (fabric_isObjectNew) {
			fabric_addObjectEvent.invoker().onEntryAdded(id, registryId.getValue(), object);
			if (!RegistrySyncManager.VANILLA_NAMESPACES.contains(registryId.getValue().getNamespace())) {
				this.syncFallbackUnsupported.add(registryId.getValue());
			}
		}
	}

	@Override
	public void remap(String name, Object2IntMap<Identifier> remoteIndexedEntries, RemapMode mode) throws RemapException {
		// Throw on invalid conditions.
		switch (mode) {
		case AUTHORITATIVE:
			break;
		case REMOTE: {
			List<String> strings = null;

			for (Identifier remoteId : remoteIndexedEntries.keySet()) {
				if (!idToEntry.containsKey(remoteId) && !SyncFallbackAccess.canUseSyncFallback(this)) {
					if (strings == null) {
						strings = new ArrayList<>();
					}

					strings.add(" - " + remoteId);
				}
			}

			if (strings != null) {
				StringBuilder builder = new StringBuilder("Received ID map for " + name + " contains IDs unknown to the receiver!");

				for (String s : strings) {
					builder.append('\n').append(s);
				}

				throw new RemapException(builder.toString());
			}

			break;
		}
		case EXACT: {
			if (!idToEntry.keySet().equals(remoteIndexedEntries.keySet())) {
				List<String> strings = new ArrayList<>();

				for (Identifier remoteId : remoteIndexedEntries.keySet()) {
					if (!idToEntry.containsKey(remoteId)) {
						strings.add(" - " + remoteId + " (missing on local)");
					}
				}

				for (Identifier localId : getIds()) {
					if (!remoteIndexedEntries.containsKey(localId)) {
						strings.add(" - " + localId + " (missing on remote)");
					}
				}

				StringBuilder builder = new StringBuilder("Local and remote ID sets for " + name + " do not match!");

				for (String s : strings) {
					builder.append('\n').append(s);
				}

				throw new RemapException(builder.toString());
			}

			break;
		}
		}

		// Make a copy of the previous maps.
		// For now, only one is necessary - on an integrated server scenario,
		// AUTHORITATIVE == CLIENT, which is fine.
		// The reason we preserve the first one is because it contains the
		// vanilla order of IDs before mods, which is crucial for vanilla server
		// compatibility.
		if (fabric_prevIndexedEntries == null) {
			fabric_prevIndexedEntries = new Object2IntOpenHashMap<>();
			fabric_prevEntries = HashBiMap.create(idToEntry);

			for (T o : this) {
				fabric_prevIndexedEntries.put(getId(o), getRawId(o));
			}
		}

		Int2ObjectMap<Identifier> oldIdMap = new Int2ObjectOpenHashMap<>();

		for (T o : this) {
			oldIdMap.put(getRawId(o), getId(o));
		}

		// If we're AUTHORITATIVE, we append entries which only exist on the
		// local side to the new entry list. For REMOTE, we instead drop them.
		switch (mode) {
		case AUTHORITATIVE: {
			int maxValue = 0;

			Object2IntMap<Identifier> oldRemoteIndexedEntries = remoteIndexedEntries;
			remoteIndexedEntries = new Object2IntOpenHashMap<>();

			for (Identifier id : oldRemoteIndexedEntries.keySet()) {
				int v = oldRemoteIndexedEntries.getInt(id);
				remoteIndexedEntries.put(id, v);
				if (v > maxValue) maxValue = v;
			}

			for (Identifier id : getIds()) {
				if (!remoteIndexedEntries.containsKey(id)) {
					FABRIC_LOGGER.warn("Adding " + id + " to saved/remote registry.");
					remoteIndexedEntries.put(id, ++maxValue);
				}
			}

			break;
		}
		case REMOTE: {
			int maxId = -1;

			for (Identifier id : getIds()) {
				if (!remoteIndexedEntries.containsKey(id)) {
					if (maxId < 0) {
						for (int value : remoteIndexedEntries.values()) {
							if (value > maxId) {
								maxId = value;
							}
						}
					}

					if (maxId < 0) {
						throw new RemapException("Failed to assign new id to client only registry entry");
					}

					maxId++;

					FABRIC_LOGGER.debug("An ID for {} was not sent by the server, assuming client only registry entry and assigning a new id ({}) in {}", id.toString(), maxId, getKey().getValue().toString());
					remoteIndexedEntries.put(id, maxId);
				}
			}

			break;
		}
		}

		Int2IntMap idMap = new Int2IntOpenHashMap();

		for (int i = 0; i < rawIdToEntry.size(); i++) {
			RegistryEntry.Reference<T> reference = rawIdToEntry.get(i);

			// Unused id, skip
			if (reference == null) continue;

			Identifier id = reference.registryKey().getValue();

			// see above note
			if (remoteIndexedEntries.containsKey(id)) {
				idMap.put(i, remoteIndexedEntries.getInt(id));
			}
		}

		// entries was handled above, if it was necessary.
		rawIdToEntry.clear();
		entryToRawId.clear();
		syncFallbackAttempted.clear();
		nextId = 0;

		List<Identifier> orderedRemoteEntries = new ArrayList<>(remoteIndexedEntries.keySet());
		orderedRemoteEntries.sort(Comparator.comparingInt(remoteIndexedEntries::getInt));

		for (Identifier identifier : orderedRemoteEntries) {
			int id = remoteIndexedEntries.getInt(identifier);
			RegistryEntry.Reference<T> object = idToEntry.get(identifier);

			// Warn if an object is missing from the local registry.
			// This should only happen in AUTHORITATIVE mode, and as such we
			// throw an exception otherwise.
			if (object == null) {
				if (mode == RemapMode.REMOTE && SyncFallbackAccess.canUseSyncFallback(this)) {
					FABRIC_LOGGER.warn(identifier + " missing from registry, but requested! Attempting to use sync fallback.");

					// We are making a hole in the registry.
					if (nextId <= id) {
						nextId = id + 1;
					}

					syncFallbackAttempted.add(identifier);
					continue;
				} else if (mode != RemapMode.AUTHORITATIVE) {
					throw new RemapException(identifier + " missing from registry, but requested!");
				} else {
					FABRIC_LOGGER.warn(identifier + " missing from registry, but requested!");
				}

				continue;
			}

			// Add the new object, increment nextId to match.
			rawIdToEntry.size(Math.max(this.rawIdToEntry.size(), id + 1));
			rawIdToEntry.set(id, object);
			entryToRawId.put(object.value(), id);

			if (nextId <= id) {
				nextId = id + 1;
			}
		}

		fabric_getRemapEvent().invoker().onRemap(new RemapStateImpl<>(this, oldIdMap, idMap));
	}

	@Override
	public void unmap(String name) throws RemapException {
		if (fabric_prevIndexedEntries != null) {
			List<Identifier> addedIds = new ArrayList<>();

			// Emit AddObject events for previously culled objects.
			for (Identifier id : fabric_prevEntries.keySet()) {
				if (!idToEntry.containsKey(id)) {
					assert fabric_prevIndexedEntries.containsKey(id);
					addedIds.add(id);
				}
			}

			idToEntry.clear();
			keyToEntry.clear();

			idToEntry.putAll(fabric_prevEntries);

			for (Map.Entry<Identifier, RegistryEntry.Reference<T>> entry : fabric_prevEntries.entrySet()) {
				RegistryKey<T> entryKey = RegistryKey.of(getKey(), entry.getKey());
				keyToEntry.put(entryKey, entry.getValue());
			}

			remap(name, fabric_prevIndexedEntries, RemapMode.AUTHORITATIVE);

			for (Identifier id : addedIds) {
				fabric_getAddObjectEvent().invoker().onEntryAdded(entryToRawId.getInt(idToEntry.get(id)), id, get(id));
			}

			fabric_prevIndexedEntries = null;
			fabric_prevEntries = null;
		}
	}

	@Override
	public void registerSyncFallback(Identifier key, Identifier fallback) {
		if (!SyncFallbackAccess.canUseSyncFallback(this)) {
			throw new IllegalStateException("Cannot register sync fallback with this registry!");
		}

		syncFallbacks.put(key, fallback);
		syncFallbackUnsupported.remove(key);
	}

	@Override
	@Nullable
	public Identifier getSyncFallback(Identifier id) {
		return syncFallbacks.get(id);
	}

	@Override
	public Set<Identifier> getSyncFallbackUnsupported() {
		return this.syncFallbackUnsupported;
	}

	@Override
	public Set<Identifier> getSyncFallbackSupported() {
		return syncFallbacks.keySet();
	}
	@Override
	public Set<Identifier> getSyncFallbackAttemptedIds() {
		return this.syncFallbackAttempted;
	}
}
