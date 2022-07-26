package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.exceptions.NoSuchGamerException;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Gate;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@BPvPListener
public class ClansWorldListener extends ClanListener {


    @Inject
    public ClansWorldListener(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @EventHandler
    public void onLogin(ClientLoginEvent event) {
        Optional<Clan> clanOptional = clanManager.getClanByClient(event.getClient());
        clanOptional.ifPresent(clan -> clan.setOnline(true));
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        Optional<Clan> clanOptional = clanManager.getClanByPlayer(event.getPlayer());
        clanOptional.ifPresent(clan -> {
            for (ClanMember member : clan.getMembers()) {
                Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
                if (player != null) {
                    return;
                }
            }

            clan.setOnline(false);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        Gamer gamer = gamerManager.getObject(player.getUniqueId().toString()).orElseThrow(() -> new NoSuchGamerException(player.getName()));
        Clan clan = clanManager.getClanByPlayer(player).orElse(null);

        if (UtilBlock.isTutorial(block.getLocation())) {
            return;
        }

        if (gamer.getClient().isAdministrating()) {
            return;
        }

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(block.getLocation());
        locationClanOptional.ifPresent(locationClan -> {
            if (!locationClan.equals(clan)) {
                ClanRelation relation = clanManager.getRelation(clan, locationClan);

                if (locationClan.isAdmin()) {
                    if (locationClan.getName().contains("Fields")) {
                        return;
                    }
                }

                // TODO this stuff

                //if (!(locationClan instanceof AdminClan)) {
                //    if (FarmBlocks.isCultivation(block.getType())) {
                //        return;
                //    }
                //}


                //if (Pillage.isPillaging(clan, locationClan)) {
                //    return;
                //}


                UtilMessage.message(player, "Clans", "You cannot break " + ChatColor.GREEN + UtilFormat.cleanString(block.getType().toString())
                        + ChatColor.GRAY + " in " + ChatColor.YELLOW + relation.getPrimaryAsChatColor()
                        + "Clan " + locationClan.getName() + ChatColor.GRAY + ".");
                event.setCancelled(true);


            } else {
                if (clan.getMember(player.getUniqueId()).getRank() == ClanMember.MemberRank.RECRUIT) {
                    UtilMessage.message(player, "Clans", "Clan Recruits cannot break blocks" + ChatColor.GRAY + ".");
                    event.setCancelled(true);

                }
            }
        });

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Gamer gamer = gamerManager.getObject(player.getUniqueId().toString()).orElseThrow(() -> new NoSuchGamerException(player.getName()));
        Block block = event.getBlock();


        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(block.getLocation());
        //Clan clan = ClanUtilities.getClan(player);
        //Clan locationClan = ClanUtilities.getClan(block.getLocation());

        if (gamer.getClient().isAdministrating()) {
            return;
        }

        if (UtilBlock.isTutorial(block.getLocation())) {
            return;
        }


        locationClanOptional.ifPresent(locationClan -> {

            ClanRelation relation = clanManager.getRelation(clan, locationClan);

            if (block.getType() == Material.SAND || block.getType() == Material.GRAVEL
                    || block.getType().name().contains("CONCRETE_POWDER")) {
                UtilMessage.message(player, "Clans", "You cannot place " + ChatColor.GREEN + UtilFormat.cleanString(block.getType().toString())
                        + ChatColor.GRAY + " in " + ChatColor.YELLOW + relation.getPrimaryAsChatColor()
                        + "Clan " + locationClan.getName() + ChatColor.GRAY + ".");
                event.setCancelled(true);
            }

            if (!locationClan.equals(clan)) {

                // TODO add pillage
                //if (Pillage.isPillaging(clan, locationClan)) {
                //    return;
                //}

                UtilMessage.message(player, "Clans", "You cannot place " + ChatColor.GREEN + UtilFormat.cleanString(block.getType().toString())
                        + ChatColor.GRAY + " in " + ChatColor.YELLOW + relation.getPrimaryAsChatColor()
                        + "Clan " + locationClan.getName() + ChatColor.GRAY + ".");
                event.setCancelled(true);
            } else {
                if (clan.getMember(player.getUniqueId()).getRank() == ClanMember.MemberRank.RECRUIT) {
                    UtilMessage.message(player, "Clans", "Clan Recruits cannot place blocks" + ChatColor.GRAY + ".");
                    event.setCancelled(true);
                }
            }
        });


    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null) return;
        if (UtilBlock.isTutorial(block.getLocation())) return;

        Gamer gamer = gamerManager.getObject(player.getUniqueId().toString()).orElseThrow(() -> new NoSuchGamerException(player.getName()));

        if (gamer.getClient().isAdministrating()) return;

        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(block.getLocation());
        locationClanOptional.ifPresent(locationClan -> {
            if (locationClan != clan) {

                ClanRelation relation = clanManager.getRelation(clan, locationClan);

                if (locationClan.isAdmin() && block.getType() == Material.ENCHANTING_TABLE) return;
                if (block.getType() == Material.REDSTONE_ORE) return;
                if (relation == ClanRelation.ALLY_TRUST && (block.getType() == Material.IRON_DOOR
                        || block.getType() == Material.IRON_TRAPDOOR
                        || block.getType().name().contains("GATE")
                        || block.getType().name().contains("DOOR")
                        || block.getType().name().contains("_BUTTON")
                        || block.getType() == Material.LEVER)) {
                    return;
                }

                // TODO this
                //if (Pillage.isPillaging(clan, locationClan)) {
                //    return;
                //}


                if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST || block.getType() == Material.LEVER
                        || block.getType().name().contains("_BUTTON") || block.getType() == Material.FURNACE
                        || block.getType() == Material.OAK_FENCE_GATE || block.getType() == Material.CRAFTING_TABLE || UtilBlock.usable(block)) {

                    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        if (block.getType() == Material.ENDER_CHEST) return;

                    }

                    // TODO koth
                    //if (KOTHManager.koth != null) {
                    //    if (KOTHManager.koth.getLocation().getBlockX() == block.getLocation().getBlockX()
                    //            && KOTHManager.koth.getLocation().getBlockZ() == block.getLocation().getBlockZ()) {
                    //        return;
                    //    }
                    //}


                    UtilMessage.message(player, "Clans", "You cannot use " + ChatColor.GREEN + UtilFormat.cleanString(block.getType().toString())
                            + ChatColor.GRAY + " in " + ChatColor.YELLOW + relation.getPrimaryAsChatColor()
                            + "Clan " + locationClan.getName() + ChatColor.GRAY + ".");
                    event.setCancelled(true);
                }
            } else {
                if (clan.getMember(player.getUniqueId()).getRank() == ClanMember.MemberRank.RECRUIT) {
                    if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                        UtilMessage.message(player, "Clans", "Clan Recruits cannot access " + ChatColor.GREEN + UtilFormat.cleanString(block.getType().toString())
                                + ChatColor.GRAY + ".");
                        event.setCancelled(true);
                    }
                }
            }
        });

    }

    /*
     * Stops players from breaking other clans bases with pistons on the outside
     */
    @EventHandler
    public void onPistonEvent(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            Optional<Clan> blockClanOptional = clanManager.getClanByLocation(block.getLocation());
            Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());

            blockClanOptional.ifPresent(blockClan -> {
                if (!blockClanOptional.equals(locationClanOptional)) {
                    event.setCancelled(true);
                }
            });
        }
    }

    /*
     * Stops players from breaking other clans bases with pistons on the outside
     */
    @EventHandler
    public void onPistonEvent(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            Optional<Clan> blockClanOptional = clanManager.getClanByLocation(block.getLocation());
            Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());

            blockClanOptional.ifPresent(blockClan -> {
                if (!blockClanOptional.equals(locationClanOptional)) {
                    event.setCancelled(true);
                }
            });
        }
    }

    /*
     * Stops players from breaking Item Frames in admin territory
     */
    @EventHandler
    public void onBreak(HangingBreakByEntityEvent event) {
        Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getEntity().getLocation());
        clanOptional.ifPresent(clan -> {
            if (!clan.isAdmin()) return;
            if (event.getRemover() instanceof Player player) {
                Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
                gamerOptional.ifPresent(gamer -> {
                    if (!gamer.getClient().isAdministrating()) {
                        event.setCancelled(true);
                    }
                });
            }
        });
    }

    /*
     * Another method of stopping players from taking items or breaking Armour Stands
     */
    @EventHandler
    public void armorStand(PlayerArmorStandManipulateEvent event) {

        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId().toString());
        gamerOptional.ifPresent(gamer -> {
            if (!gamer.getClient().isAdministrating()) {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Stops Armour stands from being broken in admin territory
     */
    @EventHandler
    public void onArmorStandDeath(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof ArmorStand || e.getEntity() instanceof ItemFrame) {

            Optional<Clan> clanOptional = clanManager.getClanByLocation(e.getEntity().getLocation());
            clanOptional.ifPresent(clan -> {
                if (!clan.isAdmin()) return;
                if (e.getDamager() instanceof Player player) {
                    Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
                    gamerOptional.ifPresent(gamer -> {
                        if (!gamer.getClient().isAdministrating()) {
                            e.setCancelled(true);
                        }
                    });
                }
            });

        }
    }

    /*
     * Stops players from interacting with item frames and armour stands (left click)
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Material material = event.getClickedBlock().getType();
        if (material == Material.ITEM_FRAME || material == Material.ARMOR_STAND) {
            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getClickedBlock().getLocation());
            clanOptional.ifPresent(clan -> {
                if (!clan.isAdmin()) return;
                Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId().toString());
                gamerOptional.ifPresent(gamer -> {
                    if (!gamer.getClient().isAdministrating()) {
                        event.setCancelled(true);
                    }
                });

            });

        }
    }

    /*
     * Stops players from taking stuff off armour stands and item frames in
     * admin territory
     */
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand || event.getRightClicked() instanceof ItemFrame) {

            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getRightClicked().getLocation());
            clanOptional.ifPresent(clan -> {
                if (clan.isAdmin()) {
                    Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId().toString());
                    gamerOptional.ifPresent(gamer -> {
                        if (!gamer.getClient().isAdministrating()) {
                            event.setCancelled(true);
                        }
                    });
                }
            });

        }
    }

    /*
     * Logs the location and player of chests that are opened in the wilderness
     * Useful for catching xrayers.
     */
    @EventHandler
    public void onOpenChestInWilderness(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Material m = event.getClickedBlock().getType();
        if (m == Material.CHEST || m == Material.TRAPPED_CHEST
                || m == Material.FURNACE || m == Material.DROPPER || m == Material.CAULDRON
                || m == Material.SHULKER_BOX || m == Material.BARREL) {

            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getPlayer().getLocation());
            if (clanOptional.isPresent()) return;

            int x = (int) event.getClickedBlock().getLocation().getX();
            int y = (int) event.getClickedBlock().getLocation().getY();
            int z = (int) event.getClickedBlock().getLocation().getZ();
            log.info("{} opened a chest at {}, {}, {}, {}", event.getPlayer().getName(), event.getPlayer().getWorld().getName(), x, y, z);
        }

    }

    /**
     * Helps against people glitching through gates, maybe this isn't an issue in 1.19 anymore
     *
     * @param event
     */
    @EventHandler
    public void onBreakGate(BlockBreakEvent event) {

        if (!event.getBlock().getType().name().contains("GATE")) return;
        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(event.getPlayer());
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());

        Gate gate = (Gate) event.getBlock().getState().getBlockData();
        if (gate.isOpen()) {
            locationClanOptional.ifPresent(locationClan -> {
                if (playerClanOptional.isEmpty() || !playerClanOptional.equals(locationClanOptional)) {
                    if (event.getPlayer().getLocation().distance(event.getBlock().getLocation()) < 1.5) {

                        UtilVelocity.velocity(event.getPlayer(),
                                UtilVelocity.getTrajectory(event.getPlayer().getLocation(), clanManager.closestWildernessBackwards(event.getPlayer())),
                                0.5, true, 0.25, 0.25, 0.25, false);

                    }
                }
            });
        }
    }

    /*
     * Turns lapis into water when placed.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLapisPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        if (event.getBlock().getType() == Material.LAPIS_BLOCK) {
            Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId().toString());
            if (gamerOptional.isEmpty()) return;
            Gamer gamer = gamerOptional.get();
            if (gamer.getClient().isAdministrating()) return;

            Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
            Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(event.getPlayer());

            if (locationClanOptional.isEmpty() || playerClanOptional.isEmpty() || !locationClanOptional.equals(playerClanOptional)) {
                if (event.getBlock().getLocation().getY() > 32) {
                    UtilMessage.message(event.getPlayer(), "Clans", "You can only place water in your own territory.");
                    event.setCancelled(true);
                    return;
                }
            }

            if (event.getBlock().getY() > 150) {
                UtilMessage.message(event.getPlayer(), "Clans", "You can only place water below 150Y");
                event.setCancelled(true);
                return;
            }
            Block block = event.getBlock();
            block.setType(Material.WATER);
            block.getLocation().getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F);
            block.getState().update();


        }
    }

    /*
     * Prevent obsidian from being broken by non admins
     */
    @EventHandler
    public void onBreakObsidian(BlockBreakEvent event) {

        if (event.getBlock().getType() == Material.OBSIDIAN) {
            Player player = event.getPlayer();
            event.setCancelled(true);
            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
            if (clanOptional.isPresent()) {
                if (clanOptional.get().isAdmin()) {
                    UtilMessage.message(player, "Server", "You cannot break " + ChatColor.YELLOW + "Obsidian" + ChatColor.GRAY + ".");
                    return;
                }
            }

            event.getBlock().setType(Material.AIR);
            UtilMessage.message(player, "Server", "You cannot break " + ChatColor.YELLOW + "Obsidian" + ChatColor.GRAY + ".");
        }
    }

    /*
     * Stops leaf decay in admin clan territory
     */
    @EventHandler
    public void stopLeafDecay(LeavesDecayEvent event) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            event.setCancelled(true);
            return;
        }
        Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
        clanOptional.ifPresent(clan -> {
            if (clan.isAdmin()) {
                event.setCancelled(true);
            }
        });

    }

    /*
     * Stops players from placing items such a levers and buttons on the outside of peoples bases
     * This is required, as previously, players could open the doors to an enemy base.
     */
    @EventHandler
    public void onAttachablePlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.LEVER || event.getBlock().getType().name().contains("_BUTTON")) {
            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlockAgainst().getLocation());
            clanOptional.ifPresent(clan -> {
                Optional<Clan> playerClanOption = clanManager.getClanByPlayer(event.getPlayer());
                if (!playerClanOption.equals(clanOptional)) {
                    event.setCancelled(true);
                }
            });

        }
    }

    /**
     * Stop players shooting bows in safezones if they have not taken damage recently
     *
     * @param event The event
     */
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        clanManager.getClanByLocation(player.getLocation()).ifPresent(clan -> {
            if (clan.isSafe()) {
                gamerManager.getObject(player.getUniqueId()).ifPresent(gamer -> {
                    if (UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                        event.setCancelled(true);
                    }
                });
            }
        });
    }
}
