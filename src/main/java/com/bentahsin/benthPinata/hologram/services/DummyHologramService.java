package com.bentahsin.benthPinata.hologram.services;

import com.bentahsin.benthPinata.pinata.model.Pinata;

/**
 * Hiçbir hologram eklentisi bulunamadığında kullanılan boş bir uygulama.
 * NullPointerException'ları önler ve hologram mantığını opsiyonel hale getirir.
 */
public class DummyHologramService implements IHologramService {
    @Override
    public void createHologram(Pinata pinata) { }

    @Override
    public void updateHologram(Pinata pinata) { }

    @Override
    public void deleteHologram(Pinata pinata) { }
}