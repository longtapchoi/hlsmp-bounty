package de.elivb.donutBounty.commands;

import de.elivb.donutBounty.Bounty;
import de.elivb.donutBounty.gui.BountyGUI;
import de.elivb.donutBounty.gui.ConfirmGUI;
import de.elivb.donutBounty.manager.SignManager;
import de.elivb.donutBounty.utils.MessageUtils;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class BountyCommand implements CommandExecutor, TabCompleter {
   private final Bounty plugin;
   private SignManager signManager;

   public BountyCommand(Bounty plugin) {
      this.plugin = plugin;
   }

   public void setSignManager(SignManager signManager) {
      this.signManager = signManager;
   }

   private String formatAmountWithAbbreviation(double amount) {
      boolean abbreviationsEnabled = this.plugin.getConfig().getBoolean("economy.abbreviations.enabled", true);
      if (!abbreviationsEnabled) {
         return MessageUtils.formatAmount(amount);
      } else {
         List<String> formats = this.plugin.getConfig().getStringList("economy.abbreviations.formats");
         if (amount >= 1.0E12 && formats.contains("T")) {
            return String.format("%.1fT", amount / 1.0E12);
         } else if (amount >= (double)1.0E9F && formats.contains("B")) {
            return String.format("%.1fB", amount / (double)1.0E9F);
         } else if (amount >= (double)1000000.0F && formats.contains("M")) {
            return String.format("%.1fM", amount / (double)1000000.0F);
         } else {
            return amount >= (double)1000.0F && formats.contains("K") ? String.format("%.1fK", amount / (double)1000.0F) : MessageUtils.formatAmount(amount);
         }
      }
   }

   private double parseAmountWithAbbreviation(String input) throws NumberFormatException {
      if (input != null && !input.isEmpty()) {
         boolean abbreviationsEnabled = this.plugin.getConfig().getBoolean("economy.abbreviations.enabled", true);
         if (!abbreviationsEnabled) {
            return Double.parseDouble(input);
         } else {
            List<String> formats = this.plugin.getConfig().getStringList("economy.abbreviations.formats");
            String upperInput = input.toUpperCase();
            double multiplier = (double)1.0F;
            String numberPart = input;

            for(String format : formats) {
               if (upperInput.endsWith(format)) {
                  switch (format) {
                     case "K" -> multiplier = (double)1000.0F;
                     case "M" -> multiplier = (double)1000000.0F;
                     case "B" -> multiplier = (double)1.0E9F;
                     case "T" -> multiplier = 1.0E12;
                  }

                  numberPart = input.substring(0, input.length() - format.length());
                  break;
               }
            }

            double number = Double.parseDouble(numberPart);
            return number * multiplier;
         }
      } else {
         throw new NumberFormatException("Empty input");
      }
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         if (sender instanceof Player) {
            (new BountyGUI(this.plugin, (Player)sender, this.signManager)).open();
         } else {
            sender.sendMessage(MessageUtils.colorize(this.plugin.getConfigManager().getMessage("player-only")));
         }

         return true;
      } else {
         switch (args[0].toLowerCase()) {
            case "add":
               if (!(sender instanceof Player)) {
                  sender.sendMessage(MessageUtils.colorize(this.plugin.getConfigManager().getMessage("player-only")));
                  return true;
               }

               Player player = (Player)sender;
               if (!player.hasPermission("bounty.use")) {
                  MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("no-permission"));
                  this.plugin.getSoundManager().playSound(player, "no-perm");
                  return true;
               }

               if (args.length < 3) {
                  return true;
               }

               String targetName = args[1];

               double amount;
               try {
                  amount = this.parseAmountWithAbbreviation(args[2]);
               } catch (NumberFormatException var19) {
                  MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("invalid-amount"));
                  return true;
               }

               Player targetOnline = Bukkit.getPlayer(targetName);
               UUID targetUUID = null;
               if (targetOnline != null && targetOnline.isOnline()) {
                  targetUUID = targetOnline.getUniqueId();
                  String var22 = targetOnline.getName();
               } else {
                  OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                  if (offlineTarget != null && offlineTarget.hasPlayedBefore()) {
                     targetUUID = offlineTarget.getUniqueId();
                     if (offlineTarget.getName() != null) {
                        offlineTarget.getName();
                     }
                  } else {
                     try {
                        PreparedStatement stmt = this.plugin.getHeadDatabaseConnection().prepareStatement("SELECT uuid, name FROM player_heads WHERE name LIKE ? OR uuid LIKE ? LIMIT 1");

                        try {
                           stmt.setString(1, "%" + targetName + "%");
                           stmt.setString(2, "%" + targetName + "%");
                           ResultSet rs = stmt.executeQuery();
                           if (rs.next()) {
                              targetUUID = UUID.fromString(rs.getString("uuid"));
                              String finalName = rs.getString("name");
                           }
                        } catch (Throwable var20) {
                           if (stmt != null) {
                              try {
                                 stmt.close();
                              } catch (Throwable var18) {
                                 var20.addSuppressed(var18);
                              }
                           }

                           throw var20;
                        }

                        if (stmt != null) {
                           stmt.close();
                        }
                     } catch (SQLException e) {
                        e.printStackTrace();
                     }
                  }
               }

               if (targetUUID == null) {
                  MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("player-not-found"));
                  return true;
               }

               if (player.getUniqueId().equals(targetUUID)) {
                  MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("cannot-add-bounty-to-yourself"));
                  return true;
               }

               if (amount < this.plugin.getConfigManager().getMinAmount()) {
                  String msg = this.plugin.getConfigManager().getMessage("amount-below-min").replace("%min_amount_format%", this.formatAmountWithAbbreviation(this.plugin.getConfigManager().getMinAmount()));
                  MessageUtils.sendMessage(player, msg);
                  return true;
               }

               if (amount > this.plugin.getConfigManager().getMaxAmount()) {
                  String msg = this.plugin.getConfigManager().getMessage("amount-above-max").replace("%max_amount_format%", this.formatAmountWithAbbreviation(this.plugin.getConfigManager().getMaxAmount()));
                  MessageUtils.sendMessage(player, msg);
                  return true;
               }

               if (!this.plugin.getEconomy().has(player, amount)) {
                  MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("not-enough-balance"));
                  return true;
               }

               if (targetOnline != null) {
                  (new ConfirmGUI(this.plugin, player, targetOnline, amount)).open();
               } else {
                  (new ConfirmGUI(this.plugin, player, targetUUID, amount)).open();
               }
               break;
            case "reload":
               if (!sender.hasPermission("bounty.reload")) {
                  if (sender instanceof Player) {
                     MessageUtils.sendMessage((Player)sender, this.plugin.getConfigManager().getMessage("no-permission"));
                     this.plugin.getSoundManager().playSound((Player)sender, "no-perm");
                  }

                  return true;
               }

               this.plugin.getConfigManager().reload();
               this.plugin.reloadConfig();
               sender.sendMessage(MessageUtils.colorize(this.plugin.getConfigManager().getMessage("reload")));
               break;
            default:
               if (sender instanceof Player) {
                  (new BountyGUI(this.plugin, (Player)sender, this.signManager)).open();
               } else {
                  sender.sendMessage(MessageUtils.colorize(this.plugin.getConfigManager().getMessage("player-only")));
               }
         }

         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (args.length == 1) {
         completions.add("add");
         completions.add("reload");
      } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
         String input = args[1].toLowerCase();

         for(Player player : Bukkit.getOnlinePlayers()) {
            completions.add(player.getName());
         }

         try {
            PreparedStatement stmt = this.plugin.getHeadDatabaseConnection().prepareStatement("SELECT name FROM player_heads ORDER BY name ASC LIMIT 100");

            try {
               ResultSet rs = stmt.executeQuery();

               while(rs.next()) {
                  String name = rs.getString("name");
                  if (!completions.contains(name)) {
                     completions.add(name);
                  }
               }
            } catch (Throwable var11) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (SQLException var12) {
         }

         completions = (List)completions.stream().filter((namex) -> namex.toLowerCase().startsWith(input)).distinct().limit(50L).collect(Collectors.toList());
      } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
         completions.add(String.valueOf((int)this.plugin.getConfigManager().getMinAmount()));
         completions.add(String.valueOf((int)this.plugin.getConfigManager().getMaxAmount()));
         completions.add("10K");
         completions.add("100K");
         completions.add("1M");
         completions.add("10M");
      }

      return completions;
   }
}
