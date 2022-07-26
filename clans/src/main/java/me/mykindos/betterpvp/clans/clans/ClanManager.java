package me.mykindos.betterpvp.clans.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.repository.ClanRepository;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

@Slf4j
@Singleton
public class ClanManager extends Manager<Clan> {

    @Getter
    private final ClanRepository repository;
    private final GamerManager gamerManager;

    private HashMap<Integer, Integer> dominanceScale;

    @Inject
    @Config(path = "clans.claims.additional", defaultValue = "3")
    private int additionalClaims;

    @Inject
    public ClanManager(ClanRepository repository, GamerManager gamerManager) {
        this.repository = repository;
        this.gamerManager = gamerManager;
        this.dominanceScale = new HashMap<>();

        var clans = repository.getAll();
        loadFromList(clans);

        clans.forEach(clan -> {
            clan.setTerritory(repository.getTerritory(this, clan));
            clan.setAlliances(repository.getAlliances(this, clan));
            clan.setEnemies(repository.getEnemies(this, clan));
            clan.setMembers(repository.getMembers(this, clan));
        });

        dominanceScale = repository.getDominanceScale();

        log.info("Loaded {} clans", clans.size());

    }

    public Optional<Clan> getClanById(int id) {
        return objects.values().stream().filter(clan -> clan.getId() == id).findFirst();
    }

    public Optional<Clan> getClanByClient(Client client) {
        return objects.values().stream()
                .filter(clan -> clan.getMemberByUUID(client.getUuid()).isPresent()).findFirst();
    }

    public Optional<Clan> getClanByPlayer(Player player) {
        return objects.values().stream()
                .filter(clan -> clan.getMemberByUUID(player.getUniqueId().toString()).isPresent()).findFirst();
    }

    public Optional<Clan> getClanByName(String name) {
        return Optional.ofNullable(objects.get(name.toLowerCase()));
    }

    /**
     * Finds a clan if the location is within a claimed chunk
     *
     * @param location The location to check
     * @return a Clan optional
     */
    public Optional<Clan> getClanByLocation(Location location) {
        return getClanByChunk(location.getChunk());
    }

    public Optional<Clan> getClanByChunk(Chunk chunk) {
        return objects.values().stream()
                .filter(clan -> clan.getTerritory().stream()
                        .anyMatch(territory -> territory.getChunk().equalsIgnoreCase(UtilWorld.chunkToFile(chunk)))).findFirst();
    }

    public boolean isClanMember(Player player, Player target) {
        Optional<Clan> aClanOptional = getClanByPlayer(player);
        Optional<Clan> bClanOptional = getClanByPlayer(target);

        if (aClanOptional.isEmpty() || bClanOptional.isEmpty()) return false;

        return aClanOptional.equals(bClanOptional);

    }

    // TODO implement pillaging
    public ClanRelation getRelation(IClan clanA, IClan clanB) {
        if (clanA == null || clanB == null) {
            return ClanRelation.NEUTRAL;
        } else if (clanA.equals(clanB)) {
            return ClanRelation.SELF;
        } else if (clanB.hasTrust(clanA)) {
            return ClanRelation.ALLY_TRUST;
        } else if (clanA.isAllied(clanB)) {
            return ClanRelation.ALLY;
        } else if (clanA.isEnemy(clanB)) {
            return ClanRelation.ENEMY;

            // } else if (Pillage.isPillaging(clanA, clanB)) {
            //    return ClanRelation.PILLAGE;
            //} else if (Pillage.isPillaging(clanB, clanA)) {
            //    return ClanRelation.PILLAGE;
        }

        return ClanRelation.NEUTRAL;
    }

    public boolean hasAccess(Player player, Location location) {
        Optional<Clan> playerClanOptional = getClanByPlayer(player);
        Optional<Clan> locationClanOptional = getClanByLocation(location);

        if (locationClanOptional.isEmpty()) return true;
        if (playerClanOptional.isEmpty()) return false;

        Clan playerClan = playerClanOptional.get();
        Clan locationClan = locationClanOptional.get();


        // TODO implement pillaging
        //if (Pillage.isPillaging(pClan, locClan)) {
        //    return true;
        //}


        ClanRelation relation = getRelation(playerClan, locationClan);

        return relation == ClanRelation.SELF || relation == ClanRelation.ALLY_TRUST;
    }

    public Location closestWildernessBackwards(Player player) {
        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            Location location = player.getLocation().add(player.getLocation().getDirection().multiply(i * -1));
            Optional<Clan> clanOptional = getClanByLocation(location);
            if (clanOptional.isEmpty()) {
                locations.add(location);
                break;
            }
        }

