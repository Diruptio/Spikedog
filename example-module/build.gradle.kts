plugins {
    id("java")
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
