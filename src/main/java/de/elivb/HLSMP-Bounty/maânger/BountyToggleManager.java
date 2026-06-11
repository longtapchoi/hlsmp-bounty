package de.elivb.donutBounty.manager;

import de.elivb.donutBounty.Bounty;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BountyToggleManager {
   private final Bounty plugin;
   private final Set<UUID> disabledPlayers = new HashSet<>();
   private final File dataFile;
   private YamlConfiguration config;

   public BountyToggleManager(Bounty plugin) {
      this.plugin = plugin;
      this.dataFile = new File(plugin.getDataFolder(), "bounty-toggle.yml");
      load();
   }

   private void load() {
      if (!dataFile.exists()) {
         config = new YamlConfiguration();
         return;
      }
      config = YamlConfiguration.loadConfiguration(dataFile);
      for (String uuid : config.getStringList("disabled")) {
         try {
            disabledPlayers.add(UUID.fromString(uuid));
         } catch (IllegalArgumentException ignored) {}
      }
   }

   private void save() {
      config.set("disabled", disabledPlayers.stream().map(UUID::toString).toList());
      try {
         config.save(dataFile);
      } catch (IOException e) {
         plugin.getLogger().severe("Lỗi lưu bounty-toggle.yml: " + e.getMessage());
      }
   }

   public boolean isDisabled(UUID uuid) {
      return disabledPlayers.contains(uuid);
   }

   public boolean toggle(UUID uuid) {
      boolean nowDisabled;
      if (disabledPlayers.contains(uuid)) {
         disabledPlayers.remove(uuid);
         nowDisabled = false;
      } else {
         disabledPlayers.add(uuid);
         nowDisabled = true;
      }
      save();
      return nowDisabled;
   }
}
