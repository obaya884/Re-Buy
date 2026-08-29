package io.github.obaya884.rebuy

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * インストルメンテーションテストで [HiltTestApplication] を起動させるランナー。
 * 既定のランナーだと [ReBuyApplication] が起動してしまい、Hilt のテスト用コンポーネントが差し込めない。
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
