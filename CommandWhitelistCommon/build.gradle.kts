description = "CommandWhitelist-Common"

dependencies {
    api("net.kyori:adventure-api:4.23.0")
    implementation("com.github.thatsmusic99:ConfigurationMaster-API:v2.0.0-rc.3")

    compileOnly("net.kyori:adventure-text-minimessage:4.23.0")
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("io.github.waterfallmc:waterfall-api:1.21-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
