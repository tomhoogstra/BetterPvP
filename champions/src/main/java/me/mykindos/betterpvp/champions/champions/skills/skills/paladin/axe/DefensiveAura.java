package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class DefensiveAura extends Skill implements InteractSkill, CooldownSkill {

    @Inject
    public DefensiveAura(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Defensive Aura";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a axe to Activate",
                "",
                "Gives you, and all allies within " + ChatColor.GREEN + (6 + level) + ChatColor.GRAY + " blocks",
                "2 bonus hearts",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @Override
    public SkillType getType() {

        return SkillType.AXE;
    }


    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2);
    }


    @Override
    public void activate(Player player, int level) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 200, 0));
        AttributeInstance playerMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (playerMaxHealth != null) {
            player.setHealth(Math.min(player.getHealth() + 4, playerMaxHealth.getValue()));
            for (Player target : UtilPlayer.getNearbyAllies(player, player.getLocation(), (6 + level))) {

                target.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 200, 0));
                AttributeInstance targetMaxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (targetMaxHealth != null) {
                    target.setHealth(Math.min(target.getHealth() + 4, targetMaxHealth.getValue()));
                }

            }
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
