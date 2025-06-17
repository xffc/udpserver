import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.1.20"

    alias(libs.plugins.mavenpublish)
}

val githubAuthor = rootProject.property("github.author") as String

group = "io.github.${githubAuthor.split('/')[1]}"
version = "1.0"
description = "Simple UDP server library"

val githubProject = "$githubAuthor/${rootProject.name}"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    api(libs.ktor.network)
}

mavenPublishing {
    coordinates(
        groupId = group as String,
        artifactId = name,
        version = version as String
    )

    pom {
        name.set(project.name)
        description.set(project.description)
        url.set("https://$githubProject")

        licenses {
            license {
                name = "MIT"
                url = "https://${
                    githubProject.replace(
                        "github.com",
                        "raw.githubusercontent.com"
                    )
                }/refs/heads/master/LICENSE"
            }
        }

        developers {
            developer {
                id.set("xffcsk")
                name.set("xffc")
                url.set(githubAuthor)
            }
        }

        scm {
            url.set(githubProject)
            connection.set("scm:git:git://$githubProject.git")
            developerConnection.set("scm:git:ssh://$githubProject.git")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()
}

sourceSets.main {
    java.srcDir("src")
    kotlin.srcDir("src")
    resources.srcDir("resources")
}

sourceSets.main {
    java.srcDir("test/src")
    kotlin.srcDir("test/src")
    resources.srcDir("resources")
}

kotlin {
    jvmToolchain(21)
}