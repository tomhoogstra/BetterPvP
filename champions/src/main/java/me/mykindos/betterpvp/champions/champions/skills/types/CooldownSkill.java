package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.core.components.champions.ISkill;

public interface CooldownSkill extends ISkill {

    double getCooldown(int level);

    default boolean showCooldownFinished() {
        return true;
    }

    default boolean isCancellable(){
        return false;
    }

}
