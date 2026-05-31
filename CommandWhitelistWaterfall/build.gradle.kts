plugins {
    id("com.gradleup.shadow")
}

description = "CommandWhitelist-Waterfall"

dependencies {
    compileOnly("io.github.waterfallmc:waterfall-api:1.21-R0.1-SNAPSHOT")

    implementation(project(":CommandWhitelistCommon"))
    implementation("net.kyori:adventure-platform-bungeecord:4.4.0")
    implementation("net.kyori:adventure-text-minimessage:4.23.0")
    implementation("org.bstats:bstats-bungeecord:3.0.2")
}

tasks.shadowJar {
    archiveFileName.set("CommandWhitelist-Waterfall-${project.version}.jar")
    relocate("org.bstats", "eu.endermite.bstats")
    relocate("net.kyori", "eu.endermite")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
