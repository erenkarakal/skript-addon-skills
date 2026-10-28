package com.example.exampleaddon.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Example Announce")
@Description("Sends an example message to the given players.")
@Example("example announce \"Hello!\"")
@Example("example announce \"Hello!\" to all players")
@Since("1.0")
public class EffExample extends Effect {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.EFFECT,
			SyntaxInfo.builder(EffExample.class)
				.supplier(EffExample::new)
				.addPattern("example announce %string% [to %players%]")
				.build()
		);
	}

	private Expression<String> message;
	private @Nullable Expression<Player> targets;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		message = (Expression<String>) expressions[0];
		// Optional parts yield null entries in expressions - always null-check them.
		targets = (Expression<Player>) expressions[1];
		return true;
	}

	@Override
	protected void execute(Event event) {
		String message = this.message.getSingle(event);
		if (message == null)
			return;

		Player[] targets;
		if (this.targets == null) {
			// No explicit target: fall back to the event's player, if any.
			Player player = event instanceof PlayerEvent playerEvent
					? playerEvent.getPlayer()
					: null;
			if (player == null) {
				error("No target players and no player in this event");
				return;
			}
			targets = new Player[]{player};
		} else {
			targets = this.targets.getArray(event);
		}

		for (Player target : targets) {
			target.sendMessage(Component.text(message));
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return new SyntaxStringBuilder(event, debug)
				.append("example announce", message)
				.appendIf(targets != null, "to", targets)
				.toString();
	}

}
