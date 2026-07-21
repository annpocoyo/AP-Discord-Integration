pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
		gradlePluginPortal()
	}

	plugins {
		id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version")
		id("com.gradleup.shadow") version providers.gradleProperty("shadow_version")
	}
}

// Should match your modid
rootProject.name = "ap-discord-integration"
