package me.becja10.SimpleSilkSpawners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleSilkSpawners extends JavaPlugin implements Listener{

	public static SimpleSilkSpawners instance;
	public final static Logger logger = Logger.getLogger("Minecraft");

	
	private String configPath;
	private FileConfiguration config;
	private FileConfiguration outConfig;
	
	//Config Settings
	public ArrayList<World> worlds; private String _worlds = "EnabledWorlds";
	
	private void loadConfig(){
		configPath = this.getDataFolder().getAbsolutePath() + File.separator + "config.yml";
		config = YamlConfiguration.loadConfiguration(new File(configPath));
		outConfig = new YamlConfiguration();
		
		List<String> configWorlds = config.getStringList(_worlds);
		if(configWorlds == null)
			configWorlds = new ArrayList<String>();
		
		worlds = new ArrayList<World>();		
		for(String cw: configWorlds){
			World world = getServer().getWorld(cw);
			if(world == null)
				logger.warning("There is no world named: " + cw + ". Check your config.yml");
			else
				worlds.add(world);			
		}
		
		outConfig.set(_worlds, configWorlds);
		
		saveConfig(outConfig, configPath);		
	}
	
	private void saveConfig(FileConfiguration config, String path)
	{
        try{config.save(path);}
        catch(IOException exception){logger.info("Unable to write to the configuration file at \"" + path + "\"");}
	}
	
	@Override
	public void onEnable(){
		PluginDescriptionFile pdfFile = this.getDescription();
		PluginManager manager = getServer().getPluginManager();

		logger.info(pdfFile.getName() + " Version " + pdfFile.getVersion() + " has been enabled!");
		instance = this;		
		
		manager.registerEvents(this, this);
		
		loadConfig();		
	}
		
	@Override
	public void onDisable(){
		PluginDescriptionFile pdfFile = this.getDescription();
		logger.info(pdfFile.getName() + " Has Been Disabled!");
		saveConfig(outConfig, configPath);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBreak(BlockBreakEvent event){
		Block block = event.getBlock();
		Player player = event.getPlayer();
		if(block.getType() != Material.MOB_SPAWNER || 
				player.getGameMode() == GameMode.CREATIVE ||
				!player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH) ||
				!worlds.contains(event.getBlock().getWorld()) || 
				!player.hasPermission("sss.break")){
			return;
		}
		
		event.setExpToDrop(0);
		CreatureSpawner spawner = (CreatureSpawner)block.getState();
		
		ItemStack toDrop = new ItemStack(block.getType(), 1);
		ItemMeta meta = toDrop.getItemMeta();
		
		ArrayList<String> lore = new ArrayList<String>();
		lore.add(spawner.getCreatureTypeName());
		
		meta.setLore(lore);
		toDrop.setItemMeta(meta);
		
		player.getWorld().dropItemNaturally(block.getLocation(), toDrop);		
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlace(BlockPlaceEvent event){
		Block block = event.getBlock();
		if(block.getType() == Material.MOB_SPAWNER){
			String mob = "";
			try{
				mob = event.getItemInHand().getItemMeta().getLore().get(0);
			}
			catch(Exception e){
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + "There is a problem with this spawner. Please contact staff.");
				return;
			}
			if(!mob.isEmpty())
				setSpawner(event.getBlock(), mob);
		}
	}
	
	private void setSpawner (final Block block, final String mob){
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
			public void run() {
				CreatureSpawner spawner = (CreatureSpawner)block.getState();
				spawner.setCreatureTypeByName(mob);
				spawner.update();				
			}				
		});
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		switch(cmd.getName().toLowerCase()){ 
			case "sssreload":
				if(sender instanceof Player && !sender.hasPermission("sss.admin"))
					sender.sendMessage(ChatColor.RED + "No.");
				else{
					loadConfig();
					sender.sendMessage(ChatColor.GREEN + "SSS config reloaded.");
				}
				return true;
		}
		return true;
	}	
}

