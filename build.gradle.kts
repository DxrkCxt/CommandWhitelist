plugins {
    id("com.gradleup.shadow") version "9.0.0" apply false
}

allprojects {
    group = "eu.endermite.commandwhitelist"
    version = "2.12.0"
}

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.velocitypowered.com/releases/")
        maven("https://repo.bsdevelopment.org/releases")
        maven("https://libraries.minecraft.net/")
        maven("https://repo.spongepowered.org/maven")
    }

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    // Replace ${project.version} in the plugin descriptors (was Maven resource filtering)
    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json")) {
            expand(mapOf("project" to props))
        }
    }
}
