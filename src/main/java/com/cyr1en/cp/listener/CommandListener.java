package com.cyr1en.cp.listener;

import com.cyr1en.cp.CommandPrompter;
import com.cyr1en.cp.util.SRegex;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandListener implements Listener {

  private CommandPrompter plugin;

  public CommandListener(CommandPrompter plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    if (plugin.inCommandProcess(event.getPlayer())) {
      System.out.println("Player is still in command completion");
      String prefix = plugin.getConfiguration().getString("Prompt-Prefix");
      event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
              "&6You are still trying to complete a command. Type \"cancel\" to cancel"));
      event.setCancelled(true);
    } else {
      SRegex simpleRegex = new SRegex();
      simpleRegex.find(event.getMessage(), "<.*?>");
      List<String> prompts = simpleRegex.getResults();
      if (prompts.size() > 0) {
        event.setCancelled(true);
        plugin.registerPrompt(new Prompt(plugin, event.getPlayer(), prompts, event.getMessage()));
      }
    }
  }

}