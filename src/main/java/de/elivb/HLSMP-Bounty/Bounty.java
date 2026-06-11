package de.elivb.donutBounty;

import de.elivb.donutBounty.commands.BountyCommand;
import de.elivb.donutBounty.commands.BountyToggleCommand;
import de.elivb.donutBounty.listeners.KillListener;
import de.elivb.donutBounty.manager.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;

public final class Bounty extends JavaPlugin {
   private static Bounty instance;
   private Economy economy;
   private BountyManager bountyManager;
   private ConfigManager configManager;
   private SoundManager soundManager;
   private SignManager signManager;
   private HeadCacheManager headCacheManager;
   private BountyToggleManager bountyToggleManager;
   private Connection databaseConnection;
   private Connection headDatabaseConnection;
   private LicenseManager licenseManager;

   @Override
   public void onEnable() {
      instance = this;
      this.licenseManager = new LicenseManager(this);

      if (!this.setupEconomy()) {
         getLogger().severe("Không tìm thấy Vault! Đang tắt plugin.");
         this.getServer().getPluginManager().disablePlugin(this);
         return;
      }

      // Tạo folder data trước
      if (!this.getDataFolder().exists()) this.getDataFolder().mkdirs();

      this.saveDefaultConfig();
      this.saveResource("lang.yml", false);
      this.saveResource("gui/confirm-gui.yml", false);
      this.saveResource("gui/main-gui.yml", false);

      this.configManager = new ConfigManager(this);
      this.soundManager = new SoundManager(this);
      this.signManager = new SignManager(this);
      this.bountyToggleManager = new BountyToggleManager(this);

      this.setupDatabase();
      this.setupHeadDatabase();

      this.bountyManager = new BountyManager(this);
      this.headCacheManager = new HeadCacheManager(this);

      BountyCommand bountyCommand = new BountyCommand(this);
      bountyCommand.setSignManager(this.signManager);
      this.getCommand("bounty").setExecutor(bountyCommand);
      this.getCommand("bounty").setTabCompleter(bountyCommand);
      this.getCommand("bountytoggle").setExecutor(new BountyToggleCommand(this));

      this.getServer().getPluginManager().registerEvents(new KillListener(this), this);

      getLogger().info("HLSMP-Bounty đã khởi động!");
   }

   public LicenseManager getLicenseManager() {
      return this.licenseManager;
   }

   @Override
   public void onDisable() {
      try {
         if (this.databaseConnection != null && !this.databaseConnection.isClosed())
            this.databaseConnection.close();
      } catch (SQLException e) { e.printStackTrace(); }

      try {
         if (this.headDatabaseConnection != null && !this.headDatabaseConnection.isClosed())
            this.headDatabaseConnection.close();
      } catch (SQLException e) { e.printStackTrace(); }

      getLogger().info("HLSMP-Bounty đã tắt!");
   }

   private boolean setupEconomy() {
      if (this.getServer().getPluginManager().getPlugin("Vault") == null) return false;
      RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp == null) return false;
      this.economy = rsp.getProvider();
      return this.economy != null;
   }

   private void setupDatabase() {
      try {
         Class.forName("org.sqlite.JDBC");
         this.databaseConnection = DriverManager.getConnection("jdbc:sqlite:" + this.getDataFolder() + "/bounties.db");
         try (Statement stmt = this.databaseConnection.createStatement()) {
            stmt.execute("""
               CREATE TABLE IF NOT EXISTS bounties (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  target_uuid VARCHAR(36) NOT NULL,
                  target_name VARCHAR(16) NOT NULL,
                  amount DOUBLE NOT NULL,
                  added_by VARCHAR(36),
                  timestamp BIGINT NOT NULL,
                  claimed BOOLEAN DEFAULT 0
               )
            """);
         }
      } catch (SQLException | ClassNotFoundException e) {
         getLogger().severe("Lỗi khởi tạo database bounties: " + e.getMessage());
      }
   }

   private void setupHeadDatabase() {
      try {
         Class.forName("org.sqlite.JDBC");
         this.headDatabaseConnection = DriverManager.getConnection("jdbc:sqlite:" + this.getDataFolder() + "/heads.db");
         try (Statement stmt = this.headDatabaseConnection.createStatement()) {
            stmt.execute("""
               CREATE TABLE IF NOT EXISTS player_heads (
                  uuid VARCHAR(36) PRIMARY KEY,
                  name VARCHAR(16) NOT NULL,
                  head_base64 TEXT,
                  last_updated BIGINT NOT NULL
               )
            """);
         }
      } catch (SQLException | ClassNotFoundException e) {
         getLogger().severe("Lỗi khởi tạo database heads: " + e.getMessage());
      }
   }

   public static Bounty getInstance() { return instance; }
   public Economy getEconomy() { return this.economy; }
   public BountyManager getBountyManager() { return this.bountyManager; }
   public ConfigManager getConfigManager() { return this.configManager; }
   public SoundManager getSoundManager() { return this.soundManager; }
   public SignManager getSignManager() { return this.signManager; }
   public HeadCacheManager getHeadCacheManager() { return this.headCacheManager; }
   public BountyToggleManager getBountyToggleManager() { return this.bountyToggleManager; }
   public Connection getDatabaseConnection() { return this.databaseConnection; }
   public Connection getHeadDatabaseConnection() { return this.headDatabaseConnection; }
}
