package com.example.exampleaddon;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.Converters;

/**
 * Types, comparators and converters belong in the module's {@code init} phase:
 * syntax parsing may need them.
 */
public final class Types {

	private Types() {
	}

	public static void register() {
		Classes.registerClass(new ClassInfo<>(ExampleType.class, "exampletype")
			.user("example ?types?")
			.name("Example Type")
			.description("An example type added by this addon.")
			.examples("example:hello")
			.since("1.0")
			.parser(new Parser<>() {
				@Override
				public @Nullable ExampleType parse(String input, ParseContext context) {
					return input.startsWith("example:") ? new ExampleType(input.substring(8)) : null;
				}

				@Override
				public String toString(ExampleType type, int flags) {
					return type.id();
				}

				@Override
				public String toVariableNameString(ExampleType type) {
					return "exampletype:" + type.id();
				}
			}));

		Comparators.registerComparator(ExampleType.class, String.class,
				(type, string) -> type.id().equals(string) ? Relation.EQUAL : Relation.NOT_EQUAL);

		Converters.registerConverter(ExampleType.class, String.class, ExampleType::id);
	}

}
