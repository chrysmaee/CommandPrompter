/*
 * MIT License
 *
 * Copyright (c) 2020 Ethan Bacurio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cyr1en.commandprompter;

import com.cyr1en.commandprompter.commands.Cancel;
import com.cyr1en.commandprompter.config.PromptConfig;
import com.cyr1en.commandprompter.command.CommodoreRegistry;
import com.cyr1en.commandprompter.commands.Reload;
import com.cyr1en.commandprompter.config.CommandPrompterConfig;
import com.cyr1en.commandprompter.config.ConfigurationManager;
import com.cyr1en.commandprompter.listener.CommandListener;
import com.cyr1en.commandprompter.listener.ModifiedListener;
import com.cyr1en.commandprompter.listener.VanillaListener;
import com.cyr1en.commandprompter.prompt.PromptManager;
import com.cyr1en.commandprompter.prompt.PromptResponseListener;
import com.cyr1en.commandprompter.prompt.ui.SkullCache;
import com.cyr1en.commandprompter.unsafe.CommandMapHacker;
import com.cyr1en.commandprompter.unsafe.ModifiedCommandMap;
import com.cyr1en.commandprompter.unsafe.PvtFieldMutator;
import com.cyr1en.kiso.mc.I18N;
import com.cyr1en.kiso.mc.UpdateChecker;
import com.cyr1en.kiso.mc.command.CommandManager;
import com.cyr1en.kiso.utils.SRegex;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

import org.bstats.bukkit.Metrics;

public class CommandPrompter extends JavaPlugin {

    private static CommandPrompter instance;

    private ConfigurationManager configManager;
    private CommandPrompterConfig config;
    private PromptConfig promptConfig;

    private PluginLogger logger;
    private CommandManager commandManager;
    private CommandListener commandListener;
    private I18N i18n;
    private UpdateChecker updateChecker;
    private PromptManager promptManager;
    private PluginMessenger messenger;

    @Override
    public void onEnable() {
        new Metrics(this, 5359);
        setupConfig();
        logger = new PluginLogger(this, "CommandPrompter");
        logger.setDebugMode(config.debugMode());
        i18n = new I18N(this, "CommandPrompter");
        setupUpdater();
        setupCommands();
        initPromptSystem();
        messenger = new PluginMessenger(config.promptPrefix());
        instance = this;
    }

    @Override
    public void onDisable() {
        promptManager.clearPromptRegistry();
        getPluginLogger().ansiUninstall();
        if (Objects.nonNull(updateChecker) && !updateChecker.isDisabled())
            HandlerList.unregisterAll(updateChecker);
    }

    private void initPromptSystem() {
        promptManager = new PromptManager(this);
        initCommandListener();
        Bukkit.getPluginManager().registerEvents(new PromptResponseListener(promptManager, this), this);
        Bukkit.getPluginManager().registerEvents(new SkullCache(this), this);
    }

    /**
     * Function to initialize the command listener that this plugin will use
     * <p>
     * If unsafe is enabled in the config, this plugin will use the modified
     * command map. Otherwise, it will just use the vanilla listener.
     */
    private void initCommandListener() {
        var useUnsafe = config.enableUnsafe();
        if (!useUnsafe) {
            commandListener = new VanillaListener(promptManager);
            Bukkit.getPluginManager().registerEvents(commandListener, this);
            return;
        }
        var delay = (long) config.modificationDelay();
        Bukkit.getScheduler().runTaskLater(this, this::hackMap, delay);
    }

    private void hackMap() {
        try {
            var mapHacker = new CommandMapHacker(this);

            var newCommandMap = new ModifiedCommandMap(getServer(), this);
            mapHacker.hackCommandMapIn(getServer(), newCommandMap);
            mapHacker.hackCommandMapIn(getServer().getPluginManager(), newCommandMap);

            commandListener = new ModifiedListener(promptManager);

            var mutator = new PvtFieldMutator();
            var sHash = mutator.forField("commandMap").in(getServer()).getHashCode();
            var pHash = mutator.forField("commandMap").in(getServer().getPluginManager()).getHashCode();
            logger.warn("sHash: " + sHash + " | pHash: " + pHash);
            Bukkit.getPluginManager().registerEvents(commandListener, this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupConfig() {
        configManager = new ConfigurationManager(this);
        config = configManager.getConfig(CommandPrompterConfig.class);
        promptConfig = configManager.getConfig(PromptConfig.class);
    }

    private void setupCommands() {
        setupCommandManager();
        commandManager.registerCommand(Reload.class);
        commandManager.registerCommand(Cancel.class);
        PluginCommand command = getCommand("commandprompter");
        Objects.requireNonNull(command).setExecutor(commandManager);
        commandManager.registerTabCompleter(command);
        CommodoreRegistry.register(this, command);
    }

    private void setupCommandManager() {
        var cmgBuilder = new CommandManager.Builder();
        cmgBuilder.plugin(this);
        cmgBuilder.setPrefix(getConfig().getString("Prompt-Prefix"));
        cmgBuilder.setPlayerOnlyMessage(getI18N().getProperty("CommandPlayerOnly"));
        cmgBuilder.setCommandInvalidMessage(getI18N().getProperty("CommandInvalid"));
        cmgBuilder.setNoPermMessage(getI18N().getFormattedProperty("CommandNoPerm"));
        cmgBuilder.setFallBack(context -> {
            getCommandManager().getMessenger().sendMessage(context.getSender(),
                    getI18N().getFormattedProperty("PluginVersion", getDescription().getVersion()));
            UpdateChecker uC = getUpdateChecker();
            if (!uC.isDisabled() && uC.newVersionAvailable())
                uC.sendUpdateAvailableMessage(context.getSender());
            return false;
        });
        commandManager = cmgBuilder.build();
    }

    private void setupUpdater() {
        updateChecker = new UpdateChecker(this, 47772);
        if (updateChecker.isDisabled()) return;
        Bukkit.getServer().getScheduler().runTaskAsynchronously(this, () -> {
            if (updateChecker.newVersionAvailable())
                logger.info(SRegex.ANSI_GREEN + "A new update is available! (" +
                        updateChecker.getCurrVersion().asString() + ")" + SRegex.ANSI_RESET);
            else
                logger.info("No update was found.");
        });
        Bukkit.getPluginManager().registerEvents(updateChecker, this);
    }

    public I18N getI18N() {
        return i18n;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public PluginMessenger getMessenger() {
        return messenger;
    }

    public PromptManager getPromptManager() {
        return promptManager;
    }

    public PluginLogger getPluginLogger() {
        return logger;
    }

    public void reload(boolean clean) {
        config = configManager.reload(CommandPrompterConfig.class);
        promptConfig = configManager.reload(PromptConfig.class);
        messenger.setPrefix(config.promptPrefix());
        logger = new PluginLogger(this, "CommandPrompter");
        i18n = new I18N(this, "CommandPrompter");
        commandManager.getMessenger().setPrefix(config.promptPrefix());
        setupUpdater();
        if (clean)
            promptManager.clearPromptRegistry();
    }

    public static CommandPrompter getInstance() {
        return instance;
    }

    public CommandPrompterConfig getConfiguration() {
        return config;
    }

    public PromptConfig getPromptConfig() {
        return promptConfig;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}

