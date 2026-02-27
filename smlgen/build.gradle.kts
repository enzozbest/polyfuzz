plugins {
    kotlin("jvm") version "2.3.0"
    application
    jacoco
}

group = "bestetti.enzo"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("bestetti.enzo.smlgen.sml.cli.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.withType<JacocoReport>().configureEach {
    classDirectories.setFrom(
        classDirectories.files.map { file ->
            fileTree(file) {
                exclude("**/*\$DefaultImpls.class")
                exclude("**/smlgen/MainKt.class")  // Exclude CLI entry point from coverage
            }
        }
    )
}
