package com.example.exampleaddon;

import com.example.exampleaddon.elements.conditions.CondExample;
import com.example.exampleaddon.elements.effects.EffAsyncExample;
import com.example.exampleaddon.elements.effects.EffExample;
import com.example.exampleaddon.elements.effects.EffSecExample;
import com.example.exampleaddon.elements.events.EvtExample;
import com.example.exampleaddon.elements.expressions.ExprExample;
import com.example.exampleaddon.elements.expressions.ExprPropertyExample;
import com.example.exampleaddon.elements.expressions.ExprSecExample;
import com.example.exampleaddon.elements.functions.ExampleFunction;
import com.example.exampleaddon.elements.structures.StructExample;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.registration.SyntaxRegistry;

public final class Elements {

	private Elements() {
	}

	public static void register(SkriptAddon addon) {
		SyntaxRegistry registry = addon.syntaxRegistry();

		ExprExample.register(registry);
		ExprPropertyExample.register(registry);
		ExprSecExample.register(registry);

		EffExample.register(registry);
		EffAsyncExample.register(registry);
		EffSecExample.register(registry, addon.registry(EventValueRegistry.class));

		CondExample.register(registry);
		EvtExample.register(registry);
		StructExample.register(registry);

		ExampleFunction.register(addon);
	}

}
