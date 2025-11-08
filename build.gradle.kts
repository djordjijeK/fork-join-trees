plugins {
    id("java")
    id("io.github.reyerizo.gradle.jcstress") version "0.9.0"
}

group = "io.github.djordjijeK"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openjdk.jcstress:jcstress-core:0.16")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
}

tasks.test {
    useJUnitPlatform()
}


jcstress {
    timeMillis = "250"
    iterations = "25"
    spinStyle = "HARD"
}