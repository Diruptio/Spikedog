plugins {
    id("java-gradle-plugin")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
}

gradlePlugin {
    plugins {
        create("spikedev") {
            id = "diruptio.spikedog.spikedev"
            implementationClass = "diruptio.spikedog.spikedev.Spikedev"
        }
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }

    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        manifest.attributes["Implementation-Title"] = "Spikedev"
        manifest.attributes["Implementation-Version"] = version
        manifest.attributes["Implementation-Vendor"] = "Diruptio"
        archiveBaseName = "Spikedev"
        isZip64 = true
    }
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
            artifactId = "Spikedev"
            from(components["java"])
        }
    }
}
