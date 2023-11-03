package net.fabricmc.fabric.impl.registry.sync.entrylist;

import java.util.LinkedHashSet;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;

import net.fabricmc.fabric.mixin.registry.sync.RegistryEntryListListBackedAccessor;

import net.minecraft.registry.entry.RegistryEntryListCodec;

public class AndRegistryEntryList<T> extends SequenceRegistryEntryList<T> {
	public AndRegistryEntryList(List<RegistryEntryList<T>> children) {
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
					set.retainAll(entries);
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
			if (!child.contains(entry)) return false;
		}

		// Return false on empty children
		return !this.children.isEmpty();
	}

	public static <E> Codec<AndRegistryEntryList<E>> createCodec(RegistryKey<? extends Registry<E>> registryRef, Codec<RegistryEntry<E>> entryCodec, boolean alwaysSerializeAsList) {
		return RegistryEntryListCodec.create(registryRef, entryCodec, alwaysSerializeAsList).listOf().fieldOf("values").xmap(AndRegistryEntryList::new, list -> list.children).codec();
	}
}
