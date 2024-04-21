plugins {
    id("java")
}

group = "net.defade"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    repositories {
        mavenCentral()
        maven("https://repo.defade.net/defade") {
            name = "DefadeRepository"
            credentials(PasswordCredentials::class)
        }
    }
}

dependencies {
    implementation("net.defade:minestom:1.20.4-726941df6a")
}