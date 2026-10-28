package com.example.exampleaddon;

import ch.njol.skript.Skript;
import ch.njol.skript.util.Version;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;

/**
 * Minimal Skript addon for Skript 2.17+.
 *
 * <p>{@link AddonModule#name()} is abstract in 2.17. {@code init} runs before
 * {@code load}: types belong in init, syntax in load.
 */
public class ExampleAddon extends JavaPlugin implements AddonModule {

	private static final String MINIMUM_SKRIPT_VERSION = "2.17";

	@Override
	public void onEnable() {
		if (Skript.getVersion().isSmallerThan(new Version(MINIMUM_SKRIPT_VERSION))) {
			getLogger().severe("Skript " + MINIMUM_SKRIPT_VERSION + "+ is required, found "
					+ Skript.getVersion() + ". Disabling " + getName() + ".");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		SkriptAddon addon = Skript.instance().registerAddon(ExampleAddon.class, name());
		addon.loadModules(this);
	}

	@Override
	public String name() {
		return "example-addon";
	}

	@Override
	public void init(SkriptAddon addon) {
		// Call at most once; a second call throws.
		addon.localizer().setSourceDirectories("lang", null);
		Types.register();
	}

	@Override
	public void load(SkriptAddon addon) {
		Elements.register(addon);
	}

}
