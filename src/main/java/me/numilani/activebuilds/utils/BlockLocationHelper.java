package me.numilani.activebuilds.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BlockLocationHelper {

    public static String getSerializedLocation(Location loc) { 
        return loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getWorld().getUID();
    }

    public static Location getDeserializedLocation(String s) {
        String [] parts = s.split(";");
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        UUID u = UUID.fromString(parts[3]);
        World w = Bukkit.getServer().getWorld(u);
        return new Location(w, x, y, z); 
    }

    public static boolean hasSpaceFor(Inventory inv, ItemStack itemToAdd){
        int foundcount = itemToAdd.getAmount();
        for (ItemStack stack : inv.getContents()) {
            if (stack == null) foundcount -= itemToAdd.getMaxStackSize();
            if (stack.getType() == itemToAdd.getType()) {
                if (stack.getDurability() == itemToAdd.getDurability()) {
                    foundcount -= itemToAdd.getMaxStackSize() - stack.getAmount();
                }
            }
        }
        return foundcount <= 0;
    }
}
