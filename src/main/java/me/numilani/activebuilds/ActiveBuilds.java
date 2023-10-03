package me.numilani.activebuilds;

import com.bergerkiller.bukkit.common.cloud.CloudSimpleHandler;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import lombok.experimental.ExtensionMethod;
import me.numilani.activebuilds.commands.BuildCommandHandler;
import me.numilani.activebuilds.data.IDataSourceConnector;
import me.numilani.activebuilds.data.SqliteDataSourceConnector;
import me.numilani.activebuilds.objects.ConfigurationContents;
import me.numilani.activebuilds.services.BuildingService;
import me.numilani.activebuilds.utils.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.Arrays;

@ExtensionMethod(ItemStackHelper.class)
public final class ActiveBuilds extends JavaPlugin {
    public CloudSimpleHandler cmdHandler = new CloudSimpleHandler();
    public ConfigurationContents cfg = new ConfigurationContents();
    public IDataSourceConnector dataSource;
    public BuildingService buildingService = new BuildingService(this);

    private BukkitTask checkIntervalTask;

    @Override
    public void onEnable() {
        // First run setup
        var isFirstRun = false;
        if (!(new FileConfiguration(this, "config.yml").exists())) {
            isFirstRun = true;
            doPluginInit();
        }

        loadConfig(false);

        // do a check for datasourcetype once that's added to config
        // for now, just set datasource to sqlite always
        try {
            dataSource = new SqliteDataSourceConnector(this);
            if (isFirstRun) dataSource.initDatabase();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Register events
//        getServer().getPluginManager().registerEvents();

        // Register commands
        cmdHandler.enable(this);
        cmdHandler.getParser().parse(new BuildCommandHandler(this));

        // auto-schedule event to run on the configured interval (20 ticks * 60 seconds per minute * checkInterval minutes)
        var scheduler = getServer().getScheduler();
        checkIntervalTask = scheduler.runTaskTimer(this, () -> {
            try {
                buildingService.runBuildingUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, 20L, 20L * cfg.getCheckInterval() * 60L);

    }

    public void loadConfig(boolean isReload) {
        // store old cfg for if something goes wrong
        var old_cfg = cfg;
        // ensures cfg is cleared out
        cfg = new ConfigurationContents();
        var cfgFile = new FileConfiguration(this, "config.yml");
        cfgFile.load();

        try{
            cfg.setCheckInterval(cfgFile.get("settings.checkInterval", Integer.class));
            cfg.loadBuildings(cfgFile.getNode("buildings").getNodes());
        }
        catch (Exception ex){
            getLogger().severe("Failure while loading config file! Is everything formatted correctly?");
//            if (isReload){
//                cfg = old_cfg;
//            }
//            else{
//                getServer().getPluginManager().disablePlugin(this);
//            }
        }
    }

    private void doPluginInit() {
        var cfgFile = new FileConfiguration(this, "config.yml");
        cfgFile.set("settings.checkInterval", 90);

        cfgFile.set("buildings.testMine.name", "mine_stone-iron");
        cfgFile.set("buildings.testMine.inputs", Arrays.asList(new ItemStack(Material.STONE_PICKAXE, 1).toSimpleString(), new ItemStack(Material.STONE_PICKAXE, 1).toSimpleString(), new ItemStack(Material.STONE_PICKAXE, 1).toSimpleString(), new ItemStack(Material.TORCH, 8).toSimpleString()));
        cfgFile.set("buildings.testMine.outputs", Arrays.asList(new ItemStack(Material.COBBLESTONE, 192).toSimpleString(), new ItemStack(Material.IRON_ORE, 14).toSimpleString()));

        cfgFile.set("buildings.testMill.name", "lumbermill_spruce");
        cfgFile.set("buildings.testMill.inputs", Arrays.asList(new ItemStack(Material.STONE_AXE, 1).toSimpleString(), new ItemStack(Material.STONE_AXE, 1).toSimpleString(), new ItemStack(Material.STONE_AXE, 1).toSimpleString(), new ItemStack(Material.SPRUCE_SAPLING, 8).toSimpleString()));
        cfgFile.set("buildings.testMill.outputs", Arrays.asList(new ItemStack(Material.SPRUCE_LOG, 128).toSimpleString(), new ItemStack(Material.SPRUCE_LEAVES, 64).toSimpleString()));

        cfgFile.saveSync();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        checkIntervalTask.cancel();
    }
}
