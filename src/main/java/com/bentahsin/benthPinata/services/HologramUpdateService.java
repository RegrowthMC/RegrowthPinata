package com.bentahsin.benthPinata.services;

import com.bentahsin.benthPinata.hologram.services.IHologramService;
import com.bentahsin.benthPinata.pinata.PinataRepository;
import com.bentahsin.benthPinata.pinata.model.Pinata;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Sadece aktif Piñata'ların hologramlarını periyodik olarak güncellemekle sorumlu olan görev.
 * Çalışma sıklığı, kullanılan hologram servisine göre dinamik olarak ayarlanır.
 */
public class HologramUpdateService extends BukkitRunnable {

    private final PinataRepository pinataRepository;
    private final IHologramService hologramService;

    public HologramUpdateService(PinataRepository pinataRepository, IHologramService hologramService) {
        this.pinataRepository = pinataRepository;
        this.hologramService = hologramService;
    }

    @Override
    public void run() {
        if (pinataRepository.findAll().isEmpty()) {
            return;
        }

        for (Pinata pinata : pinataRepository.findAll()) {
            if (pinata.getEntity() != null && pinata.getEntity().isValid() && !pinata.isDying()) {
                hologramService.updateHologram(pinata);
            }
        }
    }
}