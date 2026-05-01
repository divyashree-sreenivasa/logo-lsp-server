import org.gradle.api.plugins.antlr.AntlrTask

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
    id("antlr")
}

group = "jetbrains.lsp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // LSP protocol + JSON-RPC
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.23.1")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.23.1")

    // ANTLR4 — grammar compiled at build time, runtime needed at execution
    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")

    // Logging — all output goes to stderr so stdout stays clean for JSON-RPC
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Tests
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<AntlrTask>("generateGrammarSource") {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-package", "jetbrains.lsp.logo.parser", "-visitor", "-no-listener")
    outputDirectory = file("build/generated-src/antlr/main")
}

tasks.compileJava {
    dependsOn(tasks.named("generateGrammarSource"))
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", "build/generated-src/antlr/main")
        }
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "jetbrains.lsp.logo.ServerLauncher"
    }
    mergeServiceFiles()
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
