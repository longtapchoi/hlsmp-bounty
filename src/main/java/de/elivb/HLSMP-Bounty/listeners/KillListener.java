package de.elivb.donutBounty.listeners;

import de.elivb.donutBounty.Bounty;
import de.elivb.donutBounty.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class KillListener implements Listener {
   private final Bounty plugin;

   public KillListener(Bounty plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player victim = event.getEntity();
      Player killer = victim.getKiller();
      if (killer == null) return;

      double bounty = this.plugin.getBountyManager().getBounty(victim.getUniqueId());
      if (bounty <= 0.0) return;
      if (killer.equals(victim) && !this.plugin.getConfigManager().isKillMyselfGetReward()) return;

      this.plugin.getBountyManager().claimBounty(victim.getUniqueId(), killer.getUniqueId()).thenAccept((claimedAmount) -> {
         if (claimedAmount > 0.0) {
            this.plugin.getEconomy().depositPlayer(killer, claimedAmount);

            // Thông báo cho người giết
            String msg = this.plugin.getConfigManager().getMessage("u-got-bounty-kill")
               .replace("%user_name%", victim.getName());
            MessageUtils.sendMessage(killer, msg);
            MessageUtils.sendActionBar(killer, this.plugin.getConfigManager().getActionBar("u-got-bounty-kill")
               .replace("%user_name%", victim.getName()));
            this.plugin.getSoundManager().playSound(killer, "bounty-added");

            // Thông báo cho victim — check toggle trước
            if (!this.plugin.getBountyToggleManager().isDisabled(victim.getUniqueId())) {
               String victimMsg = this.plugin.getConfigManager().getMessage("bounty-claimed-on-you")
                  .replace("%killer_name%", killer.getName());
               if (!victimMsg.isEmpty()) {
                  MessageUtils.sendMessage(victim, victimMsg);
               }
            }
         }
      });
   }
}
