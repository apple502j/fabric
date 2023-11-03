package net.fabricmc.fabric.mixin.registry.sync;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(RegistryEntryList.ListBacked.class)
public interface RegistryEntryListListBackedAccessor<T> {
	@Invoker
	List<RegistryEntry<T>> callGetEntries();
}
