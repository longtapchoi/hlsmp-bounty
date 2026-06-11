package de.elivb.donutBounty.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.elivb.donutBounty.Bounty;
import de.elivb.donutBounty.manager.SignManager;
import de.elivb.donutBounty.utils.MessageUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

public class BountyGUI implements Listener {
   private final Bounty plugin;
   private final Player player;
   private final SignManager signManager;
   private Inventory inventory;
   private String currentSort = "highest_price";
   private int currentPage = 0;
   private String searchFilter = null;
   private boolean isSearchMode = false;
   private static final int ITEMS_PER_PAGE = 45;
   private static final Map<UUID, BountyGUI> openGUIs = new ConcurrentHashMap();

   public BountyGUI(Bounty plugin, Player player, SignManager signManager) {
      this.plugin = plugin;
      this.player = player;
      this.signManager = signManager;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void open() {
      this.openPage(0);
   }

   public void openWithSearch(String search) {
      this.searchFilter = search;
      this.isSearchMode = true;
      this.currentPage = 0;
      this.openPage(0);
   }

   public void clearSearch() {
      this.searchFilter = null;
      this.isSearchMode = false;
      this.currentPage = 0;
      this.openPage(0);
   }

   private String formatBountyAmount(double amount) {
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

   private void openPage(int page) {
      this.currentPage = page;
      List<Map.Entry<UUID, Double>> allBounties = this.plugin.getBountyManager().getAllBountiesSorted(this.currentSort);
      List<Map.Entry<UUID, Double>> bounties;
      if (this.isSearchMode && this.searchFilter != null && !this.searchFilter.isEmpty()) {
         bounties = allBounties.stream().filter((entryx) -> {
            String name = this.plugin.getHeadCacheManager().getPlayerNameSync((UUID)entryx.getKey());
            return name != null && name.toLowerCase().contains(this.searchFilter.toLowerCase());
         }).toList();
      } else {
         bounties = allBounties;
      }

      int totalPages = (int)Math.ceil((double)bounties.size() / (double)45.0F);
      if (totalPages == 0) {
         totalPages = 1;
      }

      String title = this.plugin.getConfigManager().getMainGuiConfig().getString("gui-main.title", "&8ʙᴏᴜɴᴛʏ (Page %page%/%max%)").replace("%page%", String.valueOf(page + 1)).replace("%max%", String.valueOf(totalPages));
      title = MessageUtils.colorize(title);
      int rows = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.rows", 6);
      this.inventory = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
      int start = page * 45;
      int end = Math.min(start + 45, bounties.size());
      int slot = 0;

      for(int i = start; i < end; ++i) {
         Map.Entry<UUID, Double> entry = (Map.Entry)bounties.get(i);
         UUID uuid = (UUID)entry.getKey();
         double bounty = (Double)entry.getValue();
         this.loadPlayerHeadAsync(uuid, slot, bounty);
         ++slot;
      }

      this.addNavigationItems(totalPages);
      this.player.openInventory(this.inventory);
      openGUIs.put(this.player.getUniqueId(), this);
   }

   private void loadPlayerHeadAsync(UUID uuid, int slot, double bounty) {
      this.plugin.getHeadCacheManager().getPlayerName(uuid).thenAccept((playerName) -> {
         if (playerName != null) {
            this.plugin.getHeadCacheManager().getOrFetchHeadBase64(uuid, playerName).thenAccept((base64) -> {
               ItemStack head = this.createPlayerHead(uuid, playerName, bounty, base64);
               if (head != null) {
                  this.inventory.setItem(slot, head);
               }

            });
         }
      });
   }

   private ItemStack createPlayerHead(UUID uuid, String name, double bounty, String base64) {
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)head.getItemMeta();
      if (base64 != null && !base64.isEmpty()) {
         try {
            PlayerProfile profile = Bukkit.createProfile(uuid, name);
            profile.setProperty(new ProfileProperty("textures", base64));
            meta.setPlayerProfile(profile);
         } catch (Exception var12) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
               meta.setOwningPlayer(target);
            } else {
               meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            }
         }
      } else {
         Player target = Bukkit.getPlayer(uuid);
         if (target != null && target.isOnline()) {
            meta.setOwningPlayer(target);
         } else {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
         }
      }

      meta.setDisplayName(MessageUtils.colorize(this.plugin.getConfigManager().getMainGuiConfig().getString("gui-main.items.bounty-head.name", "&#00fc88%name%").replace("%name%", name)));
      List<String> lore = new ArrayList();

      for(String line : this.plugin.getConfigManager().getMainGuiConfig().getStringList("gui-main.items.bounty-head.lore")) {
         String formattedBounty = this.formatBountyAmount(bounty);
         lore.add(MessageUtils.colorize(line.replace("%bounty%", formattedBounty)));
      }

