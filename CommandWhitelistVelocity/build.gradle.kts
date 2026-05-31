plugins {
    id("com.gradleup.shadow")
}

description = "CommandWhitelist-Velocity"

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

    implementation(project(":CommandWhitelistCommon"))
    implementation("org.bstats:bstats-velocity:3.0.2")
}

tasks.shadowJar {
    archiveFileName.set("CommandWhitelist-Velocity-${project.version}.jar")
    relocate("org.bstats", "eu.endermite.bstats")
    mergeServiceFiles()
    // Adventure and annotations are provided by Velocity itself — don't bundle them.
    dependencies {
        exclude(dependency("net.kyori:.*:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
        exclude(dependency("org.intellij.lang:annotations:.*"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
