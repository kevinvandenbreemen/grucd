plugins {
    kotlin("jvm") version "1.8.22"
    java
    `maven-publish`
    `java-library`
}

val versionRaw = File("src/main/resources/version.properties")
val versionNumber = versionRaw.readText()

group = "com.vandenbreemen"
version = versionNumber

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    val javaParserVersion = "3.25.6"
    implementation("com.github.javaparser:javaparser-symbol-solver-core:$javaParserVersion")

    val kluentVersion = "1.68"
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")


    val log4jVersion = "1.2.14"
    implementation("log4j:log4j:$log4jVersion")

    val kevinCommonVersion = "1.0.6.1000"
    implementation("com.github.kevinvandenbreemen:kevin-common:$kevinCommonVersion")

    val kotlinParserVersion = "0.1.0"
    implementation("com.github.kotlinx.ast:common:$kotlinParserVersion")
    implementation("com.github.kotlinx.ast:grammar-kotlin-parser-antlr-kotlin:$kotlinParserVersion")

}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

//  Based on https://github.com/gradle/kotlin-dsl-samples/blob/master/samples/maven-publish/build.gradle.kts
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}