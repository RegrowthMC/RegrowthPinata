package org.lushplugins.pinata.expansion.placeholders;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public interface IPlaceholder {

    /**
     * Placeholder'ın benzersiz tanımlayıcısını döndürür.
     * Örn: "stats_damage" (ana ön ek olan "benthpinata_" olmadan)
     * @return Placeholder tanımlayıcısı.
     */
    @NotNull
    String getIdentifier();

    /**
     * Placeholder istendiğinde değeri hesaplayan ve döndüren metot.
     * @param player Değerin hesaplanacağı oyuncu.
     * @return Placeholder'ın işlenmiş metin değeri.
     */
    String getValue(OfflinePlayer player);
}