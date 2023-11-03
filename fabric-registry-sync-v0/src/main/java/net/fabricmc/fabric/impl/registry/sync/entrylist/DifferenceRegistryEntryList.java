package net.fabricmc.fabric.impl.registry.sync.entrylist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;

import net.fabricmc.fabric.mixin.registry.sync.RegistryEntryListListBackedAccessor;

import net.minecraft.registry.entry.RegistryEntryListCodec;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.util.dynamic.Codecs;

public class DifferenceRegistryEntryList<T> extends SequenceRegistryEntryList<T> {
	private final RegistryEntryList<T> base;
	protected DifferenceRegistryEntryList(RegistryEntryList<T> base, List<RegistryEntryList<T>> children) {
		super(children);
		this.base = base;
	}

	@Override
	protected List<RegistryEntry<T>> getEntries() {
		if (base instanceof RegistryEntryList.ListBacked<T> listBacked) {
			List<RegistryEntry<T>> baseEntries = ((RegistryEntryListListBackedAccessor) listBacked).callGetEntries();

			if (this.children.isEmpty()) return baseEntries;

			List<RegistryEntry<T>> copied = new ArrayList<>(baseEntries);
			this.children.forEach(entries -> entries.forEach(copied::remove));
			return Collections.unmodifiableList(copied);
		}

		return List.of();
	}

	/**
	 * Allocation-free override.
	 */
	@Override
	public boolean contains(RegistryEntry<T> entry) {
		if (!this.base.contains(entry)) return false;

		for (RegistryEntryList<T> child : this.children) {
			if (child.contains(entry)) return false;
		}

		return true;
	}

	@Override
	public boolean ownerEquals(RegistryEntryOwner<T> owner) {
		return this.base.ownerEquals(owner) && super.ownerEquals(owner);
	}

	public static <E> Codec<DifferenceRegistryEntryList<E>> createCodec(RegistryKey<? extends Registry<E>> registryRef, Codec<RegistryEntry<E>> entryCodec, boolean alwaysSerializeAsList) {
		return RecordCodecBuilder.create(instance -> instance.group(
				RegistryEntryListCodec.create(registryRef, entryCodec, alwaysSerializeAsList).fieldOf("base").forGetter(entryList -> entryList.base),
				Codecs.either(
						RegistryEntryListCodec.create(registryRef, entryCodec, alwaysSerializeAsList).listOf(),
						RegistryEntryListCodec.create(registryRef, entryCodec, alwaysSerializeAsList)
				)
						.xmap(either -> either.map(Function.identity(), Collections::singletonList), lists -> lists.size() == 1 ? Either.right(lists.get(0)) : Either.left(lists))
						.fieldOf("subtracted").forGetter(entryList -> entryList.children)
		).apply(instance, DifferenceRegistryEntryList::new));
	}
}
