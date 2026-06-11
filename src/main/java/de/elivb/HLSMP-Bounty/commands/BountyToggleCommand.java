package de.elivb.donutBounty.commands;

import de.elivb.donutBounty.Bounty;
import de.elivb.donutBounty.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BountyToggleCommand implements CommandExecutor {
   private final Bounty plugin;

   public BountyToggleCommand(Bounty plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(MessageUtils.colorize(plugin.getConfigManager().getMessage("player-only")));
         return true;
      }

      boolean nowDisabled = plugin.getBountyToggleManager().toggle(player.getUniqueId());
      if (nowDisabled) {
         MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("bounty-toggle-off"));
      } else {
         MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("bounty-toggle-on"));
      }
      return true;
   }
}
