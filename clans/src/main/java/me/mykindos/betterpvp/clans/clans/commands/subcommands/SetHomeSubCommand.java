package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanSetHomeEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SetHomeSubCommand extends ClanSubCommand {

    public SetHomeSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "sethome";
    }

    @Override
    public String getDescription() {
        return "Set the home teleport for your clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player);
        if (playerClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "You are not in a clan");
            return;
        }

        Clan playerClan = playerClanOptional.get();
        if (!playerClan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, "Clans", "You must be a clan admin or above to use this command");
            return;
        }

        UtilServer.callEvent(new ClanSetHomeEvent(player, playerClan));
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
