package com.bentahsin.benthPinata.hologram.services.adapter;

import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;

import java.util.List;

/**
 * Farklı Minecraft sürümleri için sürüme özel paket oluşturma mantığını soyutlar.
 */
public interface IPacketAdapter {
    PacketContainer createSpawnPacket(int entityId, Location location);
    PacketContainer createMetadataPacket(int entityId, String text, boolean initial);
    PacketContainer createTeleportPacket(int entityId, Location location);
    PacketContainer createDestroyPacket(List<Integer> entityIds);
}