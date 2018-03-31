package de.epiceric.shopchest.listeners;

import de.epiceric.shopchest.ShopChest;
import net.minecraft.server.v1_12_R1.ChatMessageType;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;


import java.util.UUID;


public class PointerListener implements Runnable {

    private ShopChest plugin;
    private int tick = 0;
    private boolean inLoop = false;
    public PointerListener(ShopChest plugin) {
        this.plugin = plugin;
    }


    @Override
    public void run() {
        if (inLoop)
        {
            return;
        }
        try {
            inLoop = true;
            for (UUID playerID : plugin.pointerMap.keySet()) {
                Player player = Bukkit.getPlayer(playerID);
                if (player.isSneaking()) {
                    plugin.pointerMap.remove(player.getUniqueId());
                    return;
                }

                Location shopLoc = plugin.pointerMap.get(player.getUniqueId());
                Location playerLoc = player.getLocation();
                if (playerLoc.getWorld().getName().equals(shopLoc.getWorld().getName())) {
                    double d = playerLoc.distance(shopLoc);
                    double vectorX = (shopLoc.getBlockX() - playerLoc.getBlockX()) / d;
                    double vectorY = (shopLoc.getBlockY() - playerLoc.getBlockY()) / d;
                    double vectorZ = (shopLoc.getBlockZ() - playerLoc.getBlockZ()) / d;

                    tick++;
                    double i = tick;
                    if (tick > 30 || i / 2 >= d) {
                        tick = 0;
                    }
                    //for(double i = 0; i< 30; i++)
                    {
                        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(vectorX * (i / 2), 1 + vectorY * (i / 2), vectorZ * (i / 2)), 1);
                    }

                    int distance = (int) Math.round(player.getLocation().distance(plugin.pointerMap.get(player.getUniqueId())));
                    String Message = ChatColor.GREEN + "Your Shop is " + ChatColor.WHITE + distance + ChatColor.GREEN + " blocks away. (Sneak to stop tracking)";


                    IChatBaseComponent comp = IChatBaseComponent.ChatSerializer
                            .a("{\"text\":\"" + Message + ChatColor.WHITE + "\"}");
                    PacketPlayOutChat packet = new PacketPlayOutChat(comp, ChatMessageType.GAME_INFO);
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                }
            }
        }
        finally {
            inLoop = false;
        }
    }
}
