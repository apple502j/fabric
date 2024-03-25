package net.fabricmc.fabric.api.resource.conditions.v1;

import net.fabricmc.fabric.api.resource.conditions.v1.conditions.AllModsLoadedResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.conditions.AndResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.conditions.AnyModsLoadedResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.conditions.FeaturesEnabledResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.conditions.NotResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.conditions.OrResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.conditions.RegistryContainsResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.conditions.TagsPopulatedResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.conditions.TrueResourceCondition;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceConditions {
	private static final Map<Identifier, ResourceConditionType<?>> REGISTERED_CONDITIONS = new ConcurrentHashMap<>();

	public static final String CONDITIONS_KEY = "fabric:load_conditions";


	public static void register(ResourceConditionType<?> condition) {
		Objects.requireNonNull(condition, "Condition may not be null.");

		if (REGISTERED_CONDITIONS.put(condition.id(), condition) != null) {
			throw new IllegalArgumentException("Duplicate resource condition registered with id " + condition.id());
		}
	}

	public static ResourceConditionType<?> getConditionType(Identifier id) {
		return REGISTERED_CONDITIONS.get(id);
	}

	public static ResourceCondition alwaysTrue() {
		return new TrueResourceCondition();
	}

	public static ResourceCondition not(ResourceCondition condition) {
		return new NotResourceCondition(condition);
	}

	public static ResourceCondition and(ResourceCondition... conditions) {
		return new AndResourceCondition(List.of(conditions));
	}

	public static ResourceCondition or(ResourceCondition... conditions) {
		return new OrResourceCondition(List.of(conditions));
	}

	public static ResourceCondition allModsLoaded(String... modIds) {
		return new AllModsLoadedResourceCondition(List.of(modIds));
	}

	public static ResourceCondition anyModsLoaded(String... modIds) {
		return new AnyModsLoadedResourceCondition(List.of(modIds));
	}

	@SafeVarargs
	public static <T> ResourceCondition tagsPopulated(Identifier registry, TagKey<T>... tags) {
		return new TagsPopulatedResourceCondition(registry, tags);
	}

	public static ResourceCondition featuresEnabled(Identifier... features) {
		return new FeaturesEnabledResourceCondition(features);
	}

	public static <T> ResourceCondition registryContains(RegistryKey<T> entries) {
		return new RegistryContainsResourceCondition(entries);
	}

	public static ResourceCondition registryContains(Identifier registry, Identifier... entries) {
		return new RegistryContainsResourceCondition(registry, entries);
	}

	public static ResourceCondition registryContains(Identifier registry, RegistryKey<?>... entries) {
		return new RegistryContainsResourceCondition(registry, entries);
	}

	public static <T> ResourceCondition registryContains(RegistryKey<Registry<T>> registry, Identifier... entries) {
		return new RegistryContainsResourceCondition(registry.getValue(), entries);
	}

	@SafeVarargs
	public static <T> ResourceCondition registryContains(RegistryKey<Registry<T>> registry, RegistryKey<T>... entries) {
		return new RegistryContainsResourceCondition(registry.getValue(), entries);
	}

	static {
		ResourceConditions.register(ResourceConditionType.TRUE);
		ResourceConditions.register(ResourceConditionType.NOT);
		ResourceConditions.register(ResourceConditionType.AND);
		ResourceConditions.register(ResourceConditionType.OR);
		ResourceConditions.register(ResourceConditionType.ALL_MODS_LOADED);
		ResourceConditions.register(ResourceConditionType.ANY_MODS_LOADED);
		ResourceConditions.register(ResourceConditionType.TAGS_POPULATED);
		ResourceConditions.register(ResourceConditionType.FEATURES_ENABLED);
		ResourceConditions.register(ResourceConditionType.REGISTRY_CONTAINS);
	}
}
