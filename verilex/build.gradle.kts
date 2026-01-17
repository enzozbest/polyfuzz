plugins {
    kotlin("jvm") version "1.9.22"
    id("jacoco")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            // 100% line coverage
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
            // 100% branch coverage
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
        }
    }
}

tasks.register("coverage") {
    group = "verification"
    description = "Run tests with JaCoCo report and verify 100% coverage"
    dependsOn(tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
}

kotlin {
    jvmToolchain(17)
}
