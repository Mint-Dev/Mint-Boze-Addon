package me.otter.mintaddon;

import com.google.gson.JsonObject;
import dev.boze.api.BozeInstance;
import dev.boze.api.addon.Addon;
import dev.boze.api.addon.module.ToggleableModule;
import me.otter.mintaddon.modules.AntiPistonPush;
import me.otter.mintaddon.modules.AutoSupportPlace;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MintAddon extends Addon {

    public static final String ID = "mintaddon";
    public static final String NAME = "Mint Addon";
    public static final String DESCRIPTION = "$wag Boze Addon";
    public static final String VERSION = "1.0.3";

    public static final MintAddon INSTANCE = new MintAddon();
    public static final Logger LOG = LogManager.getLogger();
    public static MinecraftClient mc;

    public MintAddon() {
        super(ID, NAME, DESCRIPTION, VERSION);
    }

    @Override
    public boolean initialize() {
        LOG.info("Initializing " + name);
        mc = MinecraftClient.getInstance();
        BozeInstance.INSTANCE.registerPackage("me.otter.mintaddon");

        AutoSupportPlace autoSupportPlace = new AutoSupportPlace();
        AntiPistonPush antiPistonPush = new AntiPistonPush();

        modules.add(autoSupportPlace);
        modules.add(antiPistonPush);

        LOG.info("Successfully initialized " + name);

        return super.initialize();
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();

        JsonObject modulesObject = new JsonObject();

        for (ToggleableModule module : modules) {
            modulesObject.add(module.getName(), module.toJson());
        }

        object.add("modules", modulesObject);

        return object;
    }

    @Override
    public Addon fromJson(JsonObject jsonObject) {
        if (!jsonObject.has("modules")) {
            return this;
        }

        JsonObject modulesObject = jsonObject.getAsJsonObject("modules");

        for (ToggleableModule module : modules) {
            if (modulesObject.has(module.getName())) {
                module.fromJson(modulesObject.getAsJsonObject(module.getName()));
            }
        }

        return this;
    }
}
