package io.github.obaya884.rebuy

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mikepenz.aboutlibraries.Libs
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.screen.license.ABOUT_LIBRARIES_PATH
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
 * **ライセンス本文もここで見る。** OFL-1.1 は「フォントと一緒にライセンスを配ること」を
 * 条件にしているので、**画面に出る本文とリポジトリに置いた本文が同じ**であることまでを守る。
 * 一覧への掲載（entry があること）は `LicenseLibrariesTest`。
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
    fun ライセンス本文がAPKに載っている() {
        val text = runBlocking { Res.readBytes(LICENSE_PATH).decodeToString() }

        assertTrue("OFL の本文が入っていない", "SIL OPEN FONT LICENSE" in text.uppercase())
    }

    /**
     * 画面に出る本文が、同梱した OFL の本文と**同じ**であること。
     *
     * 一覧の本文は `shared/ui/aboutlibraries/licenses/` に置いた定義から来る。
     * **ここを手元に持っているのは、プラグインに解決させると本文の取得元によって
     * 生成物が変わり、CI の「生成物が最新か」で落ちるため**（実際に踏んだ）。
     */
    @Test
    fun 画面に出るライセンス本文が同梱したものと同じ() {
        val bundled = runBlocking { Res.readBytes(LICENSE_PATH).decodeToString() }
        val shown = runBlocking {
            Libs.Builder()
                .withJson(Res.readBytes(ABOUT_LIBRARIES_PATH).decodeToString())
                .build()
                .licenses
                .single { it.hash == FONT_LICENSE_HASH }
                .licenseContent
        }

        assertEquals(bundled, shown)
    }

    private companion object {
        const val FONT_PATH = "font/zen_maru_gothic_bold.ttf"
        const val LICENSE_PATH = "files/zen_maru_gothic_OFL.txt"

        /** `shared/ui/aboutlibraries/licenses/ofl_1_1.json` の hash。 */
        const val FONT_LICENSE_HASH = "ofl-1.1-bundled"
    }
}
