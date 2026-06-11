package de.elivb.donutBounty.manager;

import de.elivb.donutBounty.Bounty;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;

public class BountyManager {
   private final Bounty plugin;
   private final Map<UUID, Double> bountyCache;
   private final Map<UUID, Long> bountyTimestampCache;
   private final Map<UUID, Map.Entry<UUID, Map.Entry<String, Double>>> pendingBounties;

   public BountyManager(Bounty plugin) {
      this.plugin = plugin;
      this.bountyCache = new HashMap();
      this.bountyTimestampCache = new HashMap();
      this.pendingBounties = new HashMap();
      this.loadAllBounties();
   }

   private void loadAllBounties() {
      try {
         Statement stmt = this.plugin.getDatabaseConnection().createStatement();

         try {
            ResultSet rs = stmt.executeQuery("SELECT target_uuid, SUM(amount) as total, MAX(timestamp) as last FROM bounties WHERE claimed = 0 GROUP BY target_uuid");

            try {
               while(rs.next()) {
                  UUID uuid = UUID.fromString(rs.getString("target_uuid"));
                  this.bountyCache.put(uuid, rs.getDouble("total"));
                  this.bountyTimestampCache.put(uuid, rs.getLong("last"));
               }
            } catch (Throwable var7) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var8) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var5) {
                  var8.addSuppressed(var5);
               }
            }

            throw var8;
         }

         if (stmt != null) {
            stmt.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

   }

   public CompletableFuture<Boolean> addBounty(UUID target, String targetName, UUID adder, double amount) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            PreparedStatement stmt = this.plugin.getDatabaseConnection().prepareStatement("INSERT INTO bounties (target_uuid, target_name, amount, added_by, timestamp) VALUES (?, ?, ?, ?, ?)");

            Boolean var7;
            try {
               stmt.setString(1, target.toString());
               stmt.setString(2, targetName);
               stmt.setDouble(3, amount);
               stmt.setString(4, adder != null ? adder.toString() : null);
               stmt.setLong(5, System.currentTimeMillis());
               stmt.execute();
               this.bountyCache.merge(target, amount, Double::sum);
               this.bountyTimestampCache.put(target, System.currentTimeMillis());
               var7 = true;
            } catch (Throwable var10) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable x2) {
                     var10.addSuppressed(x2);
                  }
               }

               throw var10;
            }

            if (stmt != null) {
               stmt.close();
            }

            return var7;
         } catch (SQLException e) {
            e.printStackTrace();
            return false;
         }
      });
   }

   public CompletableFuture<Double> claimBounty(UUID target, UUID killer) {
      return CompletableFuture.supplyAsync(() -> {
         double total = this.getBounty(target);
         if (total <= (double)0.0F) {
            return (double)0.0F;
         } else {
            try {
               PreparedStatement stmt = this.plugin.getDatabaseConnection().prepareStatement("UPDATE bounties SET claimed = 1 WHERE target_uuid = ? AND claimed = 0");

               Double var5;
               try {
                  stmt.setString(1, target.toString());
                  stmt.execute();
                  this.bountyCache.remove(target);
                  this.bountyTimestampCache.remove(target);
                  var5 = total;
               } catch (Throwable var8) {
                  if (stmt != null) {
                     try {
                        stmt.close();
                     } catch (Throwable x2) {
                        var8.addSuppressed(x2);
                     }
                  }

                  throw var8;
               }

               if (stmt != null) {
                  stmt.close();
               }

               return var5;
            } catch (SQLException e) {
               e.printStackTrace();
               return (double)0.0F;
            }
         }
      });
   }

   public double getBounty(UUID target) {
      return (Double)this.bountyCache.getOrDefault(target, (double)0.0F);
   }

   public long getBountyTimestamp(UUID target) {
      return (Long)this.bountyTimestampCache.getOrDefault(target, 0L);
   }

   public List<Map.Entry<UUID, Double>> getTopBounties(int limit) {
      return this.bountyCache.entrySet().stream().sorted(Entry.comparingByValue().reversed()).limit((long)limit).toList();
   }

   public List<Map.Entry<UUID, Double>> getAllBountiesSorted(String sortBy) {
      Comparator<Map.Entry<UUID, Double>> comparator;
      switch (sortBy) {
         case "oldest":
            comparator = Comparator.comparing((e) -> (Long)this.bountyTimestampCache.getOrDefault(e.getKey(), 0L));
            break;
         case "highest_price":
            comparator = Entry.comparingByValue().reversed();
            break;
         case "lowest_price":
            comparator = Entry.comparingByValue();
            break;
         case "newest":
         default:
            comparator = Comparator.comparingDouble((e) -> (double)(Long)this.bountyTimestampCache.getOrDefault(e.getKey(), 0L)).reversed();
      }

      return this.bountyCache.entrySet().stream().sorted(comparator).toList();
   }

   public void addPendingBounty(UUID adder, UUID targetUUID, String targetName, double amount) {
      this.pendingBounties.put(adder, Map.entry(targetUUID, Map.entry(targetName, amount)));
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.removePendingBounty(adder), 600L);
   }

   public Map.Entry<UUID, Map.Entry<String, Double>> getPendingBounty(UUID adder) {
      return (Map.Entry)this.pendingBounties.get(adder);
   }

   public boolean hasPendingBounty(UUID adder) {
      return this.pendingBounties.containsKey(adder);
   }

   public void removePendingBounty(UUID adder) {
      this.pendingBounties.remove(adder);
   }
}
