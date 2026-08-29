package io.github.obaya884.rebuy

import android.app.Application
import io.github.obaya884.rebuy.di.sharedModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ReBuyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ReBuyApplication)
            modules(sharedModules)
        }
    }
}
