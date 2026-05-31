plugins {
    id("com.gradleup.shadow")
}

description = "CommandWhitelist-Bukkit"

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    implementation(project(":CommandWhitelistCommon"))
    implementation("net.kyori:adventure-platform-bukkit:4.4.1-SNAPSHOT")
    implementation("net.kyori:adventure-text-minimessage:4.23.0")
    implementation("org.bstats:bstats-bukkit:3.0.2")
}

tasks.shadowJar {
    archiveFileName.set("CommandWhitelist-Bukkit-${project.version}.jar")
    relocate("net.kyori", "eu.endermite.net.kyori")
    relocate("org.bstats", "eu.endermite.bstats")
    mergeServiceFiles()
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
