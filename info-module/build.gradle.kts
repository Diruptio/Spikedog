plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":"))
    compileOnly("org.jetbrains:annotations:24.1.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 17
    }

    jar {
        archiveBaseName = "info-module"
        doLast {
            copy {
                from(archiveFile)
                into(rootProject.file("run/modules"))
            }
        }
    }
}
