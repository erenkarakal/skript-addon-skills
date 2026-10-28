package com.example.exampleaddon.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Player;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Example Flying")
@Description("Whether a player is flying.")
@Example("if player is example flying:")
@Since("1.0")
public class CondExample extends PropertyCondition<Player> {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.CONDITION,
			infoBuilder(CondExample.class, PropertyType.BE, "example flying", "players")
				.supplier(CondExample::new)
				.build()
		);
	}

	@Override
	public boolean check(Player player) {
		return player.isFlying();
	}

	@Override
	protected String getPropertyName() {
		return "example flying";
	}

}
