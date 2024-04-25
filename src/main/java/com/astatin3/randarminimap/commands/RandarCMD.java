package com.astatin3.randarminimap.commands;

import com.astatin3.randarminimap.modules.Randar;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class RandarCMD extends Command {
    final Randar randar;

    public RandarCMD(Randar randar) {
        super("randar", "Randar utility commands.");
        this.randar = randar;
    }

    private void printHelpInfo(){
        info("\n" +
            "Randar Help\n" +
            "\n" +
            ".randar help - print this help info\n" +
            ".randar clear - clear map data\n" +
            ".randar getseed - print current seed\n" +
            ".rander setseed <seed> - set current seed\n");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(literal("clear").executes(context -> {
            info("Map cleared!");
            this.randar.clearMap();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("getseed").executes(context -> {
            info("Current seed: " + this.randar.getSeed());
            return SINGLE_SUCCESS;
        }));

        builder.then(
            literal("setseed")
            .then(argument("seed", StringArgumentType.greedyString())
            .executes(context -> {
                final String newSeed;
                try{
                    newSeed = context.getArgument("seed", String.class);
                }catch (Exception e){
                    info("Error parsing seed");
                    return SINGLE_SUCCESS;
                }

                try{
                    Long.parseLong(context.getArgument("seed", String.class));
                }catch (Exception e){
                    info(newSeed + " is not a valid seed!");
                    return SINGLE_SUCCESS;
                }

                info("Previous seed: " + this.randar.getSeed());
                this.randar.setSeed(newSeed);
                info("Current seed: " + this.randar.getSeed());
                return SINGLE_SUCCESS;
            }
        )));

        builder.executes(context -> {
            printHelpInfo();
            return SINGLE_SUCCESS;
        });

//        builder.executes(context -> {
//            info("Map cleared!");
//            this.randar.clearMap();
//            return SINGLE_SUCCESS;
//        });
    }
}
