package com.example.exampleaddon.elements.effects;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.List;

/**
 * An effect section works both as a plain effect and as an effect with a body.
 *
 * <pre>
 * example section "hello"
 *
 * example section "hello":
 *     send event-string to console
 * </pre>
 *
 * <p>Registered under {@link SyntaxRegistry#SECTION}, not EFFECT.
 */
@Name("Example Section Effect")
@Description("An example effect that may own a code block. Inside the block, event-string is the message.")
@Example("example section \"hello\"")
@Example("""
	example section "hello":
		send event-string to console
	""")
@Since("1.0")
public class EffSecExample extends EffectSection {

	/**
	 * Never dispatched by Bukkit; it only carries values into the body's trigger.
	 */
	public static class ExampleSectionEvent extends Event {

		private static final HandlerList HANDLERS = new HandlerList();

		private final String message;

		public ExampleSectionEvent(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		@Override
		@NotNull
		public HandlerList getHandlers() {
			return HANDLERS;
		}

	}

	public static void register(SyntaxRegistry registry, EventValueRegistry eventValues) {
		registry.register(
			SyntaxRegistry.SECTION,
			SyntaxInfo.builder(EffSecExample.class)
				.supplier(EffSecExample::new)
				.addPattern("example section %string%")
				.build()
		);

		eventValues.register(
			EventValue.builder(ExampleSectionEvent.class, String.class)
				.patterns("[the] event-string")
				.getter(ExampleSectionEvent::getMessage)
				.build()
		);
	}

	private Expression<String> message;

	@Nullable
	private Trigger trigger;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult,
			@Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
		message = (Expression<String>) expressions[0];

		// EffectSection sets hasSection() before this method runs.
		if (hasSection()) {
			assert sectionNode != null;
			// Returns a detached trigger whose argument is the event in scope for the body.
			trigger = SectionUtils.loadLinkedCode("example section", (beforeLoading, afterLoading) ->
				loadCode(sectionNode, "example section", beforeLoading, afterLoading, ExampleSectionEvent.class));
			return trigger != null; // a parse error in the body is already reported
		}

		return true;
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		String message = this.message.getSingle(event);
		if (message == null)
			return super.walk(event, false);

		if (trigger != null) {
			ExampleSectionEvent sectionEvent = new ExampleSectionEvent(message);
			Variables.withLocalVariables(event, sectionEvent, () -> TriggerItem.walk(trigger, sectionEvent));
		} else {
			Bukkit.getLogger().info("[section] " + message);
		}

		// false: this section has no own code, so continue with the next item.
		return super.walk(event, false);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "example section " + message.toString(event, debug);
	}

}
