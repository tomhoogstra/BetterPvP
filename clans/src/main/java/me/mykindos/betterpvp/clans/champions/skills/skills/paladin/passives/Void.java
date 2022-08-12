package me.mykindos.betterpvp.clans.champions.skills.skills.paladin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.UUID;

@Singleton
@BPvPListener
public class Void extends ActiveToggleSkill implements EnergySkill {

    @Inject
    public Void(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Void";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop Axe/Sword to Toggle.",
                "",
                "While in void form, you receive",
                "Slownesss III, and take no Knockback",
                "",
                "Reduces incoming damage by 5, but",
                "burns 20 of your energy",
                "",
                "Energy / Second: " + ChatColor.GREEN + getEnergy(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }


    @UpdateEvent(delay = 1000)
    public void audio() {
        for (UUID uuid : active) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2F, 0.5F);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2F, 0.5F);
            }
        }
    }

    @UpdateEvent
    public void update() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                if (!player.hasPotionEffect(PotionEffectType.SLOW)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 2));
                }

                int level = getLevel(player);
                if (level <= 0) {
                    it.remove();
                } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 6, true)) {
                    it.remove();
                }
            } else {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (!active.contains(damagee.getUniqueId())) return;

        int level = getLevel(damagee);
        if (level > 0) {
            event.setDamage(event.getDamage() - 5);
            championsManager.getEnergy().degenerateEnergy(damagee, 0.20);

            event.setKnockback(false);
        }


    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public float getEnergy(int level) {

        return (float) (energy - ((level - 1) * 0.5));
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {
            active.remove(player.getUniqueId());
            UtilMessage.message(player, getClassType().getName(), "Void: " + ChatColor.RED + "Off");
        } else {
            active.add(player.getUniqueId());
            if (championsManager.getEnergy().use(player, "Void", 5, false)) {
                UtilMessage.message(player, getClassType().getName(), "Void: " + ChatColor.GREEN + "On");
            }
        }
    }
}
