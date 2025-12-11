package org.lushplugins.pinata.pinata;

import org.lushplugins.pinata.pinata.model.Pinata;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aktif Piñata nesnelerinin depolandığı ve yönetildiği yer.
 * Bu sınıf, Piñata'ların bellekteki veritabanı gibi çalışır.
 */
public class PinataRepository {

    private final Map<UUID, Pinata> pinatasByEntityId = new ConcurrentHashMap<>();

    public void save(Pinata pinata) {
        pinatasByEntityId.put(pinata.getEntity().getUniqueId(), pinata);
    }

    public void remove(Pinata pinata) {
        pinatasByEntityId.remove(pinata.getEntity().getUniqueId());
    }

    /**
     * Bir Bukkit Entity'sine göre aktif bir Piñata bulur.
     * Bu, eski Pinata.isPinata() metodunun modern ve daha güvenli halidir.
     * @param entity Kontrol edilecek entity.
     * @return Bir Optional<Pinata> nesnesi. Piñata bulunursa dolu, bulunmazsa boş.
     */
    public Optional<Pinata> findByEntity(Entity entity) {
        return Optional.ofNullable(pinatasByEntityId.get(entity.getUniqueId()));
    }

    public Collection<Pinata> findAll() {
        return pinatasByEntityId.values();
    }

    public void clear() {
        pinatasByEntityId.clear();
    }
}