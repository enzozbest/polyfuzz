plugins {
    kotlin("jvm") version "2.3.0"
    jacoco
}

group = "bestetti.enzo"
version = "1.0-SNAPSHOT"

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
                exclude("**/DemoKt.class")  // Exclude Demo.kt main function from coverage
            }
        }
    )
}
