package me.mykindos.betterpvp.champions.champions.skills.skills.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class BreakFall extends Skill implements PassiveSkill {

    @Inject
    public BreakFall(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Break Fall";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You roll when you hit the ground.",
                "",
                "Fall damage is reduced by " + ChatColor.GREEN + (5 + level)};
    }

    @Override
    public Role getClassType() {
        return null;
    }

    @Override
    public SkillType getType() {

        return SkillType.GLOBAL;
    }


    @EventHandler
    public void onFall(CustomDamageEvent e) {
        if(!(e.getDamagee() instanceof Player player)) return;
        if(e.getCause() != DamageCause.FALL) return;

        int level = getLevel(player);
        if(level > 0) {
            e.setDamage(e.getDamage() - (3 + level));
        }

    }

}
