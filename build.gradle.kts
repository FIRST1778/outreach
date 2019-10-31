import edu.wpi.first.gradlerio.GradleRIOPlugin
import edu.wpi.first.gradlerio.frc.FRCJavaArtifact
import edu.wpi.first.gradlerio.frc.RoboRIO
import edu.wpi.first.toolchain.NativePlatforms
import io.gitlab.arturbosch.detekt.detekt
import jaci.gradle.deploy.artifact.ArtifactsExtension
import jaci.gradle.deploy.artifact.FileTreeArtifact
import jaci.gradle.deploy.target.TargetsExtension

plugins {
    kotlin("jvm") version "1.3.50"
    idea

    id("edu.wpi.first.GradleRIO") version "2019.4.1"

    id("org.jetbrains.dokka") version "0.10.0"
    id("com.diffplug.gradle.spotless") version "3.25.0"
    id("com.github.ben-manes.versions") version "0.27.0"
    id("io.gitlab.arturbosch.detekt") version "1.1.1"
}

val ROBOT_MAIN_CLASS = "org.frc1778.robot.MainKt"

deploy {
    targets {
        roboRIO()
    }
    artifacts {
        frcJavaArtifact()
        frcStaticFileDeploy()
    }
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(platform(kotlin("bom")))

    implementation("com.google.guava:guava:28.1-jre")
    implementation("com.google.code.gson:gson:2.8.6")
    // implementation("com.github.MTHSRoboticsClub:freezylib:-SNAPSHOT")

    wpi.deps.wpilib().forEach { compile(it) }
    wpi.deps.vendor.java().forEach { compile(it) }
    wpi.deps.vendor.jni(NativePlatforms.roborio).forEach { nativeZip(it) }
    wpi.deps.vendor.jni(NativePlatforms.desktop).forEach { nativeDesktopZip(it) }

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
}

spotless {
    kotlin { ktlint() }
    java { googleJavaFormat("1.7") }
    kotlinGradle { ktlint() }
}

detekt {
    config = files("$rootDir/detekt-config.yml")
}

tasks.compileKotlin {
    dependsOn("spotlessApply")
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xjvm-default=compatibility"
    }
}

tasks.jar {
    doFirst {
        from(configurations.compile.get().map {
            if (it.isDirectory) it else zipTree(it)
        })
        manifest(GradleRIOPlugin.javaManifest(ROBOT_MAIN_CLASS))
    }
}

fun TargetsExtension.roboRIO() =
    target("roboRIO", RoboRIO::class.java, closureOf<RoboRIO> { team = frc.teamNumber })

fun ArtifactsExtension.frcJavaArtifact() =
    artifact("frcJava", FRCJavaArtifact::class.java, closureOf<FRCJavaArtifact> {
        targets.add("roboRIO")
    })

fun ArtifactsExtension.frcStaticFileDeploy() =
    fileTreeArtifact("frcStaticFileDeploy", closureOf<FileTreeArtifact> {
        setFiles(fileTree("src/main/deploy"))
        targets.add("roboRIO")
        directory = "/home/lvuser/deploy"
    })
