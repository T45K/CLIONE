import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    kotlin("jvm") version "1.3.71"
    kotlin("plugin.spring") version "1.3.71"
    application
    id("antlr")
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "io.github.t45k"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("commons-codec:commons-codec:1.14")
    implementation("com.auth0:java-jwt:3.10.2")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation(fileTree("./lib/github-api-1.113-SNAPSHOT.jar"))
    implementation(fileTree("./lib/github-api-1.113-SNAPSHOT-jar-with-dependencies.jar"))
    implementation(fileTree("./lib/indexbased.SearchManager.jar"))
    implementation(fileTree("./lib/kotlin-grammar-tools-0.1-43.jar"))
    implementation("org.bouncycastle:bcprov-jdk15on:1.65")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.7.0.202003110725-r")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.0")
    implementation("com.github.kusumotolab:sdl4j:0.4.0")
    implementation("org.jgrapht:jgrapht-core:1.4.0")
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.22.0")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("io.mockk:mockk:1.10.0")
    antlr("org.antlr:antlr4:4.7.2")
}

/*
tasks.withType<Test> {
    useJUnitPlatform()
}
*/

application {
    mainClassName = "io.github.t45k.clione.ClioneApplicationKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
