plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "io.github.nerdygekko"
version = "1.0.0-SNAPSHOT"
description = "Sequence 1 Fool Grafting ability for the Mysterria trial dev task"

val minecraftVersion = "26.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.133-stable")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
    }

    processResources {
        val props = mapOf("version" to project.version, "description" to project.description)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    // ./gradlew runServer starts a local Paper test server with this plugin loaded
    runServer {
        minecraftVersion(minecraftVersion)
    }
}
