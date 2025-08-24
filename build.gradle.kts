plugins {
    id("java")
    id("application")
    id("com.gradleup.shadow") version "8.3.4"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots-longpolling:7.10.0")
    implementation("org.telegram:telegrambots-client:7.10.0")
    implementation("org.mongodb:mongodb-driver-sync:5.2.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "org.qless.Main"
        )
    }
}

application {
    mainClass.set("org.qless.Main")
}

tasks {
    shadowJar {
        archiveBaseName.set("qless-bot")
        archiveVersion.set("1.0-SNAPSHOT")
        archiveClassifier.set("")
    }
}

tasks.register("stage") {
    dependsOn("clean", "shadowJar")
}
