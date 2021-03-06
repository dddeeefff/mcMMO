package com.gmail.nossr50.commands.database;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.database.DatabaseManager;
import com.gmail.nossr50.database.DatabaseManagerFactory;
import com.gmail.nossr50.datatypes.database.DatabaseType;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.runnables.database.DatabaseConversionTask;
import com.gmail.nossr50.util.player.UserManager;

public class ConvertDatabaseCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (args.length) {
            case 2:
                DatabaseType previousType = DatabaseType.getDatabaseType(args[1]);
                DatabaseType newType = mcMMO.getDatabaseManager().getDatabaseType();

                if ((newType == DatabaseType.CUSTOM && DatabaseManagerFactory.getCustomDatabaseManagerClass().getSimpleName().equalsIgnoreCase(args[1])) || previousType == newType) {
                    sender.sendMessage(LocaleLoader.getString("Commands.mcconvert.Database.Same", newType.toString()));
                    return true;
                }

                DatabaseManager oldDatabase = DatabaseManagerFactory.createDatabaseManager(previousType);

                if (previousType == DatabaseType.CUSTOM) {
                    Class<?> clazz;

                    try {
                        clazz = Class.forName(args[1]);

                        if (!DatabaseManager.class.isAssignableFrom(clazz)) {
                            sender.sendMessage(LocaleLoader.getString("Commands.mcconvert.Database.InvalidType", args[1]));
                            return true;
                        }
                    }
                    catch (Exception e) {
                        sender.sendMessage(LocaleLoader.getString("Commands.mcconvert.Database.InvalidType", args[1]));
                        return true;
                    }

                    try {
                        oldDatabase = DatabaseManagerFactory.createCustomDatabaseManager((Class<? extends DatabaseManager>) clazz);
                    }
                    catch (Throwable t) {
                        sender.sendMessage("An error occurred during the conversion process."); // TODO: Localize
                        return true;
                    }
                }

                sender.sendMessage(LocaleLoader.getString("Commands.mcconvert.Database.Start", previousType.toString(), newType.toString()));

                UserManager.saveAll();
                UserManager.clearAll();

                for (Player player : mcMMO.p.getServer().getOnlinePlayers()) {
                    PlayerProfile profile = oldDatabase.loadPlayerProfile(player.getName(), false);

                    if (profile.isLoaded()) {
                        mcMMO.getDatabaseManager().saveUser(profile);
                    }

                    UserManager.addUser(player);
                }

                new DatabaseConversionTask(oldDatabase, sender, previousType.toString(), newType.toString()).runTaskAsynchronously(mcMMO.p);
                return true;

            default:
                return false;
        }
    }
}
