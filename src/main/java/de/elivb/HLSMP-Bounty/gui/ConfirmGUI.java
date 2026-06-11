package de.elivb.donutBounty.gui;

import de.elivb.donutBounty.Bounty;
import de.elivb.donutBounty.utils.MessageUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ConfirmGUI implements Listener {
   private final Bounty plugin;
   private final Player player;
   private Player target;
   private UUID targetUUID;
   private String targetName;
   private final double amount;
   private Inventory inventory;
   private static final Map<UUID, ConfirmGUI> openGUIs = new HashMap();

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

   public ConfirmGUI(Bounty plugin, Player player, Player target, double amount) {
      this.plugin = plugin;
      this.player = player;
      this.target = target;
      this.targetUUID = target.getUniqueId();
      this.targetName = target.getName();
      this.amount = amount;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public ConfirmGUI(Bounty plugin, Player player, UUID targetUUID, double amount) {
      this.plugin = plugin;
      this.player = player;
      this.target = null;
      this.targetUUID = targetUUID;
      this.targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
      if (this.targetName == null) {
         this.targetName = "Unknown";
      }

      this.amount = amount;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void open() {
      String title = this.plugin.getConfigManager().getConfirmGuiConfig().getString("gui-confirm.titel", "&8ᴄᴏɴꜰɪʀᴍ ʙᴏᴜɴᴛʏ");
      int rows = this.plugin.getConfigManager().getConfirmGuiConfig().getInt("gui-confirm.rows", 3);
      this.inventory = Bukkit.createInventory((InventoryHolder)null, rows * 9, MessageUtils.colorize(title));
      int confirmSlot = this.plugin.getConfigManager().getConfirmGuiConfig().getInt("gui-confirm.items.confirm.slot", 15);
      this.inventory.setItem(confirmSlot, this.createConfirmItem());
      int cancelSlot = this.plugin.getConfigManager().getConfirmGuiConfig().getInt("gui-confirm.items.cancel.slot", 11);
      this.inventory.setItem(cancelSlot, this.createCancelItem());
      int playerSlot = this.plugin.getConfigManager().getConfirmGuiConfig().getInt("gui-confirm.items.player.slot", 13);
      this.inventory.setItem(playerSlot, this.createPlayerHead());
      this.player.openInventory(this.inventory);
      openGUIs.put(this.player.getUniqueId(), this);
   }

   private ItemStack createConfirmItem() {
      String path = "gui-confirm.items.confirm";
      Material material = Material.getMaterial(this.plugin.getConfigManager().getConfirmGuiConfig().getString(path + ".material", "LIME_STAINED_GLASS_PANE"));
      if (material == null) {
         material = Material.LIME_STAINED_GLASS_PANE;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName(MessageUtils.colorize(this.plugin.getConfigManager().getConfirmGuiConfig().getString(path + ".name", "&#04fc04ᴄᴏɴғɪʀᴍ")));
      String amountFormatted = this.formatAmountWithAbbreviation(this.amount);
      List<String> lore = new ArrayList();

      for(String line : this.plugin.getConfigManager().getConfirmGuiConfig().getStringList(path + ".lore")) {
         lore.add(MessageUtils.colorize(line.replace("%amount_format%", amountFormatted)));
      }

      meta.setLore(lore);
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack createCancelItem() {
      String path = "gui-confirm.items.cancel";
      Material material = Material.getMaterial(this.plugin.getConfigManager().getConfirmGuiConfig().getString(path + ".material", "RED_STAINED_GLASS_PANE"));
      if (material == null) {
         material = Material.RED_STAINED_GLASS_PANE;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName(MessageUtils.colorize(this.plugin.getConfigManager().getConfirmGuiConfig().getString(path + ".name", "&#fc0404ᴄᴀɴᴄᴇʟ")));
      List<String> lore = new ArrayList();

      for(String line : this.plugin.getConfigManager().getConfirmGuiConfig().getStringList(path + ".lore")) {
         lore.add(MessageUtils.colorize(line));
      }

      meta.setLore(lore);
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack createPlayerHead() {
      String path = "gui-confirm.items.player";
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)head.getItemMeta();
      String amountFormatted = this.formatAmountWithAbbreviation(this.amount);
      if (this.target != null) {
         meta.setOwningPlayer(this.target);
         String displayName = this.plugin.getConfigManager().getConfirmGuiConfig().getString(path + ".name", "&#00fc88%target_name%");
         displayName = displayName.replace("%target_name%", this.target.getName());
         meta.setDisplayName(MessageUtils.colorize(displayName));
      } else {
         String displayName = this.plugin.getConfigManager().getConfirmGuiConfig().getString(path + ".name", "&#00fc88%target_name%");
         displayName = displayName.replace("%target_name%", this.targetName);
         meta.setDisplayName(MessageUtils.colorize(displayName));
         meta.setOwningPlayer(Bukkit.getOfflinePlayer(this.targetUUID));
      }

      List<String> lore = new ArrayList();

      for(String line : this.plugin.getConfigManager().getConfirmGuiConfig().getStringList(path + ".lore")) {
         String formattedLine = line.replace("%amount_format%", amountFormatted);
         lore.add(MessageUtils.colorize(formattedLine));
      }

      meta.setLore(lore);
      head.setItemMeta(meta);
      return head;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player clickedPlayer) {
         ConfirmGUI gui = (ConfirmGUI)openGUIs.get(clickedPlayer.getUniqueId());
         if (gui != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
               int confirmSlot = this.plugin.getConfigManager().getConfirmGuiConfig().getInt("gui-confirm.items.confirm.slot", 15);
               int cancelSlot = this.plugin.getConfigManager().getConfirmGuiConfig().getInt("gui-confirm.items.cancel.slot", 11);
               if (event.getSlot() == confirmSlot) {
                  Player bountySetter = gui.player;
                  double bountyAmount = gui.amount;
                  UUID finalTargetUUID = gui.targetUUID;
                  String finalTargetName = gui.targetName;
                  if (!this.plugin.getEconomy().has(bountySetter, bountyAmount)) {
                     MessageUtils.sendMessage(bountySetter, this.plugin.getConfigManager().getMessage("not-enough-balance"));
                     bountySetter.closeInventory();
                     return;
                  }

                  this.plugin.getEconomy().withdrawPlayer(bountySetter, bountyAmount);
                  this.plugin.getBountyManager().addBounty(finalTargetUUID, finalTargetName, bountySetter.getUniqueId(), bountyAmount).thenAccept((success) -> {
                     if (success) {
                        String msg = this.plugin.getConfigManager().getMessage("bounty-added").replace("%amount_format%", this.formatAmountWithAbbreviation(bountyAmount)).replace("%target_name%", finalTargetName);
                        MessageUtils.sendMessage(bountySetter, msg);
                        MessageUtils.sendActionBar(bountySetter, this.plugin.getConfigManager().getActionBar("bounty-added").replace("%amount_format%", this.formatAmountWithAbbreviation(bountyAmount)).replace("%target_name%", finalTargetName));
                        this.plugin.getSoundManager().playSound(bountySetter, "bounty-added");
                        Player onlineTarget = Bukkit.getPlayer(finalTargetUUID);
                        if (onlineTarget != null && onlineTarget.isOnline()
                              && !this.plugin.getBountyToggleManager().isDisabled(finalTargetUUID)) {
                           String targetMsg = this.plugin.getConfigManager().getMessage("bounty-added-on-you").replace("%amount_format%", this.formatAmountWithAbbreviation(bountyAmount)).replace("%user_name%", bountySetter.getName());
                           MessageUtils.sendMessage(onlineTarget, targetMsg);
                           MessageUtils.sendActionBar(onlineTarget, this.plugin.getConfigManager().getActionBar("bounty-added-on-you").replace("%amount_format%", this.formatAmountWithAbbreviation(bountyAmount)).replace("%user_name%", bountySetter.getName()));
                        }
                     }

                  });
                  bountySetter.closeInventory();
                  this.plugin.getSoundManager().playSound(bountySetter, "gui-click");
               } else if (event.getSlot() == cancelSlot) {
                  gui.player.closeInventory();
                  this.plugin.getSoundManager().playSound(gui.player, "gui-click");
               }

            }
         }
      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      openGUIs.remove(event.getPlayer().getUniqueId());
   }
}
