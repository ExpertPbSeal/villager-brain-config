plugins {
	id("java")
	id("net.neoforged.moddev") version "2.0.148"
}

version = "${property("mod.version")}-1.21.1"
group = property("mod.group") as String

base {
	archivesName.set("${property("mod.id")}-neoforge")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

neoForge {
	version = "21.1.267"
	validateAccessTransformers = true

	runs {
		create("client") {
			client()
		}
		create("server") {
			server()
		}
	}

	mods {
		create("villager_brain_config") {
			sourceSet(sourceSets.getByName("main"))
		}
	}
}

tasks.processResources {
	val tokens = mapOf(
		"id" to project.property("mod.id").toString(),
		"name" to project.property("mod.name").toString(),
		"display_name" to project.property("mod.display_name").toString(),
		"author" to project.property("mod.author").toString(),
		"github" to project.property("mod.github").toString(),
		"description" to project.property("mod.description").toString(),
		"license" to project.property("mod.license").toString(),
		"modversion" to project.property("mod.version").toString(),
	)
	inputs.properties(tokens)
	filesMatching("META-INF/neoforge.mods.toml") {
		expand(tokens)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
}
