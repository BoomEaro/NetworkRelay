import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    alias(libs.plugins.shadow)
    id("java")
    id("java-library")
}

group = "ru.boomearo"
version = "1.0"

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        manifest {
            attributes(
                "Main-Class" to "ru.boomearo.networkrelay.NetworkRelayBootstrap",
                "Multi-Release" to "true"
            )
        }
    }

    shadowJar {
        archiveBaseName.set("NetworkRelay")
        archiveClassifier.set("")
        archiveVersion.set("")

        filesMatching("META-INF/org/apache/logging/log4j/core/config/plugins/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        mergeServiceFiles()
        transform(Log4j2PluginsCacheFileTransformer::class.java)
    }

    build {
        dependsOn(shadowJar)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.netty.codec.haproxy)
    implementation(libs.netty.transport.native.epoll)
    implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-x86_64") })
    implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-aarch_64") })
    implementation(libs.netty.handler)

    implementation(libs.console.appender)
    implementation(libs.disruptor)
    implementation(libs.slf4j.api)
    implementation(libs.log4j.slf4j2.impl)
    implementation(libs.slf4j.jdk.platform.logging)
    implementation(libs.guava)
    implementation(libs.configurate.yaml)

    implementation(libs.dsiutils) {
        exclude(group = "ch.qos.logback")
        exclude(group = "it.unimi.di")
        exclude(group = "org.slf4j")
    }

    implementation(libs.bundles.log4j)
    implementation(libs.bundles.jline.terminal)

    implementation(libs.logj4j.core)
    annotationProcessor(libs.logj4j.core)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
