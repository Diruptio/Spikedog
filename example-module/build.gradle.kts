plugins {
    id("java")
    id("spikedev") version "2.0.0-beta.7.3"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":"))
    compileOnly("org.jetbrains:annotations:26.0.1")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }

    jar {
        archiveFileName = "example-module"
    }
}
