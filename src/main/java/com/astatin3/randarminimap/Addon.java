package com.astatin3.randarminimap;

import com.astatin3.randarminimap.modules.Randar;
import com.astatin3.randarminimap.commands.RandarCMD;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Exploits");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Randar Minimap");

        // Modules
        final Randar r = new Randar();
        Modules.get().add(r);

        // Commands
        Commands.add(new RandarCMD(r));
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.astatin3.randarminimap";
    }
}
