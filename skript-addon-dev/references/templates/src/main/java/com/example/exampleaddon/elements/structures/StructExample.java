package com.example.exampleaddon.elements.structures;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Example Structure")
@Description("A minimal section structure that does nothing.")
@Example("""
	example section:
		stop
	""")
@Since("1.0")
public class StructExample extends Structure {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.STRUCTURE,
			SyntaxInfo.Structure.builder(StructExample.class)
				.supplier(StructExample::new)
				.addPattern("example section")
				.nodeType(SyntaxInfo.Structure.NodeType.SECTION)
				.build()
		);
	}

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult,
						@Nullable EntryContainer entryContainer) {
		// entryContainer is null for simple structures or when no EntryValidator is set.
		return true;
	}

	@Override
	public boolean load() {
		return true;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "example section";
	}

}
