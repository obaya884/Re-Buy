package io.github.obaya884.rebuy.di

/**
 * アプリが起動時に合成する Koin モジュールの一覧。
 *
 * ③ の段 2 で `:shared:ui` に置き、`:androidApp` はこれだけを見る形にする。
 * アプリ側が `dataModule` を直接参照すると、依存の向きが
 * `:androidApp` → `:shared:ui` → `:shared:domain` → `:shared:data` からずれる。
 */
val sharedModules = listOf(dataModule, domainModule, uiModule)
