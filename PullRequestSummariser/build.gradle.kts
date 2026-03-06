plugins {
    id("java")
    id("application")
    id("jacoco")
    alias(libs.plugins.versions)
}

group = "org.fifties.housewife"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("org.fifties.housewife.Main")
}

dependencies {
    implementation(libs.gson)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    group = "verification"
    description = "Runs integration tests against the live GitHub API (requires GITHUB_TOKEN)"
    classpath = sourceSets["test"].runtimeClasspath
    testClassesDirs = sourceSets["test"].output.classesDirs
    systemProperty("https.proxyHost", System.getProperty("https.proxyHost") ?: "")
    systemProperty("https.proxyPort", System.getProperty("https.proxyPort") ?: "")
    systemProperty("http.proxyHost", System.getProperty("http.proxyHost") ?: "")
    systemProperty("http.proxyPort", System.getProperty("http.proxyPort") ?: "")
    systemProperty("http.nonProxyHosts", System.getProperty("http.nonProxyHosts") ?: "")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.55".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