      meta.setLore(lore);
      head.setItemMeta(meta);
      return head;
   }

   private void addNavigationItems(int totalPages) {
      int searchSlot = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.items.search.slot", 50);
      this.inventory.setItem(searchSlot, this.createGuiItem("search"));
      int refreshSlot = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.items.refresh.slot", 49);
      this.inventory.setItem(refreshSlot, this.createGuiItem("refresh"));
      int sortSlot = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.items.sort.slot", 48);
      this.inventory.setItem(sortSlot, this.createSortItem());
      int prevSlot = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.items.previous-page.slot", 45);
      this.inventory.setItem(prevSlot, this.createGuiItem("previous-page"));
      int nextSlot = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.items.next-page.slot", 53);
      this.inventory.setItem(nextSlot, this.createGuiItem("next-page"));
   }

   private ItemStack createGuiItem(String key) {
      String path = "gui-main.items." + key;
      Material material = Material.getMaterial(this.plugin.getConfigManager().getMainGuiConfig().getString(path + ".material", "STONE"));
      if (material == null) {
         material = Material.STONE;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName(MessageUtils.colorize(this.plugin.getConfigManager().getMainGuiConfig().getString(path + ".name", "&fItem")));
      List<String> lore = new ArrayList();

      for(String line : this.plugin.getConfigManager().getMainGuiConfig().getStringList(path + ".lore")) {
         lore.add(MessageUtils.colorize(line));
      }

      meta.setLore(lore);
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack createSortItem() {
      String path = "gui-main.items.sort";
      Material material = Material.getMaterial(this.plugin.getConfigManager().getMainGuiConfig().getString(path + ".material", "HOPPER"));
      if (material == null) {
         material = Material.HOPPER;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      List<String> keys = this.plugin.getConfigManager().getMainGuiConfig().getStringList(path + ".keys");
      List<String> options = this.plugin.getConfigManager().getMainGuiConfig().getStringList(path + ".options");
      String activePrefix = this.plugin.getConfigManager().getMainGuiConfig().getString(path + ".active_prefix", "&#00fc88● ");
      String inactivePrefix = this.plugin.getConfigManager().getMainGuiConfig().getString(path + ".inactive_prefix", "&f● ");
      meta.setDisplayName(MessageUtils.colorize(this.plugin.getConfigManager().getMainGuiConfig().getString(path + ".name", "&fSort")));
      List<String> lore = new ArrayList();

      for(int i = 0; i < keys.size() && i < options.size(); ++i) {
         String prefix = ((String)keys.get(i)).equals(this.currentSort) ? activePrefix : inactivePrefix;
         lore.add(MessageUtils.colorize(prefix + (String)options.get(i)));
      }

      meta.setLore(lore);
      item.setItemMeta(meta);
      return item;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player clickedPlayer) {
         if (openGUIs.containsKey(clickedPlayer.getUniqueId())) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
               int slot = event.getSlot();
               int sortSlot = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.items.sort.slot", 48);
               if (slot == sortSlot) {
                  this.cycleSort();
                  this.openPage(0);
                  this.plugin.getSoundManager().playSound(clickedPlayer, "gui-click");
               } else {
                  int refreshSlot = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.items.refresh.slot", 49);
                  if (slot == refreshSlot) {
                     if (this.isSearchMode) {
                        this.clearSearch();
                     } else {
                        this.openPage(this.currentPage);
                     }

                     this.plugin.getSoundManager().playSound(clickedPlayer, "gui-click");
                  } else {
                     int prevSlot = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.items.previous-page.slot", 45);
                     if (slot == prevSlot) {
                        this.plugin.getSoundManager().playSound(clickedPlayer, "gui-click");
                        if (this.currentPage > 0) {
                           this.openPage(this.currentPage - 1);
                        }

                     } else {
                        int nextSlot = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.items.next-page.slot", 53);
                        if (slot == nextSlot) {
                           this.plugin.getSoundManager().playSound(clickedPlayer, "gui-click");
                           List<Map.Entry<UUID, Double>> allBounties = this.plugin.getBountyManager().getAllBountiesSorted(this.currentSort);
                           List<Map.Entry<UUID, Double>> bounties;
                           if (this.isSearchMode && this.searchFilter != null && !this.searchFilter.isEmpty()) {
                              bounties = allBounties.stream().filter((entry) -> {
                                 String name = this.plugin.getHeadCacheManager().getPlayerNameSync((UUID)entry.getKey());
                                 return name != null && name.toLowerCase().contains(this.searchFilter.toLowerCase());
                              }).toList();
                           } else {
                              bounties = allBounties;
                           }

                           int totalPages = (int)Math.ceil((double)bounties.size() / (double)45.0F);
                           if (totalPages == 0) {
                              totalPages = 1;
                           }

                           if (this.currentPage < totalPages - 1) {
                              this.openPage(this.currentPage + 1);
                           }

                        } else {
                           int searchSlot = this.plugin.getConfigManager().getMainGuiConfig().getInt("gui-main.items.search.slot", 50);
                           if (slot != searchSlot) {
                              if (slot < 45) {
                                 ItemStack item = event.getCurrentItem();
                                 if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                                    String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                                    this.plugin.getSoundManager().playSound(clickedPlayer, "gui-click");
                                 }
                              }

                           } else {
                              this.plugin.getSoundManager().playSound(clickedPlayer, "gui-click");
                              clickedPlayer.closeInventory();
                              if (this.signManager != null && this.plugin.getConfig().getBoolean("chat-input.SignAPI", true)) {
                                 this.signManager.openSignEditor(clickedPlayer, (player1, input) -> {
                                    if (input != null && !input.isEmpty()) {
                                       (new BountyGUI(this.plugin, player1, this.signManager)).openWithSearch(input);
                                    } else {
                                       (new BountyGUI(this.plugin, player1, this.signManager)).open();
                                    }

                                 });
                              } else {
                                 MessageUtils.sendMessage(clickedPlayer, this.plugin.getConfigManager().getMessage("search-prompt"));
                              }

                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void cycleSort() {
      List<String> keys = this.plugin.getConfigManager().getMainGuiConfig().getStringList("gui-main.items.sort.keys");
      int currentIndex = keys.indexOf(this.currentSort);
      if (currentIndex + 1 < keys.size()) {
         this.currentSort = (String)keys.get(currentIndex + 1);
      } else {
         this.currentSort = (String)keys.get(0);
      }

   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      openGUIs.remove(event.getPlayer().getUniqueId());
   }
}
