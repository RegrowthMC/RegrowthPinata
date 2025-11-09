package com.bentahsin.benthPinata.hologram.services;

import com.bentahsin.benthPinata.pinata.model.Pinata;

/**
 * Piñata hologramlarını yönetmek için bir arayüz tanımlar.
 * Bu, farklı hologram eklentileri arasında geçiş yapabilen modüler bir yapı sağlar.
 */
public interface IHologramService {
    void createHologram(Pinata pinata);
    void updateHologram(Pinata pinata);
    void deleteHologram(Pinata pinata);
}