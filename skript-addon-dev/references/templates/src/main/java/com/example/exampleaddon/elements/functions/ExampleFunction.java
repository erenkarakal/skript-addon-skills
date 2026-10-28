package com.example.exampleaddon.elements.functions;

import ch.njol.skript.lang.function.Functions;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.common.function.DefaultFunction;
import org.skriptlang.skript.common.function.Parameter.Modifier;

import java.util.Arrays;
import java.util.Locale;

/**
 * Functions are registered globally and cannot read the current event. Pass every
 * value the function needs as a parameter.
 */
public final class ExampleFunction {

	private ExampleFunction() {
	}

	public static void register(SkriptAddon addon) {
		Functions.register(DefaultFunction.builder(addon, "example_slug", String.class)
			.description("Turns text into a lowercase slug separated by hyphens.")
			.examples("example_slug(\"Hello, World!\") = \"hello-world\"")
			.since("1.0")
			.parameter("text", String.class)
			.build(args -> {
				String text = args.get("text");
				if (text == null)
					return null;
				return text.toLowerCase(Locale.ROOT)
					.replaceAll("[^a-z0-9]+", "-")
					.replaceAll("^-|-$", "");
			}));

		Functions.register(DefaultFunction.builder(addon, "example_repeat", String[].class)
			.description("Returns the text repeated the given number of times.")
			.examples("set {_parts::*} to example_repeat(\"ab\", 3)")
			.since("1.0")
			.parameter("text", String.class)
			.parameter("times", Number.class, Modifier.OPTIONAL)
			.build(args -> {
				String text = args.get("text");
				if (text == null)
					return new String[0];

				Number times = args.getOrDefault("times", 1);
				int count = Math.max(0, Math.min(times.intValue(), 1000));
				String[] results = new String[count];
				Arrays.fill(results, text);
				return results;
			}));
	}

}