        if (locations.size() > 0) {

            locations.sort(Comparator.comparingInt(a -> (int) player.getLocation().distance(a)));
            return locations.get(0);
        }
        return null;

    }

    public Component getClanTooltip(Player player, Clan target) {
        List<String> list = new ArrayList<>();
        Clan clan = getClanByPlayer(player).orElse(null);
        list.add(ChatColor.RED + "[Clans] " + getRelation(clan, target).getPrimaryAsChatColor() + target.getName() + " Information:");
        list.add(" Age: " + ChatColor.YELLOW + target.getAge());
        list.add(" Territory: " + ChatColor.YELLOW + target.getTerritory().size() + "/" + (target.getMembers().size() + additionalClaims));
        list.add(" Allies: " + ChatColor.YELLOW + getAllianceList(player, target));
        list.add(" Enemies: " + ChatColor.YELLOW + getEnemyList(player, target));
        list.add(" Members: " + ChatColor.YELLOW + getMembersList(target));

        Component textComponent = Component.text().build();
        for (String text : list) {
            textComponent = textComponent.append(Component.text(text + "\n"));
        }
        return textComponent;
    }

    public String getAllianceList(Player player, Clan clan) {
        Clan playerClan = getClanByPlayer(player).orElse(null);
        StringBuilder allyString = new StringBuilder();
        if (!clan.getAlliances().isEmpty()) {
            for (ClanAlliance ally : clan.getAlliances()) {
                allyString.append(allyString.length() != 0 ? ChatColor.GRAY + ", " : "")
                        .append(getRelation(playerClan, ally.getClan()).getPrimary()).append(ally.getClan().getName());
            }
        }
        return allyString.toString();
    }

    public String getEnemyList(Player player, Clan clan) {
        Clan playerClan = getClanByPlayer(player).orElse(null);
        StringBuilder enemyString = new StringBuilder();
        if (!clan.getAlliances().isEmpty()) {
            for (ClanEnemy enemy : clan.getEnemies()) {
                enemyString.append(enemyString.length() != 0 ? ChatColor.GRAY + ", " : "")
                        .append(getRelation(playerClan, enemy.getClan()).getPrimary()).append(enemy.getClan().getName());
            }
        }
        return enemyString.toString();
    }

    public String getMembersList(Clan clan) {
        StringBuilder membersString = new StringBuilder();
        if (clan.getMembers() != null && !clan.getMembers().isEmpty()) {
            for (ClanMember member : clan.getMembers()) {
                Optional<Gamer> gamerOptional = gamerManager.getObject(member.getUuid());
                gamerOptional.ifPresent(gamer -> {
                    membersString.append(membersString.length() != 0 ? ChatColor.GRAY + ", " : "").append(ChatColor.YELLOW)
                            .append(member.getRoleIcon())
                            .append(UtilFormat.getOnlineStatus(member.getUuid()))
                            .append(gamer.getClient().getName());
                });

            }
        }
        return membersString.toString();
    }

    public boolean canHurt(Player player, Player target) {

        Clan targetLocationClan = getClanByLocation(target.getLocation()).orElse(null);
        if (targetLocationClan != null && targetLocationClan.isSafe()) {
            Optional<Gamer> gamerOptional = gamerManager.getObject(target.getUniqueId());
            if (gamerOptional.isPresent()) {
                Gamer gamer = gamerOptional.get();
                if (UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                    return false;
                }
            }
        }

        Clan playerClan = getClanByPlayer(player).orElse(null);
        Clan targetClan = getClanByPlayer(target).orElse(null);
        ClanRelation relation = getRelation(playerClan, targetClan);

        return relation != ClanRelation.SELF && relation != ClanRelation.ALLY && relation != ClanRelation.ALLY_TRUST;
    }

    public boolean canCast(Player player) {
        return canCast(player, true);
    }

    public boolean canCast(Player player, boolean message) {
        Optional<Clan> locationClanOptional = getClanByLocation(player.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (locationClan.isAdmin()) {

                if (locationClan.isSafe()) {

                    Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
                    if (gamerOptional.isPresent()) {
                        Gamer gamer = gamerOptional.get();
                        // Allow skills if player is combat tagged
                        if (!UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                            return true;

                        }
                    }


                    if (message) {
                        UtilMessage.message(player, "Restriction", "You are not allowed to cast abilities here!");
                    }
                    return false;
                }
            }
        }

        return true;
    }

    public void applyDominance(Clan killed, Clan killer) {
        if (killed == null || killer == null) return;
        if (killed.equals(killer)) return;
        if (!killed.isEnemy(killer)) return;

        ClanEnemy killedEnemy = killed.getEnemy(killer);
        ClanEnemy killerEnemy = killer.getEnemy(killed);

        int dominance = dominanceScale.getOrDefault(killed.getMembers().size(), 6);
        if (killedEnemy.getDominance() == 0) {
            killerEnemy.addDominance(dominance);
        }
        killedEnemy.takeDominance(dominance);

        killed.messageClan("You lost <red>%" + dominance + "<gray> dominance to <red>" + killer.getName(), null, true);
        killer.messageClan("You gained <green>%" + dominance + "<gray> dominance on <red>" + killed.getName(), null, true);

        getRepository().updateDominance(killed, killedEnemy);
        getRepository().updateDominance(killer, killerEnemy);

        killed.getMembers().forEach(member -> {
            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if (player != null) {
                UtilServer.callEvent(new ScoreboardUpdateEvent(player));
            }
        });

        killer.getMembers().forEach(member -> {
            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if (player != null) {
                UtilServer.callEvent(new ScoreboardUpdateEvent(player));
            }
        });
    }

    @Override
    public void loadFromList(List<Clan> objects) {
        objects.forEach(clan -> addObject(clan.getName().toLowerCase(), clan));
    }
}
