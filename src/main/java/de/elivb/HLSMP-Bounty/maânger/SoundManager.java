package de.elivb.donutBounty.manager;

import de.elivb.donutBounty.Bounty;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {
   private final Bounty plugin;

   public SoundManager(Bounty plugin) {
      this.plugin = plugin;
   }

   public void playSound(Player player, String soundKey) {
      if (player != null) {
         boolean enabled = (Boolean)this.plugin.getConfigManager().getSoundConfig().getOrDefault(soundKey + ".enabled", true);
         if (enabled) {
            String soundName = (String)this.plugin.getConfigManager().getSoundConfig().getOrDefault(soundKey + ".sound", "UI_BUTTON_CLICK");

            try {
               Sound sound = Sound.valueOf(soundName);
               player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
            } catch (IllegalArgumentException var6) {
            }

         }
      }
   }
}
