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
    }

    plugins { // ★ KSPプラグインのバージョンをここで定義
        id("com.google.devtools.ksp") version "2.2.20-Beta1-2.0.2" // 例: 最新のKSPバージョンを確認してください
        // 他にバージョン管理したいプラグインがあればここに追加
        // 例: id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CatQuestApp"
include(":app")
