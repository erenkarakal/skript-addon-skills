package com.example.exampleaddon.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Example Food Level")
@Description("The food level of a player.")
@Example("send \"%example food level of player%\" to player")
@Since("1.0")
public class ExprExample extends SimpleExpression<Integer> {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.EXPRESSION,
			SyntaxInfo.Expression.builder(ExprExample.class, Integer.class)
				.supplier(ExprExample::new)
				.addPattern("example food level of %player%")
				.build()
		);
	}

	private Expression<Player> player;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		player = (Expression<Player>) expressions[0];
		return true;
	}

	@Override
	protected Integer[] get(Event event) {
		Player player = this.player.getSingle(event);
		if (player == null)
			return new Integer[0];
		return new Integer[]{player.getFoodLevel()};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Integer> getReturnType() {
		return Integer.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "example food level of " + player.toString(event, debug);
	}

}
