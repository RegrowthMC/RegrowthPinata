package com.bentahsin.benthPinata.hologram;

import com.bentahsin.benthPinata.BenthPinata;
import com.bentahsin.benthPinata.hologram.services.*;
import com.bentahsin.benthPinata.hologram.services.adapter.HologramAdapter_1_13_to_1_16;
import com.bentahsin.benthPinata.hologram.services.adapter.HologramAdapter_1_17_PLUS;
import com.bentahsin.benthPinata.hologram.services.adapter.IPacketAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

/**
 * Bir hologram servisi (IHologramService) uygulaması oluşturan bir fabrika sınıfı.
 * Sunucudaki eklentileri ve ayarları kontrol ederek en uygun hologram servisini seçer.
 * Öncelik sırası:
 * 1. Ayarlar dosyasında kapalıysa -> DummyHologramService
 * 2. ProtocolLib yüklüyse -> ProtocolHologramService (sürüme uygun adaptör ile)
 * 3. DecentHolograms yüklüyse -> DecentHologramService
 * 4. Hiçbiri yoksa -> DummyHologramService
 */
public final class HologramServiceProvider {

    /**
     * Seçilen paket adaptörünü önbelleğe alır, böylece sürüm tespiti sadece bir kez çalışır.
     * Plugin reload edildiğinde bu sınıf da yeniden yükleneceği için adaptör de yeniden seçilir.
     */
    private static IPacketAdapter packetAdapter;

    /**
     * Sunucu ortamına göre en uygun IHologramService uygulamasını oluşturur ve döndürür.
     *
     * @param plugin         Ana eklenti sınıfı, loglama için kullanılır.
     * @return Oluşturulan IHologramService nesnesi.
     */
    public static IHologramService create(BenthPinata plugin) {
        if (!plugin.getSettingsManager().isHologramEnabled()) {
            plugin.getLogger().info("Hologramlar config.yml dosyasından devre dışı bırakıldı.");
            return new DummyHologramService();
        }

        PluginManager pm = Bukkit.getPluginManager();

        if (pm.isPluginEnabled("ProtocolLib")) {
            if (packetAdapter == null) {
                packetAdapter = selectAdapter();
            }

            if (packetAdapter != null) {
                plugin.getLogger().info("Hologram desteği için ProtocolLib bulundu. " + packetAdapter.getClass().getSimpleName() + " adaptörü ile paket tabanlı sistem aktif edildi.");
                return new ProtocolHologramService(plugin.getMessageManager(), plugin.getSettingsManager(), packetAdapter);
            } else {
                plugin.getLogger().severe("BenthPinata bu sunucu sürümü için uyumlu bir ProtocolLib hologram adaptörü bulamadı!");
            }
        }

        if (pm.isPluginEnabled("DecentHolograms")) {
            plugin.getLogger().info("Hologram desteği için DecentHolograms bulundu ve API tabanlı sistem aktif edildi.");
            return new DecentHologramService(plugin.getMessageManager(), plugin.getSettingsManager());
        }

        plugin.getLogger().warning("Sunucuda ProtocolLib veya DecentHolograms bulunamadı. Hologramlar devre dışı bırakılacak.");
        return new DummyHologramService();
    }

    /**
     * Sunucunun NMS (net.minecraft.server) sürüm dizesini analiz eder ve
     * uygun paket adaptörünü seçer.
     *
     * @return Uyumlu bir IPacketAdapter nesnesi veya desteklenmiyorsa null.
     */
    private static IPacketAdapter selectAdapter() {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

            if (version.startsWith("v1_17") || version.startsWith("v1_18") || version.startsWith("v1_19") || version.startsWith("v1_20") || version.startsWith("v1_21")) {
                return new HologramAdapter_1_17_PLUS();
            }
            else if (version.startsWith("v1_13") || version.startsWith("v1_14") || version.startsWith("v1_15") || version.startsWith("v1_16")) {
                return new HologramAdapter_1_13_to_1_16();
            }
            else {
                return null;
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BenthPinata] Sunucu sürümü tespit edilemedi! Hologramlar çalışmayabilir.");
            e.printStackTrace();
            return null;
        }
    }
}