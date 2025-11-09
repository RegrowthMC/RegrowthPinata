package com.bentahsin.benthPinata.hologram.services.adapter;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Eski Minecraft sürümleri (1.13.2 - 1.16.5) için paket oluşturma mantığını uygular.
 * Bu sürüm aralığı, meta veriler için 'WrappedDataWatcher' nesnesini kullanır.
 */
public class HologramAdapter_1_13_to_1_16 implements IPacketAdapter {

    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    @Override
    public PacketContainer createSpawnPacket(int entityId, Location location) {
        PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        spawnPacket.getIntegers().write(0, entityId);
        spawnPacket.getUUIDs().write(0, UUID.randomUUID());
        spawnPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
        spawnPacket.getDoubles()
                .write(0, location.getX())
                .write(1, location.getY())
                .write(2, location.getZ());

        return spawnPacket;
    }

    @Override
    public PacketContainer createMetadataPacket(int entityId, String text, boolean initial) {
        PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entityId);

        WrappedDataWatcher watcher = new WrappedDataWatcher();

        Optional<?> optChatComponent = Optional.of(WrappedChatComponent.fromChatMessage(text)[0].getHandle());
        watcher.setObject(
                new WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true)),
                optChatComponent
        );

        if (initial) {
            watcher.setObject(
                    new WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)),
                    (byte) 0x20
            );
            watcher.setObject(
                    new WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)),
                    true
            );
            watcher.setObject(
                    new WrappedDataWatcherObject(5, WrappedDataWatcher.Registry.get(Boolean.class)),
                    true
            );
        }

        metadataPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
        return metadataPacket;
    }

    @Override
    public PacketContainer createTeleportPacket(int entityId, Location location) {
        PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

        teleportPacket.getIntegers().write(0, entityId);
        teleportPacket.getDoubles()
                .write(0, location.getX())
                .write(1, location.getY())
                .write(2, location.getZ());
        teleportPacket.getBooleans().write(0, true);

        return teleportPacket;
    }

    @Override
    public PacketContainer createDestroyPacket(List<Integer> entityIds) {
        PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);

        int[] entityIdArray = entityIds.stream().mapToInt(i -> i).toArray();
        destroyPacket.getIntegerArrays().write(0, entityIdArray);

        return destroyPacket;
    }
}