package com.bentahsin.benthPinata.hologram.services;

import com.bentahsin.benthPinata.configuration.MessageManager;
import com.bentahsin.benthPinata.configuration.SettingsManager;
import com.bentahsin.benthPinata.hologram.services.adapter.IPacketAdapter;
import com.bentahsin.benthPinata.pinata.model.Pinata;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * ProtocolLib kullanarak, sunucuya yük bindirmeyen, paket tabanlı hologramları yönetir.
 * Bu sınıf, paket oluşturma işini doğrudan yapmaz; bunun yerine sürüme özel bir 'IPacketAdapter'
 * kullanarak doğru paketleri oluşturur. Bu, çoklu sürüm desteği sağlar.
 */
public class ProtocolHologramService implements IHologramService {

    private final ProtocolManager protocolManager;
    private final MessageManager messageManager;
    private final SettingsManager settingsManager;
    private final IPacketAdapter packetAdapter;

    private final Map<UUID, List<Integer>> hologramLines = new ConcurrentHashMap<>();
    private static final AtomicInteger entityIdCounter = new AtomicInteger(Integer.MAX_VALUE - 10000);

    /**
     * ProtocolHologramService'i başlatır.
     *
     * @param messageManager  Metinleri almak için.
     * @param settingsManager Ayarları (örn: hologram yüksekliği) almak için.
     * @param packetAdapter   Sunucu sürümüne uygun paket oluşturucu.
     */
    public ProtocolHologramService(MessageManager messageManager, SettingsManager settingsManager, IPacketAdapter packetAdapter) {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.messageManager = messageManager;
        this.settingsManager = settingsManager;
        this.packetAdapter = packetAdapter;
    }

    @Override
    public void createHologram(Pinata pinata) {
        Location baseLocation = pinata.getEntity().getLocation();
        List<String> textLines = getHologramLines(pinata);
        List<Integer> entityIds = new ArrayList<>();

        double baseOffsetY = settingsManager.getHologramOffsetY();
        double lineSpacing = 0.25;

        for (int i = 0; i < textLines.size(); i++) {
            Location lineLocation = baseLocation.clone().add(0, baseOffsetY + (lineSpacing * (textLines.size() - 1 - i)), 0);
            int entityId = createHologramLine(lineLocation, textLines.get(i));
            entityIds.add(entityId);
        }

        hologramLines.put(pinata.getUniqueId(), entityIds);
    }

    @Override
    public void updateHologram(Pinata pinata) {
        List<Integer> entityIds = hologramLines.get(pinata.getUniqueId());
        if (entityIds == null || entityIds.isEmpty()) return;

        Location baseLocation = pinata.getEntity().getLocation();
        List<String> textLines = getHologramLines(pinata);

        double baseOffsetY = settingsManager.getHologramOffsetY();
        double lineSpacing = 0.25;

        for (int i = 0; i < entityIds.size(); i++) {
            if (i >= textLines.size()) break;

            int entityId = entityIds.get(i);
            Location lineLocation = baseLocation.clone().add(0, baseOffsetY + (lineSpacing * (textLines.size() - 1 - i)), 0);

            PacketContainer teleportPacket = packetAdapter.createTeleportPacket(entityId, lineLocation);
            PacketContainer metadataPacket = packetAdapter.createMetadataPacket(entityId, textLines.get(i), false); // 'initial' false çünkü bu bir güncelleme

            sendPacketsToNearbyPlayers(baseLocation, teleportPacket, metadataPacket);
        }
    }

    @Override
    public void deleteHologram(Pinata pinata) {
        List<Integer> entityIds = hologramLines.remove(pinata.getUniqueId());
        if (entityIds == null || entityIds.isEmpty()) return;

        PacketContainer destroyPacket = packetAdapter.createDestroyPacket(entityIds);
        sendPacketsToNearbyPlayers(pinata.getEntity().getLocation(), destroyPacket);
    }

    /**
     * Tek bir hologram satırı (görünmez bir zırh askısı) oluşturur ve bunu yakındaki
     * oyunculara gönderir.
     *
     * @param location Bu satırın oluşturulacağı tam konum.
     * @param text     Bu satırda gösterilecek metin.
     * @return Oluşturulan zırh askısının unique Entity ID'si.
     */
    private int createHologramLine(Location location, String text) {
        int entityId = entityIdCounter.decrementAndGet();

        PacketContainer spawnPacket = packetAdapter.createSpawnPacket(entityId, location);
        PacketContainer metadataPacket = packetAdapter.createMetadataPacket(entityId, text, true); // 'initial' true çünkü bu ilk oluşturma

        sendPacketsToNearbyPlayers(location, spawnPacket, metadataPacket);
        return entityId;
    }

    /**
     * Piñata'nın anlık durumuna göre hologram metinlerini oluşturur ve placeholder'ları doldurur.
     *
     * @param pinata Verileri alınacak Piñata.
     * @return İşlenmiş ve renklendirilmiş metin satırlarının listesi.
     */
    private List<String> getHologramLines(Pinata pinata) {
        List<String> rawLines = messageManager.getMessageList("hologram.lines");

        return rawLines.stream()
                .map(line -> line
                        .replace("%health%", String.valueOf(pinata.getCurrentHealth()))
                        .replace("%max_health%", String.valueOf(pinata.getType().getMaxHealth()))
                )
                .collect(Collectors.toList());
    }

    /**
     * Belirtilen paketleri, bir konumun etrafındaki oyunculara gönderir.
     *
     * @param location Paketlerin gönderileceği merkez konum.
     * @param packets  Gönderilecek olan bir veya daha fazla PacketContainer.
     */
    private void sendPacketsToNearbyPlayers(Location location, PacketContainer... packets) {
        final int viewDistanceSquared = 32 * 32;

        for (Player player : Objects.requireNonNull(location.getWorld()).getPlayers()) {
            if (player.getLocation().distanceSquared(location) < viewDistanceSquared) {
                for (PacketContainer packet : packets) {
                    protocolManager.sendServerPacket(player, packet);
                }
            }
        }
    }
}