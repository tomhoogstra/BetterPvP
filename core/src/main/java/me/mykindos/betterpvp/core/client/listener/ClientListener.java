package me.mykindos.betterpvp.core.client.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.inject.Inject;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.lunar.LunarClientEvent;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.protocol.game.ClientboundPlayerChatHeaderPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

@BPvPListener
public class ClientListener implements Listener {

    @Inject
    @Config(path = "pvp.enableOldPvP", defaultValue = "true")
    private boolean enableOldPvP;

    private final ClientManager clientManager;

    @Inject
    public ClientListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        Optional<Client> clientOptional = clientManager.getObject(uuid);
        Client client;
        if (clientOptional.isEmpty()) {
            client = Client.builder().uuid(uuid).name(event.getPlayer().getName()).rank(Rank.PLAYER).build();
            clientManager.addObject(uuid, client);
            clientManager.getRepository().save(client);

            event.joinMessage(Component.text(ChatColor.GREEN + "New> " + ChatColor.GRAY + event.getPlayer().getName()));
        } else {
            event.joinMessage(Component.text(ChatColor.GREEN + "Login> " + ChatColor.GRAY + event.getPlayer().getName()));
            client = clientOptional.get();
        }

        checkUnsetProperties(client);

        Bukkit.getPluginManager().callEvent(new ClientLoginEvent(client, event.getPlayer()));
        removeEncryption(event.getPlayer());
    }

    @EventHandler
    public void onClientLogin(ClientLoginEvent event) {
        if (enableOldPvP) {
            AttributeInstance attribute = event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attribute != null) {
                double baseValue = attribute.getBaseValue();

                // Setting this higher than the usual actually force removes the 1.9 attack indicator
                if (baseValue != 100000000) {
                    attribute.setBaseValue(100000000);
                    event.getPlayer().saveData();

                }
            }
        }

        updateTab(event.getPlayer());

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clientManager.getClientByName(event.getPlayer().getName()).ifPresent(client -> {
            ClientQuitEvent quitEvent = UtilServer.callEvent(new ClientQuitEvent(client, event.getPlayer()));
            if (!quitEvent.isCancelled()) {
                event.quitMessage(Component.text(quitEvent.getQuitMessage()));
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClientQuit(ClientQuitEvent event) {
        event.setQuitMessage(ChatColor.RED + "Leave> " + ChatColor.GRAY + event.getPlayer().getName());
    }

    @EventHandler
    public void onLunarEvent(LunarClientEvent event) {
        Optional<Client> clientOptional = clientManager.getObject(event.getPlayer().getUniqueId().toString());
        clientOptional.ifPresent(client -> {
            client.putProperty(ClientProperty.LUNAR, event.isRegistered());
        });
    }


    @UpdateEvent(delay = 5000)
    public void updateTabAllPlayers() {
        Bukkit.getOnlinePlayers().forEach(this::updateTab);
    }

    public void updateTab(Player player) {

        PacketContainer pc = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);

        var title = WrappedChatComponent.fromText(ChatColor.RED.toString() + ChatColor.BOLD + "Welcome to BetterPvP Clans!\n"
                + ChatColor.RED + ChatColor.BOLD + "Visit our website at: " + ChatColor.YELLOW + ChatColor.BOLD + "https://betterpvp.net");

        var info = WrappedChatComponent.fromText(ChatColor.GOLD.toString() + ChatColor.BOLD + "Ping: "
                + ChatColor.YELLOW + UtilPlayer.getPing(player) + ChatColor.GOLD + ChatColor.BOLD
                + " Online: " + ChatColor.YELLOW + Bukkit.getOnlinePlayers().size());

        pc.getChatComponents().write(0, title).write(1, info);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, pc);
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }
    }

    @EventHandler
    public void onSettingsUpdated(ClientPropertyUpdateEvent event) {

        clientManager.getRepository().saveProperty(event.getClient(), event.getProperty(), event.getValue());

        if (event.isUpdateScoreboard()) {
            Player player = Bukkit.getPlayer(UUID.fromString(event.getClient().getUuid()));
            if (player != null) {
                UtilServer.callEvent(new ScoreboardUpdateEvent(player));
            }
        }
    }

    private void checkUnsetProperties(Client client) {

        ClientProperty chatEnabledProperty = ClientProperty.CHAT_ENABLED;
        Optional<Integer> chatEnabledOptional = client.getProperty(chatEnabledProperty);
        if (chatEnabledOptional.isEmpty()) {
            client.saveProperty(chatEnabledProperty, true);
        }

    }

    @UpdateEvent(delay = 120_000)
    public void processStatUpdates() {
        clientManager.getRepository().processStatUpdates(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(event.getPlayer().getWorld().getSpawnLocation());
    }


    /**
     * Strip any encryption so chat cant be reported
     *
     * @param player The player
     */
    private void removeEncryption(Player player) {
        final ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        final ChannelPipeline pipeline = serverPlayer.connection.connection.channel.pipeline();

        pipeline.addAfter("packet_handler", "betterpvp_chat", new ChannelDuplexHandler() {
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

                // rewrite all signed (or unsigned) serverPlayer messages as system messages
                if (msg instanceof ClientboundPlayerChatPacket packet) {
                    final net.minecraft.network.chat.Component content = packet.message().unsignedContent().orElse(packet.message().signedContent().decorated());

                    final Optional<ChatType.Bound> ctbo = packet.chatType().resolve(serverPlayer.level.registryAccess());
                    if (ctbo.isEmpty()) {
                        return;
                    }
                    final net.minecraft.network.chat.Component decoratedContent = ctbo.orElseThrow().decorate(content);

                    super.write(ctx, new ClientboundSystemChatPacket(decoratedContent, false), promise);
                    return;
                }

                // strip useless header packets
                if (msg instanceof ClientboundPlayerChatHeaderPacket) {
                    return;
                }

                // remove unsigned content warning toast. all messages are now system.
                if (msg instanceof ClientboundServerDataPacket packet) {
                    super.write(ctx, new ClientboundServerDataPacket(packet.getMotd().orElse(null), packet.getIconBase64().orElse(null), packet.previewsChat(), true), promise);
                    return;
                }

                super.write(ctx, msg, promise);
            }
        });
    }
}
