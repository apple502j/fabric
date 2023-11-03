package net.fabricmc.fabric.impl.registry.sync.entrylist;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;

import net.fabricmc.fabric.mixin.registry.sync.RegistryEntryListListBackedAccessor;

import net.minecraft.registry.entry.RegistryEntryListCodec;

public class OrRegistryEntryList<T> extends SequenceRegistryEntryList<T> {
	protected OrRegistryEntryList(List<RegistryEntryList<T>> children) {
		super(children);
	}

	@Override
	protected List<RegistryEntry<T>> getEntries() {
		LinkedHashSet<RegistryEntry<T>> set = null;

		for (RegistryEntryList<T> child : this.children) {
			if (child instanceof RegistryEntryList.ListBacked<T> listBacked) {
				List<RegistryEntry<T>> entries = ((RegistryEntryListListBackedAccessor<T>) listBacked).callGetEntries();

				if (set == null) {
					set = new LinkedHashSet<>(entries);
				} else {
					set.addAll(entries);
				}
			}
		}

		return set == null ? List.of() : List.copyOf(set);
	}

	/**
	 * Allocation-free override.
	 */
	@Override
	public boolean contains(RegistryEntry<T> entry) {
		for (RegistryEntryList<T> child : this.children) {
			if (child.contains(entry)) return true;
		}

		return false;
	}

	@Override
	public void forEach(Consumer<? super RegistryEntry<T>> action) {
		if (this.children.isEmpty()) {
			return;
		} else if (this.children.size() == 1) {
			this.children.get(0).forEach(action);
			return;
		}

		final Set<RegistryEntry<T>> visited = new HashSet<>();
		Consumer<? super RegistryEntry<T>> wrapped = (entry) -> {
			if (visited.add(entry)) action.accept(entry);
		};

		for (RegistryEntryList<T> child : this.children) {
			child.forEach(wrapped);
		}
	}

	public static <E> Codec<OrRegistryEntryList<E>> createCodec(RegistryKey<? extends Registry<E>> registryRef, Codec<RegistryEntry<E>> entryCodec, boolean alwaysSerializeAsList) {
		return RegistryEntryListCodec.create(registryRef, entryCodec, alwaysSerializeAsList).listOf().fieldOf("values").xmap(OrRegistryEntryList::new, list -> list.children).codec();
	}
}
