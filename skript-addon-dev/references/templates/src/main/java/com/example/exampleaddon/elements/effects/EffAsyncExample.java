package com.example.exampleaddon.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.AsyncEffect;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

/**
 * {@link AsyncEffect} runs {@link #execute(Event)} off the main thread and resumes the
 * trigger on the main thread afterward, as if a delay had been inserted.
 */
@Name("Example Async")
@Description("An example asynchronous effect. Runs off the main thread and resumes on it.")
@Example("example async \"hello\"")
@Since("1.0")
public class EffAsyncExample extends AsyncEffect {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.EFFECT,
			SyntaxInfo.builder(EffAsyncExample.class)
				.supplier(EffAsyncExample::new)
				.addPattern("example async %string%")
				.build()
		);
	}

	private Expression<String> message;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		// AsyncEffect does not report its delay for you.
		getParser().setHasDelayBefore(Kleenean.TRUE);

		// Delayed use is allowed: AsyncEffect adds its own tick and does not read
		// event state that a prior wait would invalidate.
		message = (Expression<String>) expressions[0];
		return true;
	}

	@Override
	protected void execute(Event event) {
		String message = this.message.getSingle(event);
		if (message == null)
			return;

		Bukkit.getLogger().info("[async] " + message);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "example async " + message.toString(event, debug);
	}

}
