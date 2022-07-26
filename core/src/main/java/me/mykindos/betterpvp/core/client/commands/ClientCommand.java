package me.mykindos.betterpvp.core.client.commands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientCommand extends Command {

    @WithReflection
    public ClientCommand() {
        subCommands.add(new AdminSubCommand());
        subCommands.add(new SearchSubCommand());
        subCommands.add(new PromoteSubCommand());
    }

    @Override
    public String getName() {
        return "client";
    }

    @Override
    public String getDescription() {
        return "Base client command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Command", "You must specify a sub command");
    }

    private static class AdminSubCommand extends SubCommand {

        @Override
        public String getName() {
            return "admin";
        }

        @Override
        public String getDescription() {
            return "Enable administration mode";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            client.setAdministrating(!client.isAdministrating());
            UtilMessage.message(player, "Command", "Client admin: "
                    + (client.isAdministrating() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }

    private static class SearchSubCommand extends SubCommand {

        @Inject
        private ClientManager clientManager;

        @Override
        public String getName() {
            return "search";
        }

        @Override
        public String getDescription() {
            return "Search for a client by name";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length != 1) {
                UtilMessage.message(player, "Command", "You must provide a name to search");
                return;
            }

            String name = args[0];

            Optional<Client> clientOptional = clientManager.getClientByName(name);
            clientOptional.ifPresentOrElse(target -> {
                List<String> result = new ArrayList<>();
                result.add(ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " Client Details:");
                //event.getResult().add(ChatColor.YELLOW + "IP Address: "
                //        + (client.hasRank(Rank.ADMIN, false) ? ChatColor.GRAY + target.getIP() : ChatColor.RED + "N/A"));
                //event.getResult().add(ChatColor.YELLOW + "Previous Name: " + ChatColor.GRAY + target.getOldName());
                //event.getResult().add(ChatColor.YELLOW + "IP Alias: " + ChatColor.GRAY + (client.hasRank(Rank.ADMIN, false)
                //        ? ClientUtilities.getDetailedIPAlias(target, false) : ClientUtilities.getDetailedIPAlias(target, true)));
                //event.getResult().add(ChatColor.YELLOW + "Rank: " + ChatColor.GRAY + UtilFormat.cleanString(target.getRank().toString()));
                //event.getResult().add(ChatColor.YELLOW + "Discord Linked: " + ChatColor.GRAY + target.isDiscordLinked());
                //event.getResult().add(ChatColor.YELLOW + "Punishments: " + ChatColor.GRAY + punishments);

                result.forEach(message -> UtilMessage.message(player, message));

            }, () -> UtilMessage.message(player, "Command", "Could not find a client with this name"));
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }

    private static class PromoteSubCommand extends SubCommand {

        @Inject
        private ClientManager clientManager;

        @Override
        public String getName() {
            return "promote";
        }

        @Override
        public String getDescription() {
            return "Promote a client to a higher rank";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            if (args.length == 0) {
                UtilMessage.message(player, "Client", "You must specify a client");
                return;
            }

            Optional<Client> clientOptional = clientManager.getClientByName(args[0]);
            if (clientOptional.isPresent()) {
                Client targetClient = clientOptional.get();
                Rank targetRank = Rank.getRank(targetClient.getRank().getId() + 1);
                if(targetRank != null) {
                    if (client.getRank().getId() < targetRank.getId() || player.isOp()) {
                        targetClient.setRank(targetRank);
                        UtilMessage.message(player, "Client", "%s has been promoted to %s",
                                ChatColor.YELLOW + targetClient.getName() + ChatColor.GRAY, targetRank.getTag(true));
                        clientManager.getRepository().save(targetClient);
                    }else{
                        UtilMessage.message(player, "Client", "You cannot promote someone to your current rank or higher.");
                    }
                }else{
                    UtilMessage.message(player, "Client", "%s already has the highest rank.",
                            ChatColor.YELLOW + targetClient.getName() + ChatColor.GRAY);
                }
            }
        }
    }
}
