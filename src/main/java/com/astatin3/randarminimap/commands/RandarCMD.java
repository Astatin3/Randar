package com.astatin3.randarminimap.commands;

import com.astatin3.randarminimap.modules.Randar;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class RandarCMD extends Command {
    final Randar randar;

    public RandarCMD(Randar randar) {
        super("randar-clear", "Clear Randar map.");
        this.randar = randar;
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info("Map cleared!");
            this.randar.clearMap();
            return SINGLE_SUCCESS;
        });
    }
}
