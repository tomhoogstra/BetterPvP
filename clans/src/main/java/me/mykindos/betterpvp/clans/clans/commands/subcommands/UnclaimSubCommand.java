package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkUnclaimEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;

public class UnclaimSubCommand extends ClanSubCommand {

    @Inject
    @Config(path = "clans.claims.additional", defaultValue = "3")
    private int additionalClaims;

    public UnclaimSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "unclaim";
    }

    @Override
    public String getDescription() {
        return "Unclaim the territory you are standing on";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player);
        if(playerClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "You are not in a clan");
            return;
        }

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if(locationClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "You are not standing on claimed territory");
            return;
        }

        Clan playerClan = playerClanOptional.get();
        Clan locationClan = locationClanOptional.get();

        if(playerClan.equals(locationClan)) {
            if(!playerClan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
                UtilMessage.message(player, "Clans", "You must be an admin or above to unclaim territory");
                return;
            }

        }else{
            if(locationClan.isAdmin()) {
                UtilMessage.message(player, "Clans", "You cannot unclaim admin territory");
                return;
            }

            if(locationClan.getTerritory().size() <= locationClan.getMembers().size() + additionalClaims) {
                UtilMessage.simpleMessage(player, "Clans", "<yellow>%s<gray> has enough members to keep this territory.",
                        locationClan.getName());
                return;
            }
        }

        UtilServer.callEvent(new ChunkUnclaimEvent(player, locationClan));
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
