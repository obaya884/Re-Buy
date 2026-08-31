package io.github.obaya884.rebuy

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.obaya884.rebuy.ui.resources.Res
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 同梱した書体が**実際に APK に載っている**ことを見る（画面定義書 §5）。
 *
 * フォントが載っていなくても Compose は黙って既定の書体に落ちるので、**画面は動いたまま
 * 見た目だけが変わる**。③ の段 3 で踏んだ「Compose Resources が assets ごと消える」事故と
 * 同じ形なので、`LicenseLibrariesTest` と同じくリソースの実在をここで押さえる。
 *
 * ライセンス（OFL-1.1）の一覧への掲載は `LicenseLibrariesTest` が見る。
 */
@RunWith(AndroidJUnit4::class)
class BundledFontTest {

    @Test
    fun 丸ゴシックがAPKに載っている() {
        val bytes = runBlocking { Res.readBytes(FONT_PATH) }

        // 先頭 4 バイトは TrueType のバージョン（0x00010000）。空ファイルや
        // LFS のポインタが載っていたらここで落ちる
        assertTrue("フォントが $FONT_PATH に無いか空", bytes.size > 1_000_000)
        assertEquals("TrueType の中身ではない", 0x00, bytes[0].toInt())
        assertEquals("TrueType の中身ではない", 0x01, bytes[1].toInt())
    }

    @Test
    fun ライセンス文が同梱されている() {
        val text = runBlocking { Res.readBytes(LICENSE_PATH).decodeToString() }

        assertTrue("OFL の本文が入っていない", "SIL OPEN FONT LICENSE" in text.uppercase())
    }

    private companion object {
        const val FONT_PATH = "font/zen_maru_gothic_bold.ttf"
        const val LICENSE_PATH = "files/zen_maru_gothic_OFL.txt"
    }
}
