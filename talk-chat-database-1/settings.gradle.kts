pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.squareup.sqldelight" -> useModule("com.squareup.sqldelight:gradle-plugin:${requested.version}")
            }
        }

        fun RepositoryHandler.setup() {
            jcenter()
            if (this == pluginManagement.repositories) {
                gradlePluginPortal()
            }
        }
        repositories.setup()
        gradle.allprojects { repositories.setup() }
    }

    plugins {
        val kotlin_version: String by settings
        kotlin("jvm").version(kotlin_version)
    }
}

include("shared")
include("client")
include("registry")
