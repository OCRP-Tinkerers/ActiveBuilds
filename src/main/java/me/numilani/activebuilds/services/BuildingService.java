package me.numilani.activebuilds.services;

import me.numilani.activebuilds.ActiveBuilds;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Hashtable;

public class BuildingService {
    private ActiveBuilds plugin;

    public BuildingService(ActiveBuilds plugin) {
        this.plugin = plugin;
    }

    public void runBuildingUpdate() throws SQLException {
        var buildings = plugin.dataSource.getAllBuildings();

        if (buildings.isEmpty()){
            plugin.getLogger().warning("There are no buildings to run!");
            return;
        }

        for (var bldg : buildings)
        {
            if (bldg.Type.Name.equals("TYPE_UNAVAILABLE" ) || bldg.InputLocation == null || bldg.OutputLocation == null){
                continue;
            }

            var inputChest = ((Container)bldg.InputLocation.getBlock().getState());
            var outputChest = ((Container)bldg.OutputLocation.getBlock().getState());

            // create map of items found in chest
            Hashtable<ItemStack, Boolean> hasItems = new Hashtable<>();
            for (var item : bldg.Type.getMaterialsConsumed()){
                hasItems.put(item, false);
            }

            // check for all items present
//            var allInputsPresent = true;

            for (var input : hasItems.keySet())
            {
                for (var inChest : inputChest.getInventory()){
                    if (inChest != null && inChest.getType() == input.getType() && inChest.getAmount() <= input.getAmount())
                    {
                        hasItems.put(input, true);
                    }
                }
            }

            // if all inputs are present, take the inputs and give the outputs
            if (!hasItems.containsValue(false)){
                for (var input : bldg.Type.getMaterialsConsumed())
                {
                    inputChest.getInventory().removeItem(input);
                }
                for (var output : bldg.Type.getMaterialsAwarded())
                {
                    var x = outputChest.getInventory().addItem(new ItemStack(output));
                }
            }
        }
        plugin.dataSource.setLastBuildingUpdateTime();
    }
}
