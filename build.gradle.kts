plugins {
    java
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.github.kr328.gradle.i18n"
version = "1.0.2"

dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    compileOnly("org.projectlombok:lombok:1.18.26")

    compileOnly("com.android.tools.build:gradle:7.4.2")
    compileOnly(kotlin("gradle-plugin", version = "1.8.20"))

    implementation("com.squareup:kotlinpoet:1.13.0")
    implementation("org.yaml:snakeyaml:2.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
}

gradlePlugin {
    plugins {
        create("i18n") {
            id = project.group.toString()
            displayName = "I18n Gradle"
            description = "A gradle plugin to generate cross-platform i18n resources for Kotlin Multiplatform."
            implementationClass = "$id.I18nPlugin"
        }
    }
}

publishing {
    publications {
        withType(MavenPublication::class) {
            version = project.version.toString()
            group = project.group.toString()

            pom {
                name.set("I18n Gradle")
                description.set("A gradle plugin to generate cross-platform i18n resources for Kotlin Multiplatform.")
                url.set("https://github.com/Kr328/i18n-gradle-plugin")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/Kr328/i18n-gradle-plugin/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        name.set("Kr328")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/Kr328/i18n-gradle-plugin.git")
                    url.set("https://github.com/Kr328/i18n-gradle-plugin")
                }
            }
        }
    }
    repositories {
        mavenLocal()
        maven {
            name = "kr328app"
            url = uri("https://maven.kr328.app/releases")
            credentials(PasswordCredentials::class.java)
        }
    }
}