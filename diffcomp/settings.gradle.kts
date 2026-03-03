plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "diffcomp"

includeBuild("../verilex") {
    dependencySubstitution {
        substitute(module("bestetti.enzo:verilex")).using(project(":"))
    }
}
