package me.numilani.activebuilds;

import com.bergerkiller.bukkit.common.config.FileConfiguration;
import java.sql.SQLException;
import lombok.experimental.ExtensionMethod;
import me.numilani.activebuilds.commands.BuildCommandHandler;
import me.numilani.activebuilds.data.IDataSourceConnector;
import me.numilani.activebuilds.data.SqliteDataSourceConnector;
import me.numilani.activebuilds.objects.ConfigurationContents;
import me.numilani.activebuilds.services.BuildingService;
import me.numilani.activebuilds.utils.ItemStackHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.meta.SimpleCommandMeta;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

@ExtensionMethod(ItemStackHelper.class)
public final class ActiveBuilds extends JavaPlugin {
  public LegacyPaperCommandManager<CommandSender> manager;
  public AnnotationParser<CommandSender> cmdParser;
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
      doFirstRunPluginInit();
    }

    loadConfig(false);

    initDataSource(isFirstRun);

    initPluginCommands();

    // auto-schedule event to run on the configured interval (20 ticks * 60 seconds
    // per minute *
    // checkInterval minutes)
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
    try {
      cfg = new ConfigurationContents();
      var cfgFile = new FileConfiguration(this, "config.yml");
      cfgFile.load();
      cfg.setCheckInterval(cfgFile.get("settings.checkInterval", Integer.class));
    } catch (Exception ex) {
      getLogger().severe("Failure while loading config file! Is everything formatted correctly?");
      cfg = old_cfg;
    }
  }

  private void doFirstRunPluginInit() {
    var cfgFile = new FileConfiguration(this, "config.yml");
    cfgFile.set("settings.checkInterval", 90);

    cfgFile.saveSync();
  }

  public void initDataSource(Boolean isFirstRun) {
    // do a check for datasourcetype once that's added to config
    // for now, just set datasource to sqlite always
    try {
      dataSource = new SqliteDataSourceConnector(this);
      if (isFirstRun)
        dataSource.initDatabase();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void initPluginCommands() {
    // Register commands
    try {
      manager = LegacyPaperCommandManager.createNative(this, ExecutionCoordinator.simpleCoordinator());
      cmdParser = new AnnotationParser<>(
          manager, CommandSender.class, parserParameters -> SimpleCommandMeta.empty());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    cmdParser.parse(new BuildCommandHandler(this));
  }

  @Override
  public void onDisable() {
    // Plugin shutdown logic
    checkIntervalTask.cancel();
  }
}
