package com.bentahsin.benthPinata.hologram.services;

import com.bentahsin.benthPinata.configuration.MessageManager;
import com.bentahsin.benthPinata.configuration.SettingsManager;
import com.bentahsin.benthPinata.pinata.model.Pinata;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProtocolHologramService implements IHologramService {

    private final ProtocolManager pm;
    private final MessageManager mm;
    private final SettingsManager sm;
    private final Map<UUID, List<Integer>> hologramLines = new ConcurrentHashMap<>();
    private static final AtomicInteger entityIdCounter = new AtomicInteger(Integer.MAX_VALUE - 10000); // Çakışmayı önlemek için yüksek bir yerden başlat

    public ProtocolHologramService(MessageManager mm, SettingsManager sm) {
        this.pm = ProtocolLibrary.getProtocolManager();
        this.mm = mm;
        this.sm = sm;
    }

    @Override
    public void createHologram(Pinata pinata) {
        Location location = pinata.getEntity().getLocation();
        List<String> lines = getHologramLines(pinata);
        List<Integer> entityIds = new ArrayList<>();

        double baseOffset = sm.getHologramOffsetY();

        for (int i = 0; i < lines.size(); i++) {
            Location lineLoc = location.clone().add(0, baseOffset + (0.25 * (lines.size() - 1 - i)), 0);
            int entityId = createHologramLine(lineLoc, lines.get(i));
            entityIds.add(entityId);
        }
        hologramLines.put(pinata.getUniqueId(), entityIds);
    }

    private int createHologramLine(Location loc, String text) {
        int entityId = entityIdCounter.decrementAndGet();

        PacketContainer spawnPacket = pm.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawnPacket.getIntegers().write(0, entityId); // Entity ID
        spawnPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
        spawnPacket.getDoubles()
                .write(0, loc.getX())
                .write(1, loc.getY())
                .write(2, loc.getZ());
        spawnPacket.getUUIDs().write(0, UUID.randomUUID());

        PacketContainer metadataPacket = pm.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entityId);
        WrappedDataWatcher watcher = new WrappedDataWatcher();

        watcher.setObject(new WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)), (byte) 0x20); // Görünmez
        watcher.setObject(new WrappedDataWatcherObject(5, WrappedDataWatcher.Registry.get(Boolean.class)), true); // Yerçekimsiz (NoGravity)

        Optional<?> optChatComponent = Optional.of(WrappedChatComponent.fromChatMessage(text)[0].getHandle());
        watcher.setObject(new WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true)), optChatComponent);
        watcher.setObject(new WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)), true); // İsim görünür

        sendPacketsToNearbyPlayers(loc, spawnPacket, metadataPacket);
        return entityId;
    }

    @Override
    public void updateHologram(Pinata pinata) {
        List<Integer> entityIds = hologramLines.get(pinata.getUniqueId());
        if (entityIds == null || entityIds.isEmpty()) return;

        Location location = pinata.getEntity().getLocation();
        List<String> lines = getHologramLines(pinata);

        for (int i = 0; i < entityIds.size(); i++) {
            int entityId = entityIds.get(i);
            Location lineLoc = location.clone().add(0, 2.2 + (0.25 * (lines.size() - 1 - i)), 0);
            PacketContainer teleportPacket = pm.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
            teleportPacket.getIntegers().write(0, entityId);
            teleportPacket.getDoubles()
                    .write(0, lineLoc.getX())
                    .write(1, lineLoc.getY())
                    .write(2, lineLoc.getZ());

            PacketContainer metadataPacket = pm.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            Optional<?> optChatComponent = Optional.of(WrappedChatComponent.fromChatMessage(lines.get(i))[0].getHandle());
            watcher.setObject(new WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true)), optChatComponent);
            sendPacketsToNearbyPlayers(location, teleportPacket, metadataPacket);
        }
    }

    @Override
    public void deleteHologram(Pinata pinata) {
        List<Integer> entityIds = hologramLines.remove(pinata.getUniqueId());
        if (entityIds == null || entityIds.isEmpty()) return;
        PacketContainer destroyPacket = pm.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroyPacket.getIntegerArrays().write(0, entityIds.stream().mapToInt(i->i).toArray());
        sendPacketsToNearbyPlayers(pinata.getEntity().getLocation(), destroyPacket);
    }

    private List<String> getHologramLines(Pinata pinata) {
        List<String> rawLines = mm.getMessageList("hologram.lines");

        return rawLines.stream()
                .map(line -> line
                        .replace("%health%", String.valueOf(pinata.getCurrentHealth()))
                        .replace("%max_health%", String.valueOf(pinata.getType().getMaxHealth()))
                )
                .collect(Collectors.toList());
    }

    private void sendPacketsToNearbyPlayers(Location location, PacketContainer... packets) {
        final int viewDistanceSquared = 32 * 32;

        for (Player player : Objects.requireNonNull(location.getWorld()).getPlayers()) {
            if (player.getLocation().distanceSquared(location) < viewDistanceSquared) {
                for (PacketContainer packet : packets) {
                    pm.sendServerPacket(player, packet);
                }
            }
        }
    }
}