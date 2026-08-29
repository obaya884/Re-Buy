dependencyResolutionManagement {
    // ルートと版が二重管理になるのを避けるため、同じ catalog を読む。
    // CLAUDE.md の「dependencyResolutionManagement を使わない」はルートの settings の話で、
    // build-logic は独立したビルドなので影響しない
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
