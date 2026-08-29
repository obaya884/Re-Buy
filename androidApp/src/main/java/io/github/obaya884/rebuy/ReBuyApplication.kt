package io.github.obaya884.rebuy

import android.app.Application
import io.github.obaya884.rebuy.ui.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ReBuyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            // 同じ型の定義が 2 か所に現れたら黙って後勝ちさせず、落として気づく
            allowOverride(false)
            androidContext(this@ReBuyApplication)
            modules(uiModule)
        }
    }
}
