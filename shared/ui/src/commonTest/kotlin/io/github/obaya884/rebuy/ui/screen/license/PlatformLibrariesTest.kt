package io.github.obaya884.rebuy.ui.screen.license

import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ライセンス一覧のプラットフォーム絞りの規則を固定する（T-39）。
 *
 * 実データ（`aboutlibraries.json`）との突き合わせは instrumented の
 * `LicenseLibrariesTest` と `iosTest` の `LicenseLibrariesIosTest` が持つ。
 * ここは規則だけを、作った entry で見る。
 */
class PlatformLibrariesTest {

    private fun library(uniqueId: String, vararg targets: String) = Library(
        uniqueId = uniqueId,
        artifactVersion = null,
        name = uniqueId,
        description = null,
        website = null,
        developers = emptyList(),
        organization = null,
        scm = null,
        targets = targets.toSet()
    )

    private fun Libs.ids() = libraries.map { it.uniqueId }

    private val androidOnly = library("android-only", "android")
    private val iosOnly = library("ios-only", "iosArm64", "iosSimulatorArm64")
    private val jvmOnly = library("jvm-only", "jvm")
    private val both = library("both", "android", "iosArm64", "iosSimulatorArm64")
    private val untargeted = library("untargeted")

    private val license = License(name = "Apache-2.0", url = null, hash = "hash-1")

    private val libs = Libs(
        libraries = listOf(androidOnly, iosOnly, jvmOnly, both, untargeted),
        licenses = setOf(license)
    )

    @Test
    fun 接頭辞に一致するターゲットを持つ依存だけが残る() {
        // どのターゲットにも属さない jvm-only はどちらの絞りでも落ちる
        assertEquals(
            listOf("android-only", "both", "untargeted"),
            libs.filterByTargetPrefix("android").ids()
        )
        // iOS は実機とシミュレータの 2 ターゲットを ios 接頭辞で束ねる
        assertEquals(
            listOf("ios-only", "both", "untargeted"),
            libs.filterByTargetPrefix("ios").ids()
        )
    }

    @Test
    fun targetsが空の依存は絞らず残る() {
        // configPath で手足しした entry には targets が付かない。
        // 絞りで黙って消えると、載せる義務のある表記が落ちる方向の事故になる
        assertEquals(
            listOf("untargeted"),
            Libs(libraries = listOf(untargeted), licenses = emptySet())
                .filterByTargetPrefix("android")
                .ids()
        )
    }

    @Test
    fun 絞ってもライセンス本文の集合は変わらない() {
        assertEquals(setOf(license), libs.filterByTargetPrefix("android").licenses)
    }

    /**
     * commonTest は Android と iOS の両方で走るので、「どちらかの接頭辞である」ことしか
     * 共通には書けない。**どちらか**は各プラットフォームの実データのテストが確かめる。
     */
    @Test
    fun 実行中のプラットフォームに対象外の依存だけが落ちる() {
        val filtered = libs.forCurrentPlatform().ids()

        assertTrue("both" in filtered, "両 OS に載る依存が消えた: $filtered")
        assertTrue("untargeted" in filtered, "targets 無しの依存が消えた: $filtered")
        assertFalse("jvm-only" in filtered, "どの OS にも載らない依存が残っている: $filtered")
        assertEquals(
            1,
            listOf("android-only", "ios-only").count { it in filtered },
            "実行中の OS 専用の 1 件だけが残るはず: $filtered"
        )
    }
}
