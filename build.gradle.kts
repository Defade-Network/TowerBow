plugins {
    id("java")
    id("java-library")
    // shadowjar
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "net.defade"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.defade.net/defade") {
        name = "DefadeRepository"
        credentials(PasswordCredentials::class)
    }
}

dependencies {
    implementation("net.defade:minestom:1.21-047d973f31")
    implementation("io.github.togar2:MinestomPvP:1.0") /* TODO: change to official version once it's updated to 1.21 and arrows deflecting is fixed
    Meanwhile, the library must be compiled locally. */
}

tasks.shadowJar {
    // Main class
    manifest {
        attributes["Main-Class"] = "net.defade.towerbow.Main"
    }
}