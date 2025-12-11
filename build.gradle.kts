plugins {
    `java-library`
    id("com.gradleup.shadow") version("8.3.0")
    id("xyz.jpenilla.run-paper") version("2.2.4")
}

group = "org.lushplugins"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven("https://repo.lushplugins.org/snapshots/") // LushLib, PlaceholderHandler

    maven("https://repo.codemc.io/repository/maven-public/") // PlaceholderAPI
    maven("https://repo.dmulloy2.net/repository/public/") // ProtocolLib
    maven("https://jitpack.io/")
}

dependencies {
    // Dependencies
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("org.xerial:sqlite-jdbc:3.46.0.0")

    // Soft Dependencies
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Libraries
    implementation("org.lushplugins:LushLib:0.10.79")
    implementation("de.tr7zw:item-nbt-api:2.12.4")
    implementation("com.zaxxer:HikariCP:7.0.0")
    implementation("io.github.revxrsal:lamp.common:4.0.0-rc.12")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.12")
    implementation("org.lushplugins:PlaceholderHandler:1.0.0-alpha6")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))

    registerFeature("optional") {
        usingSourceSet(sourceSets["main"])
    }

    withSourcesJar()
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    shadowJar {
        relocate("de.tr7zw.changeme.nbtapi", "org.lushplugins.pinata.libs.nbtapi")

        minimize()

        archiveFileName.set("${project.name}-${project.version}.jar")
    }

    processResources{
        filesMatching("plugin.yml") {
            expand(project.properties)
        }

        inputs.property("version", rootProject.version)
        filesMatching("plugin.yml") {
            expand("version" to rootProject.version)
        }
    }

    runServer {
        minecraftVersion("1.21.10")
    }
}
