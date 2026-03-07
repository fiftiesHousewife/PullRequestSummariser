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
    applicationDefaultJvmArgs = listOf(
        "-Djdk.http.auth.tunneling.disabledSchemes=",
        "-Djdk.http.auth.proxying.disabledSchemes="
    )
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

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
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
    val proxyProperties = listOf(
        "https.proxyHost", "https.proxyPort", "https.proxyUser", "https.proxyPassword",
        "http.proxyHost", "http.proxyPort", "http.nonProxyHosts"
    )
    proxyProperties.forEach { property ->
        systemProperty(property, System.getProperty(property) ?: "")
    }
    jvmArgs(
        "-Djdk.http.auth.tunneling.disabledSchemes=",
        "-Djdk.http.auth.proxying.disabledSchemes="
    )
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "org/fifties/housewife/PullRequestExtract.class",
                    "org/fifties/housewife/CsvPullRequestExtract.class",
                    "org/fifties/housewife/GitHubClient.class",
                    "org/fifties/housewife/ProxyAwareHttpClient*.class",
                    "org/fifties/housewife/Main.class"
                )
            }
        })
    )
    violationRules {
        rule {
            limit {
                minimum = "0.75".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
