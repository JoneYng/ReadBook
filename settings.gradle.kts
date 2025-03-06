pluginManagement {
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
        maven(url = "https://maven.aliyun.com/nexus/content/groups/public/")
        maven(url = "https://maven.aliyun.com/nexus/content/repositories/releases/")
        maven(url = "https://maven.aliyun.com/repository/public/")
        maven(url = "https://maven.aliyun.com/repository/google/")
        maven(url = "https://maven.aliyun.com/repository/jcenter/")
        maven(url = "https://maven.aliyun.com/repository/central/")
        maven(url = "https://jitpack.io")
        maven(url = "https://maven.google.com/")
        maven(url = "https://dl.bintray.com/umsdk/release")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.aliyun.com/nexus/content/groups/public/")
        maven(url = "https://maven.aliyun.com/nexus/content/repositories/releases/")
        maven(url = "https://maven.aliyun.com/repository/public/")
        maven(url = "https://maven.aliyun.com/repository/google/")
        maven(url = "https://maven.aliyun.com/repository/jcenter/")
        maven(url = "https://maven.aliyun.com/repository/central/")
        maven(url = "https://jitpack.io")
        maven(url = "https://maven.google.com/")
        maven(url = "https://dl.bintray.com/umsdk/release")
    }
}

rootProject.name = "ReadBook"
include(":app")
 