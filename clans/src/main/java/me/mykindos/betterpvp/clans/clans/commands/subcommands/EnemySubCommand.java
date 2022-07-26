package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanEnemyEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanNeutralEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestNeutralEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;

public class EnemySubCommand extends ClanSubCommand {

    public EnemySubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "enemy";
    }

    @Override
    public String getDescription() {
        return "Wage war with another clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.message(player, "Clans", "You must specify a clan to enemy.");
            return;
        }

        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player);
        if(playerClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "You are not in a clan.");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getObject(args[0]);
        if(targetClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "The target clan does not exist.");
            return;
        }

        Clan playerClan = playerClanOptional.get();
        Clan targetClan = targetClanOptional.get();

        if(playerClan.equals(targetClan)) {
            UtilMessage.message(player, "Clans", "You cannot enemy your own clan");
            return;
        }

        if (!playerClan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, "Clans", "Only the clan admins can form alliances.");
            return;
        }

        if(playerClan.isEnemy(targetClan)) {
            UtilMessage.message(player, "Clans", "You are already enemy with this clan.");
            return;
        }

        UtilServer.callEvent(new ClanEnemyEvent(player, playerClan, targetClan));
    }

    @Override
    public String getArgumentType(int arg) {
        return ClanArgumentType.CLAN.name();
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
