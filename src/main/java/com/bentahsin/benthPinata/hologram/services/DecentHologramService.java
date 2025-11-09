package com.bentahsin.benthPinata.hologram.services;

import com.bentahsin.benthPinata.configuration.MessageManager;
import com.bentahsin.benthPinata.configuration.SettingsManager;
import com.bentahsin.benthPinata.pinata.model.Pinata;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.List;
import java.util.stream.Collectors;

public class DecentHologramService implements IHologramService {

    private final MessageManager mm;
    private final SettingsManager sm;

    public DecentHologramService(MessageManager mm, SettingsManager sm) {
        this.mm = mm;
        this.sm = sm;
    }

    @Override
    public void createHologram(Pinata pinata) {
        String hologramName = "pinata-" + pinata.getUniqueId().toString();
        Location hologramLocation = getHologramLocation(pinata.getEntity().getLocation());

        DHAPI.createHologram(hologramName, hologramLocation, getHologramLines(pinata));
        pinata.setHologramId(hologramName);
    }

    @Override
    public void updateHologram(Pinata pinata) {
        Hologram hologram = DHAPI.getHologram(pinata.getHologramId());
        if (hologram == null) return;

        DHAPI.setHologramLines(hologram, getHologramLines(pinata));
        DHAPI.moveHologram(hologram, getHologramLocation(pinata.getEntity().getLocation()));
    }

    @Override
    public void deleteHologram(Pinata pinata) {
        Hologram hologram = DHAPI.getHologram(pinata.getHologramId());
        if (hologram != null) {
            hologram.delete();
        }
    }

    private List<String> getHologramLines(Pinata pinata) {
        return mm.getMessageList("hologram.lines").stream()
                .map(line -> line.replace("%health%", String.valueOf(pinata.getCurrentHealth())))
                .map(line -> line.replace("%max_health%", String.valueOf(pinata.getType().getMaxHealth())))
                .collect(Collectors.toList());
    }

    private Location getHologramLocation(Location entityLocation) {
        return entityLocation.clone().add(0, sm.getHologramOffsetY(), 0);
    }
}