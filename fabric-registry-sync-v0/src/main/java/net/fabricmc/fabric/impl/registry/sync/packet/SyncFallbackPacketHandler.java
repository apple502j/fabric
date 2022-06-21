package net.fabricmc.fabric.impl.registry.sync.packet;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public class SyncFallbackPacketHandler {
	public static final Identifier HANDLER_ID = new Identifier("fabric", "registry/sync/fallback");

	public static PacketByteBuf createPacket(Multimap<Identifier, Identifier> syncFallbackAttempted) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeMap(syncFallbackAttempted.asMap(), PacketByteBuf::writeIdentifier, (buf2, ids) -> buf2.writeCollection(ids, PacketByteBuf::writeIdentifier));
		return buf;
	}

	public static Multimap<Identifier, Identifier> readPacket(PacketByteBuf buf) {
		Map<Identifier, Set<Identifier>> syncFallbackAttempted = buf.readMap(PacketByteBuf::readIdentifier, buf2 -> buf2.readCollection(ObjectOpenHashSet::new, PacketByteBuf::readIdentifier));
		Multimap<Identifier, Identifier> syncFallbackAttemptedMultimap = HashMultimap.create();
		for (Map.Entry<Identifier, Set<Identifier>> entry : syncFallbackAttempted.entrySet()) {
			syncFallbackAttemptedMultimap.putAll(entry.getKey(), entry.getValue());
		}
		return syncFallbackAttemptedMultimap;
	}
}
