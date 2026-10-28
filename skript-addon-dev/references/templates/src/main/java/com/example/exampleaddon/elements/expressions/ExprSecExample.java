package com.example.exampleaddon.elements.expressions;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import com.example.exampleaddon.ExampleType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.List;

/**
 * A section expression: an expression that can own a code block.
 *
 * <pre>
 * set {_value} to a new example section:
 *     # this body belongs to the expression, not to set
 * </pre>
 *
 * Registered under {@link SyntaxRegistry#EXPRESSION}, not SECTION.
 */
@Name("Example Section")
@Description("A minimal section expression that owns a code block.")
@Example("""
	set {_value} to a new example section:
		example announce "inside the section" to all players
	""")
@Since("1.0")
public class ExprSecExample extends SectionExpression<ExampleType> {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.EXPRESSION,
			SyntaxInfo.Expression.builder(ExprSecExample.class, ExampleType.class)
				.supplier(ExprSecExample::new)
				.addPattern("a new example section")
				.build()
		);
	}

	@Override
	public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, ParseResult result,
			@Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
		// node is null when the user did not write a section, or when a non-section-only
		// expression is used without one.
		if (node != null) {
			loadCode(node);
		}
		return true;
	}

	@Override
	protected ExampleType @Nullable [] get(Event event) {
		if (!runSection(event))
			return new ExampleType[0];
		return new ExampleType[]{new ExampleType("example")};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public boolean isSectionOnly() {
		// true: the ":" body is mandatory and the expression cannot be used bare.
		return true;
	}

	@Override
	public Class<? extends ExampleType> getReturnType() {
		return ExampleType.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "a new example section";
	}

}
