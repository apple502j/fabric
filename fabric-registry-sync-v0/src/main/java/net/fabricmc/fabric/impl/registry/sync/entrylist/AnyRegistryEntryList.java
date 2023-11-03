package net.fabricmc.fabric.impl.registry.sync.entrylist;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.registry.RegistryEntryLookup;

import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;

/**
 * RegistryWrapper has no access to the parent Registry.
 * This causes several enumeration operations to be slower than expected (O(n) instead of O(1)).
 */
public record AnyRegistryEntryList<T>(RegistryWrapper<T> wrapper) implements RegistryEntryList<T> {
	@Override
	public Stream<RegistryEntry<T>> stream() {
		// Function.identity() is for casting.
		return this.wrapper.streamEntries().map(Function.identity());
	}

	@Override
	public int size() {
		return (int) this.wrapper.streamKeys().count();
	}

	@Override
	public Either<TagKey<T>, List<RegistryEntry<T>>> getStorage() {
		return Either.right(this.stream().toList());
	}

	@Override
	public Optional<RegistryEntry<T>> getRandom(Random random) {
		return Util.getRandomOrEmpty(this.stream().toList(), random);
	}

	@Override
	public RegistryEntry<T> get(int index) {
		// index 0 = skip 0, get next, index 1 = skip 1, get next...
		try {
			return this.stream().skip(index).iterator().next();
		} catch (RuntimeException e) {
			// Give more detailed error message
			throw (RuntimeException) new IndexOutOfBoundsException(index).initCause(e);
		}
	}

	@Override
	public boolean contains(RegistryEntry<T> entry) {
		return true;
	}

	@Override
	public boolean ownerEquals(RegistryEntryOwner<T> owner) {
		return true;
	}

	@Override
	public Optional<TagKey<T>> getTagKey() {
		return Optional.empty();
	}

	@NotNull
	@Override
	public Iterator<RegistryEntry<T>> iterator() {
		return stream().iterator();
	}

	public static <E, O> MapCodec<RegistryWrapper<E>> getEntryWrapperCodec(RegistryKey<? extends Registry<? extends E>> registryRef) {
		return Codecs.createContextRetrievalCodec((ops) -> {
			if (ops instanceof RegistryOps<?> registryOps) {
				Optional<RegistryEntryLookup<E>> lookup = registryOps.getEntryLookup(registryRef);

				if (lookup.isEmpty()) {
					return DataResult.error(() -> "Lookup not found for: " + registryRef);
				} else if (lookup.get() instanceof RegistryWrapper<E> wrapper) {
					return DataResult.success(wrapper);
				} else {
					return DataResult.error(() -> "RegistryEntryLookup was not a RegistryWrapper");
				}
			} else {
				return DataResult.error(() -> "Not a registry ops");
			}
		});
	}

	public static <E> Codec<AnyRegistryEntryList<E>> createCodec(RegistryKey<? extends Registry<E>> registryRef) {
		return RecordCodecBuilder.create(instance -> instance.group(
				getEntryWrapperCodec(registryRef).fieldOf("registry").forGetter(o -> null)
		).apply(instance, AnyRegistryEntryList::new));
	}
}
