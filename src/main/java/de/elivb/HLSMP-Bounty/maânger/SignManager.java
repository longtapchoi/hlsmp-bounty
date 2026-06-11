package de.elivb.donutBounty.manager;

import de.elivb.donutBounty.Bounty;
import de.elivb.donutBounty.utils.MessageUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SignManager implements Listener {
   private final Bounty plugin;
   private final Map<UUID, SignSession> activeSessions;
   private final Map<UUID, Location> signLocations;

   public SignManager(Bounty plugin) {
      this.plugin = plugin;
      this.activeSessions = new HashMap();
      this.signLocations = new HashMap();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void openSignEditor(Player player, BiConsumer<Player, String> callback) {
      UUID uuid = player.getUniqueId();
      this.closeSession(player);
      SignSession session = new SignSession(callback);
      this.activeSessions.put(uuid, session);
      Location signLoc = player.getLocation().getBlock().getLocation();
      Block block = signLoc.getBlock();
      session.oldBlockType = block.getType();
      session.oldBlockData = block.getBlockData();
      block.setType(Material.OAK_SIGN);
      this.signLocations.put(uuid, signLoc);
      List<String> signLines = this.plugin.getConfig().getStringList("sign-gui");
      String[] lines = new String[4];

      for(int i = 0; i < 4 && i < signLines.size(); ++i) {
         String line = (String)signLines.get(i);
         lines[i] = line != null ? MessageUtils.colorize(line) : "";
      }

      BlockState i = block.getState();
      if (i instanceof Sign sign) {
         for(int i = 0; i < 4; ++i) {
            sign.setLine(i, lines[i]);
         }

         sign.update();
      }

      player.sendSignChange(signLoc, lines);

      try {
         if (player.getScheduler() != null) {
            player.getScheduler().run(this.plugin, (scheduledTask) -> {
               if (player.isOnline()) {
                  Location currentLoc = (Location)this.signLocations.get(uuid);
                  if (currentLoc != null && currentLoc.getWorld() != null) {
                     Block currentBlock = currentLoc.getBlock();
                     BlockState patt0$temp = currentBlock.getState();
                     if (patt0$temp instanceof Sign) {
                        Sign sign = (Sign)patt0$temp;

                        try {
                           player.openSign(sign);
                        } catch (Exception var8) {
                        }
                     }

                  }
               }
            }, (Runnable)null);
         } else {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               if (player.isOnline()) {
                  Location currentLoc = (Location)this.signLocations.get(uuid);
                  if (currentLoc != null && currentLoc.getWorld() != null) {
                     Block currentBlock = currentLoc.getBlock();
                     BlockState patt0$temp = currentBlock.getState();
                     if (patt0$temp instanceof Sign) {
                        Sign sign = (Sign)patt0$temp;

                        try {
                           player.openSign(sign);
                        } catch (Exception var7) {
                        }
                     }

                  }
               }
            }, 2L);
         }
      } catch (Exception var15) {
         try {
            Location currentLoc = (Location)this.signLocations.get(uuid);
            if (currentLoc != null && currentLoc.getWorld() != null) {
               Block currentBlock = currentLoc.getBlock();
               BlockState var13 = currentBlock.getState();
               if (var13 instanceof Sign) {
                  Sign sign = (Sign)var13;
                  player.openSign(sign);
               }
            }
         } catch (Exception var14) {
         }
      }

   }

   public void closeSession(Player player) {
      UUID uuid = player.getUniqueId();
      SignSession session = (SignSession)this.activeSessions.remove(uuid);
      if (session != null) {
         Location loc = (Location)this.signLocations.remove(uuid);
         if (loc != null && loc.getWorld() != null) {
            Block block = loc.getBlock();
            if (block != null && block.getType() == Material.OAK_SIGN) {
               if (session.oldBlockType != null && session.oldBlockType != Material.AIR) {
                  block.setType(session.oldBlockType);
                  if (session.oldBlockData != null) {
                     block.setBlockData(session.oldBlockData);
                  }
               } else {
                  block.setType(Material.AIR);
               }
            }
         }
      }

   }

   @EventHandler
   public void onSignChange(SignChangeEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (this.activeSessions.containsKey(uuid)) {
         SignSession session = (SignSession)this.activeSessions.get(uuid);
         event.setCancelled(true);
         String input = event.getLine(0);
         if (input == null || input.isEmpty()) {
            input = event.getLine(1);
         }

         if (input != null && !input.isEmpty()) {
            this.activeSessions.remove(uuid);
            Location loc = (Location)this.signLocations.remove(uuid);
            if (loc != null && loc.getWorld() != null) {
               Block block = loc.getBlock();
               if (block != null && block.getType() == Material.OAK_SIGN) {
                  if (session.oldBlockType != null && session.oldBlockType != Material.AIR) {
                     block.setType(session.oldBlockType);
                     if (session.oldBlockData != null) {
                        block.setBlockData(session.oldBlockData);
                     }
                  } else {
                     block.setType(Material.AIR);
                  }
               }
            }

            if (session.callback != null && !input.isEmpty()) {
               session.callback.accept(player, input);
            }

         } else {
            this.closeSession(player);
         }
      }
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (this.activeSessions.containsKey(uuid)) {
         if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.OAK_SIGN) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (this.activeSessions.containsKey(uuid)) {
         Location signLoc = (Location)this.signLocations.get(uuid);
         if (signLoc != null && signLoc.getWorld() != null && event.getBlock().getLocation().equals(signLoc)) {
            event.setCancelled(true);
         }
      }

   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.closeSession(event.getPlayer());
   }

   private static class SignSession {
      final BiConsumer<Player, String> callback;
      Material oldBlockType;
      BlockData oldBlockData;

      SignSession(BiConsumer<Player, String> callback) {
         this.callback = callback;
         this.oldBlockType = Material.AIR;
      }
   }
}
