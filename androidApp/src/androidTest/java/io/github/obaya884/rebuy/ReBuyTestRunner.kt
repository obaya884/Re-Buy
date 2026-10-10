package io.github.obaya884.rebuy

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * instrumented を [TestReBuyApplication] の上で走らせる。`build.gradle.kts` の
 * `testInstrumentationRunner` がこれを指す（T-21）。
 *
 * `Application` はプロセスの起動時に作られるので、差し替えられるのはこの入口だけ。
 */
class ReBuyTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader,
        className: String,
        context: Context
    ): Application = super.newApplication(cl, TestReBuyApplication::class.java.name, context)
}
