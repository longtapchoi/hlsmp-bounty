package de.elivb.donutBounty.manager;

import de.elivb.donutBounty.Bounty;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
   private final Bounty plugin;
   private final YamlConfiguration langConfig;
   private final YamlConfiguration mainGuiConfig;
   private final YamlConfiguration confirmGuiConfig;
   private double minAmount;
   private double maxAmount;
   private boolean killMyselfGetReward;
   private final Map<String, Object> soundConfig;

   public ConfigManager(Bounty plugin) {
      this.plugin = plugin;
      this.langConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "lang.yml"));
      this.mainGuiConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui/main-gui.yml"));
      this.confirmGuiConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui/confirm-gui.yml"));
      this.soundConfig = new HashMap();
      this.reload();
   }

   public void reload() {
      this.plugin.reloadConfig();
      this.plugin.getConfig().options().copyDefaults(true);
      this.plugin.saveConfig();
      this.minAmount = this.plugin.getConfig().getDouble("limits.min-amount", (double)10.0F);
      this.maxAmount = this.plugin.getConfig().getDouble("limits.max-amount", (double)10000.0F);
      this.killMyselfGetReward = this.plugin.getConfig().getBoolean("if-kill-myself-get-reward", false);
      if (this.plugin.getConfig().contains("sounds")) {
         this.soundConfig.clear();
         this.soundConfig.put("bounty-added.enabled", this.plugin.getConfig().getBoolean("sounds.bounty-added.enabled", true));
         this.soundConfig.put("bounty-added.sound", this.plugin.getConfig().getString("sounds.bounty-added.sound", "ENTITY_PLAYER_LEVELUP"));
         this.soundConfig.put("no-perm.enabled", this.plugin.getConfig().getBoolean("sounds.no-perm.enabled", true));
         this.soundConfig.put("no-perm.sound", this.plugin.getConfig().getString("sounds.no-perm.sound", "BLOCK_ANVIL_LAND"));
         this.soundConfig.put("gui-click.enabled", this.plugin.getConfig().getBoolean("sounds.gui-click.enabled", true));
         this.soundConfig.put("gui-click.sound", this.plugin.getConfig().getString("sounds.gui-click.sound", "UI_BUTTON_CLICK"));
      }

   }

   public String getMessage(String key) {
      String message = this.langConfig.getString("messages." + key);
      return message != null ? message : "&cMessage not found: " + key;
   }

   public String getActionBar(String key) {
      String message = this.langConfig.getString("action-bars." + key);
      return message != null ? message : "&cActionBar not found: " + key;
   }

   public YamlConfiguration getMainGuiConfig() {
      return this.mainGuiConfig;
   }

   public YamlConfiguration getConfirmGuiConfig() {
      return this.confirmGuiConfig;
   }

   public double getMinAmount() {
      return this.minAmount;
   }

   public double getMaxAmount() {
      return this.maxAmount;
   }

   public boolean isKillMyselfGetReward() {
      return this.killMyselfGetReward;
   }

   public Map<String, Object> getSoundConfig() {
      return this.soundConfig;
   }
}
