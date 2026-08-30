package io.github.obaya884.rebuy

import android.app.Application
import io.github.obaya884.rebuy.ui.di.initKoin
import org.koin.android.ext.koin.androidContext

class ReBuyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@ReBuyApplication)
        }
    }
}
