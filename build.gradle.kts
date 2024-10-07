plugins {
    id("com.diffplug.spotless") version "6.25.0"
    id("java")
    id("maven-publish")
    id("application")
}

version = "2.0.0-beta.4"
group = "diruptio"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:25.0.0")
    implementation("io.netty:netty-all:4.1.114.Final")
    implementation("com.google.guava:guava:33.3.1-jre")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.yaml:snakeyaml:2.3")
    runtimeOnly("org.bouncycastle:bcpkix-jdk18on:1.78.1")
}

spotless {
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint()
        endWithNewline()
    }
    java {
        target("**/src/**/*.java")
        palantirJavaFormat("2.48.0").formatJavadoc(true)
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

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    compileJava {
        dependsOn(generateSources)
        options.encoding = "UTF-8"
        options.release = 21
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
