package com.example.exampleaddon.elements.events;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

/**
 * Events are registered through {@link BukkitSyntaxInfos.Event} because they carry
 * Bukkit event classes and their documentation inline. They are NOT registered
 * with {@code SyntaxInfo.builder(...)}.
 */
public class EvtExample extends SkriptEvent {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			BukkitSyntaxInfos.Event.KEY,
			BukkitSyntaxInfos.Event.builder(EvtExample.class, "Example Join")
				.supplier(EvtExample::new)
				.addEvent(PlayerJoinEvent.class)
				.addPatterns("example join")
				.addDescription("Called when a player joins the server, as an example.")
				.addExample("on example join:\n\tsend \"Welcome!\" to event-player")
				.addSince("1.0")
				.build()
		);
	}

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
		return true;
	}

	@Override
	public boolean check(Event event) {
		return event instanceof PlayerJoinEvent;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "example join";
	}

}
