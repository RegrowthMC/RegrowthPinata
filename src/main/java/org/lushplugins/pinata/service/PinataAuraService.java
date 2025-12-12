package org.lushplugins.pinata.service;

import org.lushplugins.pinata.pinata.PinataRepository;
import org.lushplugins.pinata.pinata.model.Pinata;
import org.bukkit.DyeColor;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Tüm aktif Piñata'ların periyodik güncellemelerini yöneten merkezi bir servis.
 * Bu "Singleton Task" yapısı, her Piñata için ayrı görevler oluşturmak yerine
 * tek bir görev kullanarak sunucu kaynaklarını verimli bir şekilde kullanır.
 */
public class PinataAuraService extends BukkitRunnable {

    private final PinataRepository pinataRepository;
    private final AbilityService abilityService;
    private final Random random = new Random();

    public PinataAuraService(PinataRepository pinataRepository, AbilityService abilityService) {
        this.pinataRepository = pinataRepository;
        this.abilityService = abilityService;
    }

    @Override
    public void run() {
        if (pinataRepository.findAll().isEmpty()) {
            return;
        }

        for (Pinata pinata : pinataRepository.findAll()) {
            if (pinata.getEntity() == null || !pinata.getEntity().isValid() || pinata.isDying()) {
                continue;
            }

            abilityService.tryTriggerAbilities(pinata);

            if (pinata.getEntity() instanceof Sheep) {
                Sheep sheep = (Sheep) pinata.getEntity();
                sheep.setColor(DyeColor.values()[random.nextInt(DyeColor.values().length)]);
            }

            if (pinata.getEntity().hasAI() && pinata.getEntity() instanceof Mob) {
                updateMobTarget((Mob) pinata.getEntity());
            }
        }
    }

    private void updateMobTarget(Mob mob) {
        if (mob.getTarget() != null && mob.getTarget().isValid()) {
            return;
        }

        List<Player> nearbyPlayers = mob.getNearbyEntities(30, 30, 30)
                .stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .collect(Collectors.toList());

        if (!nearbyPlayers.isEmpty()) {
            mob.setTarget(nearbyPlayers.get(random.nextInt(nearbyPlayers.size())));
        }
    }
}