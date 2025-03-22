package org.ginafro.notenoughfakepixel.features.skyblock.qol;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.Map;

public class Aliases {

    private static final Map<String, String> commandMap = new HashMap<>();

    static {
        for (int i = 1; i <= 7; i++) {
            commandMap.put("f" + i, "/joindungeon " + i);
            commandMap.put("m" + i, "/joindungeon " + i + " master");
        }
        String[] warps = {"isle", "dh", "hub", "end", "park", "farm","forge","dwarven","castle","da","crypts","gold","deep"};
        for (String warp : warps) {
            commandMap.put(warp, "/warp " + warp);
        }
        commandMap.put("pl", "/p list");
        commandMap.put("pd", "/p disband");
        commandMap.put("fl", "/f list");
    }

    public Aliases() {
        MinecraftForge.EVENT_BUS.register(this);

        for (String shortCmd : commandMap.keySet()) {
            ClientCommandHandler.instance.registerCommand(new AliasCommand(shortCmd, commandMap.get(shortCmd)));
        }

        ClientCommandHandler.instance.registerCommand(new VpCommand());
        ClientCommandHandler.instance.registerCommand(new PtCommand());
    }

    private static class AliasCommand extends CommandBase {
        private final String shortCommand;
        private final String fullCommand;

        public AliasCommand(String shortCommand, String fullCommand) {
            this.shortCommand = shortCommand;
            this.fullCommand = fullCommand;
        }

        @Override
        public String getCommandName() {
            return shortCommand;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/" + shortCommand;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(fullCommand);
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }
    }

    private static class VpCommand extends CommandBase {
        @Override
        public String getCommandName() {
            return "vp";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/vp <name>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length > 0) {
                String name = String.join(" ", args);
                Minecraft.getMinecraft().thePlayer.sendChatMessage("/viewprofile " + name);
            }
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }
    }

    private static class PtCommand extends CommandBase {
        @Override
        public String getCommandName() {
            return "pt";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/pt <name>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length > 0) {
                String name = String.join(" ", args);
                Minecraft.getMinecraft().thePlayer.sendChatMessage("/p transfer " + name);
            }
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }
    }
}