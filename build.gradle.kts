
plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.0"
}

group = "de.gmx.simonvoid"

application {
    mainClass.set("de.gmx.simonvoid.ptv6proxy.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        extraWarnings.set(true)
        // suppress specific extra warning, because of https://youtrack.jetbrains.com/issue/KT-73736
        // might be fixed by now
        freeCompilerArgs.add("-Xsuppress-warning=UNUSED_ANONYMOUS_PARAMETER")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-config-yaml-jvm")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-apache5")
    "4.0.2".let { koinVersion ->
        runtimeOnly("io.insert-koin:koin-core:$koinVersion")
        implementation("io.insert-koin:koin-ktor:$koinVersion")
    }
    implementation("ch.qos.logback:logback-classic:1.5.15")
    // xml
    implementation("org.jdom:jdom2:2.0.6.1")
//    implementation("jaxen:jaxen:2.0.0")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5")) //kotlin-test-junit5
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.mockk:mockk:1.13.14")
    "5.15.0".let { mockserverVersion ->
        testImplementation("org.mock-server:mockserver-netty-no-dependencies:$mockserverVersion")
        testImplementation("org.mock-server:mockserver-client-java-no-dependencies:$mockserverVersion")
    }
}

tasks.test {
    useJUnitPlatform()
}
