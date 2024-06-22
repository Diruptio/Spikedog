plugins {
    id("com.diffplug.spotless") version "6.25.0"
    id("java")
    id("maven-publish")
    id("application")
}

version = "1.2.5"
group = "diruptio"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("commons-cli:commons-cli:1.8.0")
}

spotless {
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint()
        endWithNewline()
    }
    java {
        target("**/src/**/*.java")
        googleJavaFormat().aosp()
        removeUnusedImports()
        indentWithSpaces()
        endWithNewline()
    }
}

val generateSources =
    tasks.register<Copy>("generateSources") {
        doFirst { delete(layout.buildDirectory.dir("generated/sources/templates").get()) }
        from(file("src/main/templates"))
        into(layout.buildDirectory.dir("generated/sources/templates"))
        expand(mapOf("version" to version))
    }
sourceSets.main.get().java.srcDir(generateSources.map { it.outputs })

tasks {
    compileJava {
        dependsOn(generateSources)
        options.encoding = "UTF-8"
        options.release = 17
    }

    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        manifest.attributes["Implementation-Title"] = "Spikedog"
        manifest.attributes["Implementation-Version"] = version
        manifest.attributes["Main-Class"] = "diruptio.spikedog.Spikedog"
        archiveFileName = "Spikedog.jar"
    }

    named<JavaExec>("run") {
        workingDir = file("run")
        workingDir.mkdirs()
        standardOutput = System.out
        standardInput = System.`in`
    }
}

application {
    mainClass = "diruptio.spikedog.Spikedog"
}

publishing {
    repositories {
        maven("https://repo.diruptio.de/repository/maven-public-releases/") {
            name = "DiruptioPublic"
            credentials {
                username = (System.getenv("DIRUPTIO_MAVEN_USERNAME") ?: project.findProperty("maven_username") ?: "").toString()
                password = (System.getenv("DIRUPTIO_MAVEN_PASSWORD") ?: project.findProperty("maven_password") ?: "").toString()
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifactId = "Spikedog"
            from(components["java"])
        }
    }
}
