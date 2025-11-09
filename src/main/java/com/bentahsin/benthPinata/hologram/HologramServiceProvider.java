package com.bentahsin.benthPinata.hologram;

import com.bentahsin.benthPinata.BenthPinata;
import com.bentahsin.benthPinata.configuration.MessageManager;
import com.bentahsin.benthPinata.configuration.SettingsManager;
import com.bentahsin.benthPinata.hologram.services.DecentHologramService;
import com.bentahsin.benthPinata.hologram.services.DummyHologramService;
import com.bentahsin.benthPinata.hologram.services.IHologramService;
import com.bentahsin.benthPinata.hologram.services.ProtocolHologramService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public class HologramServiceProvider {

    public static IHologramService create(BenthPinata plugin) {
        PluginManager pm = Bukkit.getPluginManager();
        MessageManager mm = plugin.getMessageManager();
        SettingsManager sm = plugin.getSettingsManager();

        if (!sm.isHologramEnabled()) {
            plugin.getLogger().info("Hologramlar config.yml dosyasından devre dışı bırakıldı.");
            return new DummyHologramService();
        }

        if (pm.isPluginEnabled("ProtocolLib")) {
            plugin.getLogger().info("Hologram desteği için ProtocolLib bulundu ve paket tabanlı sistem aktif edildi.");
            return new ProtocolHologramService(mm, sm);
        }
        else if (pm.isPluginEnabled("DecentHolograms")) {
            plugin.getLogger().info("Hologram desteği için DecentHolograms bulundu ve aktif edildi.");
            return new DecentHologramService(mm, sm);
        }
        else {
            plugin.getLogger().warning("Sunucuda ProtocolLib veya DecentHolograms bulunamadı. Hologramlar devre dışı bırakılacak.");
            return new DummyHologramService();
        }
    }
}