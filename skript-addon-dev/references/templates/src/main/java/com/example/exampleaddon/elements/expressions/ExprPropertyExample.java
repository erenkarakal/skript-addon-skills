package com.example.exampleaddon.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Example Nickname")
@Description("A read-only example property of a player.")
@Example("send \"%example nickname of player%\" to player")
@Since("1.0")
public class ExprPropertyExample extends SimplePropertyExpression<Player, String> {

	public static void register(SyntaxRegistry registry) {
		// Generates "[the] example nickname of %players%" and "%players%'[s] example nickname".
		registry.register(
			SyntaxRegistry.EXPRESSION,
			infoBuilder(ExprPropertyExample.class, String.class, "example nickname", "players", false)
				.supplier(ExprPropertyExample::new)
				.build()
		);
	}

	@Override
	public @Nullable String convert(Player player) {
		return player.getName();
	}

	@Override
	protected String getPropertyName() {
		return "example nickname";
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

}
