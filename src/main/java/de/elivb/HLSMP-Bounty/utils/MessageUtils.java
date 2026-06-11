package de.elivb.donutBounty.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtils {
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

   public static String colorize(String message) {
      if (message == null) {
         return "";
      } else {
         Matcher matcher = HEX_PATTERN.matcher(message);
         StringBuffer buffer = new StringBuffer();

         while(matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hexCode).toString());
         }

         matcher.appendTail(buffer);
         return ChatColor.translateAlternateColorCodes('&', buffer.toString());
      }
   }

   public static void sendMessage(Player player, String message) {
      if (player != null && message != null && !message.isEmpty()) {
         player.sendMessage(colorize(message));
      }

   }

   public static void sendActionBar(Player player, String message) {
      if (player != null && message != null && !message.isEmpty()) {
         String coloredMessage = colorize(message);
         player.sendActionBar(coloredMessage);
      }

   }

   public static String formatAmount(double amount) {
      return amount == (double)((long)amount) ? String.format("%,d", (long)amount) : String.format("%,.2f", amount);
   }
}
