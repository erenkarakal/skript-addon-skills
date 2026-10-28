import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Keep the Skript repository LAST. Skript is not published to Maven Central.
    maven("https://repo.skriptlang.org/releases")
}

dependencies {
    // Paper API: must match the Minecraft version you target.
    compileOnly("io.papermc.paper:paper-api:26.3.build.226-stable")

    // Skript: compileOnly. NEVER shade or bundle Skript.
    compileOnly("com.github.SkriptLang:Skript:2.17.1") {
        // Skript's transitive dependencies are provided by the Skript plugin at runtime.
        isTransitive = false
    }

    compileOnly("org.jetbrains:annotations:24.1.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        filter<ReplaceTokens>("tokens" to mapOf("version" to project.version.toString()))
    }
}

tasks.jar {
    archiveFileName.set("${project.name}-${project.version}.jar")
}
