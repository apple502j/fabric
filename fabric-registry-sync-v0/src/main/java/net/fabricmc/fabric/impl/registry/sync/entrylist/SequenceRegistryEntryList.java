package net.fabricmc.fabric.impl.registry.sync.entrylist;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.registry.tag.TagKey;

public abstract class SequenceRegistryEntryList<T> extends RegistryEntryList.ListBacked<T> {
	protected List<RegistryEntryList<T>> children;

	protected SequenceRegistryEntryList(List<RegistryEntryList<T>> children) {
		super();
		this.children = children;
	}

	@Override
	public Either<TagKey<T>, List<RegistryEntry<T>>> getStorage() {
		return Either.right(getEntries());
	}

	@Override
	public boolean contains(RegistryEntry<T> entry) {
		return getEntries().contains(entry);
	}

	@Override
	public Optional<TagKey<T>> getTagKey() {
		return Optional.empty();
	}

	@Override
	public boolean ownerEquals(RegistryEntryOwner<T> owner) {
		for (RegistryEntryList<T> child : this.children) {
			if (!child.ownerEquals(owner)) return false;
		}

		return true;
	}
}
