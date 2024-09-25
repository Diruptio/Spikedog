plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":"))
    compileOnly("org.jetbrains:annotations:25.0.0")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }

    jar {
        archiveBaseName = "reload-module"
        doLast {
            copy {
                from(archiveFile)
                into(rootProject.file("run/modules"))
            }
        }
    }
}
