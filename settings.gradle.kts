pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Memora"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// App host
include(":app")

// Core (infra, atrás de interfaces + fakes)
include(":core:common")
include(":core:db")
include(":core:security")
include(":core:audio")
include(":core:transcription")
include(":core:speaker")
include(":core:location")
include(":core:digest")
include(":core:glossary")
include(":core:models")

// Features (UI)
include(":feature:today")
include(":feature:digest")
include(":feature:notes")
include(":feature:search")
include(":feature:settings")
include(":feature:onboarding")
