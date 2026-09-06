package io.github.obaya884.rebuy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * 検索キーの畳み込み（画面定義書 §05）。**畳むものと畳まないものを対で見る**——
 * 広げすぎると別の品目まで当たるので、通す網と止める網の両方が要る。
 *
 * **リスクは分岐ではなく対応表にある**ので、`when` の枝を 1 つずつ通すのではなく
 * **半角カナの全域を往復させる**。算術で畳む枝（全角英数）は両端だけで足りる。
 *
 * **正規形を実値で固定する**のも要点——両辺を同じ実装で畳んで比べる形だけだと、
 * Android と iOS で畳み方がずれても両辺が同じ方向にずれて緑のままになる。
 *
 * 当たり判定そのもの（両辺に通してから部分一致すること）は `AddNoticedViewModelTest`。
 */
class SearchKeyTest {

    /** 同じキーになる＝どちらで打っても当たる、という読み方をする。 */
    private fun assertSameKey(vararg variants: String) {
        require(variants.size >= 2) { "見比べる相手が要る" }
        val keys = variants.map { SearchKey.of(it) }
        keys.drop(1).forEach { assertEquals(keys.first(), it, variants.toList().toString()) }
    }

    // ---- 対応表の全域 ----

    /**
     * **半角カナの全域を 1 本で往復させる。** 表の 1 文字を書き換える変異も、表を丸ごと
     * 1 つずらす変異も、ここで落ちる。個別の文字を並べる形だと触れていない文字が残る。
     */
    @Test
    fun 半角カナと全角カナの全域が同じキーになる() {
        assertEquals(EXPECTED_KANA, SearchKey.of(HALFWIDTH_KANA_WITHOUT_MARKS))
        assertEquals(EXPECTED_KANA, SearchKey.of(FULLWIDTH_KANA_WITHOUT_MARKS))
    }

    /** 濁音も 23 組すべてを往復させる。**`ヷ`・`ヺ` に対応するひらがなは無い**ので残る。 */
    @Test
    fun 濁音の全域が同じキーになる() {
        assertEquals(EXPECTED_VOICED, SearchKey.of(HALFWIDTH_VOICED))
        assertEquals(EXPECTED_VOICED, SearchKey.of(FULLWIDTH_VOICED))
    }

    @Test
    fun 半濁音の全域が同じキーになる() {
        assertEquals("ぱぴぷぺぽ", SearchKey.of("ﾊﾟﾋﾟﾌﾟﾍﾟﾎﾟ"))
        assertEquals("ぱぴぷぺぽ", SearchKey.of("パピプペポ"))
    }

    /**
     * **ひらがな側も全域を通す。** ひらがなをカタカナへ寄せる枝は、合成を経由しない限り
     * 往復して元に戻る（＝範囲を狭めても結果が変わらない）ので、**観測できるのは濁点を
     * 後置したときだけ**。ここを通さないと `う゛`・`わ゛`・`を゛` が畳まれなくなっても緑になる。
     */
    @Test
    fun ひらがなに濁点を後置しても全域が合成される() {
        val spacing = withMark(VOICED_BASES_HIRAGANA, VOICED_MARK)
        val combining = withMark(VOICED_BASES_HIRAGANA, COMBINING_VOICED_MARK)

        assertEquals(EXPECTED_VOICED, SearchKey.of(spacing))
        assertEquals(EXPECTED_VOICED, SearchKey.of(combining))
    }

    @Test
    fun ひらがなに半濁点を後置しても全域が合成される() {
        assertEquals("ぱぴぷぺぽ", SearchKey.of(withMark("はひふへほ", SEMI_VOICED_MARK)))
        assertEquals("ぱぴぷぺぽ", SearchKey.of(withMark("はひふへほ", COMBINING_SEMI_VOICED_MARK)))
    }

    /**
     * **畳みすぎる側の全域。** 表の中に重複を作る変異（`ﾀ` → `ナ` など）は、往復の網では
     * 片側しか見ていないと素通りしうる。63 文字が 63 通りのキーになることで塞ぐ。
     */
    @Test
    fun 半角カナの63文字はすべて別のキーになる() {
        val keys = ('｡'..'ﾟ').map { SearchKey.of(it.toString()) }

        // **衝突した組そのものを主張する。** 件数だけ見ると、落ちたときに犯人が分からない
        assertEquals(emptyMap(), keys.groupBy { it }.filterValues { it.size > 1 })
    }

    // ---- 正規形 ----

    /**
     * **キーの見た目そのものを固定する。** 相対比較だけだと、両辺が同じ方向にずれる変異
     * （ひらがなではなくカタカナへ寄せる、小文字ではなく大文字へ寄せる）が全件素通りする。
     */
    @Test
    fun キーはひらがなと半角小文字になる() {
        assertEquals("あいてむa", SearchKey.of("ｱｲﾃﾑＡ"))
        assertEquals("あいてむa", SearchKey.of("アイテムA"))
    }

    @Test
    fun 合成した濁音もひらがなになる() {
        assertEquals("がっこう", SearchKey.of("ｶﾞｯｺｳ"))
    }

    /**
     * **両 OS でずれうるのは `lowercase()` だけ**なので、ASCII の外まで実値で固定する。
     * 他の枝は算術と自前の対応表なので構造上ずれない。ここが両 OS で違えば CI が落ちる。
     */
    @Test
    fun 大小の畳み込みはASCIIの外にも効く() {
        assertEquals("äö", SearchKey.of("ÄÖ"))
    }

    /**
     * **サロゲートペアを割らない。** 今の実装は UTF-16 単位で走査しても全分岐が
     * U+D800..DFFF を避けているので安全だが、何も固定していないと、範囲を広げたときや
     * コードポイント単位へ書き換えたときに名前が黙って壊れる（`NameRule.truncate` と同じ話）。
     */
    @Test
    fun 絵文字を含む名前でも壊れない() {
        assertEquals("🍎あいてむ", SearchKey.of("🍎アイテム"))
    }

    // ---- 畳むもの ----

    @Test
    fun ひらがなとカタカナを同一視する() {
        assertEquals("みるく", SearchKey.of("みるく"))
        assertSameKey("みるく", "ミルク", "ﾐﾙｸ")
    }

    /**
     * **半角の濁点は独立した 1 文字**（`ｶ` ＋ `ﾞ`）なので、寄せるだけでは `ガ` と長さが違う。
     * 合成を落とすとここが落ちる。
     */
    @Test
    fun 濁点は書き方が違っても同じ1文字に合成する() {
        assertSameKey("がっこう", "ガッコウ", "ｶﾞｯｺｳ", "か゛っこう")
    }

    /**
     * **NFD（結合用濁点 U+3099）も畳む。** macOS・iOS 由来のテキストを貼り付けると
     * この形で入る。**iOS 側にだけ効く穴**になるので、プラットフォームを問わない実装の
     * 建前と矛盾しないよう畳む側に倒した。
     */
    @Test
    fun 結合用の濁点も合成する() {
        assertSameKey("\u304B\u3099", "が", "\u30AB\u3099", "ガ")
        assertSameKey("\u306F\u309A", "ぱ", "\u30CF\u309A", "パ")
    }

    /** `ウ` は +1 では濁音にならない（`ヴ` は離れた位置にある）ので、表で持っていることを見る。 */
    @Test
    fun ウの濁音も合成する() {
        assertSameKey("ゔぃ", "ヴィ", "ｳﾞｨ")
    }

    @Test
    fun 全角の英数字を半角と同一視する() {
        assertSameKey("ab12", "ａｂ１２")
    }

    /** 算術で畳む枝なので**両端だけ**見る（全域を通す必要は無い）。 */
    @Test
    fun 全角英数の範囲は両端まで畳む() {
        assertSameKey("!~", "！～")
    }

    @Test
    fun 英字の大文字と小文字を同一視する() {
        assertSameKey("milk", "MILK", "Milk", "ＭＩＬＫ")
    }

    @Test
    fun 全角スペースを半角スペースと同一視する() {
        assertSameKey("a b", "ａ　ｂ")
    }

    /** 小書きの `ヵ`・`ヶ` まで畳む。上端を `ヴ` に狭めると「3ヶ入り」のような名前で効く。 */
    @Test
    fun 小書きのカとケも畳む() {
        assertEquals("ゕゖ", SearchKey.of("ヵヶ"))
    }

    // ---- 畳まないもの ----

    /** **濁点そのものは畳まない。** 畳むと「か」で「が」まで当たり、探す側の意図から離れる。 */
    @Test
    fun 濁点の有無は別物のまま() {
        assertNotEquals(SearchKey.of("か"), SearchKey.of("が"))
        assertNotEquals(SearchKey.of("は"), SearchKey.of("ぱ"))
    }

    /** 小書きの文字も別物。「きやく」と「きゃく」が同じキーになると当たりが濁る。 */
    @Test
    fun 小書きの文字は別物のまま() {
        assertNotEquals(SearchKey.of("きやく"), SearchKey.of("きゃく"))
    }

    /**
     * **長音符は落とさない。** ひらがなに対応する文字が無いので `ー` のまま残す。
     * 落とすと「ケーキ」と「ケキ」が同じキーになる。
     */
    @Test
    fun 長音符は残す() {
        assertSameKey("けーき", "ケーキ", "ｹｰｷ")
        assertNotEquals(SearchKey.of("けーき"), SearchKey.of("けき"))
    }

    /** **語中の空白は残す。** 落とすと部分一致が静かに広がる。 */
    @Test
    fun 語中の空白は残す() {
        assertNotEquals(SearchKey.of("あ い"), SearchKey.of("あい"))
    }

    @Test
    fun 漢字と読みは同一視しない() {
        assertNotEquals(SearchKey.of("牛乳"), SearchKey.of("ぎゅうにゅう"))
    }

    /** 漢字はそのまま残る。落とすと漢字だけの名前が空のキーになり、全件に当たる。 */
    @Test
    fun 漢字はそのまま残る() {
        assertEquals("牛乳", SearchKey.of("牛乳"))
    }

    /**
     * **2 つの範囲の隙間**（U+FF5F・U+FF60）はどちらの枝にも入らず素通りするのが正。
     * 半角カナ側の下端を広げる変異は、ここで添字が負になって落ちる。
     */
    @Test
    fun 全角英数と半角カナの隙間はそのまま残る() {
        assertEquals("｟｠", SearchKey.of("｟｠"))
    }

    // ---- 部分一致と端の入力 ----

    /** キーは**部分一致で使う**ので、混ざった表記でも途中から当たる。 */
    @Test
    fun 混ざった表記でも部分一致する() {
        assertTrue(SearchKey.of("たまごMサイズ").contains(SearchKey.of("Ｍｻｲｽﾞ")))
    }

    @Test
    fun 空文字はそのまま空になる() {
        assertEquals("", SearchKey.of(""))
    }

    /**
     * 末尾に濁点だけが残る打ちかけでも落ちない（合成の相手が無い側の枝）。**半濁点も対で見る**
     * ——`indexOf` の -1 を素の添字で引く実装だと、ここが例外になる。行き先も実値で固定して
     * おく（対応表の末尾 2 文字の畳み先は、相対比較だけでは動いても気づけない）。
     */
    @Test
    fun 合成の相手が無い濁点と半濁点でも落ちない() {
        assertSameKey("ん゛", "ﾝﾞ")
        assertSameKey("ん゜", "ﾝﾟ")
        assertEquals("ん゛", SearchKey.of("ﾝﾞ"))
        assertEquals("ん゜", SearchKey.of("ﾝﾟ"))
    }

    private companion object {
        /** 濁点・半濁点を後置した列を作る。**間隔（U+309B・U+309C）と結合用（U+3099・U+309A）**を差し替える。 */
        fun withMark(bases: String, mark: Char): String =
            buildString { bases.forEach { append(it).append(mark) } }

        const val VOICED_MARK = '\u309B'
        const val SEMI_VOICED_MARK = '\u309C'
        const val COMBINING_VOICED_MARK = '\u3099'
        const val COMBINING_SEMI_VOICED_MARK = '\u309A'

        // **濁点を持たない 61 文字ぶん**（表の末尾 2 つは濁点そのものなので、後置の形で別に見る）
        const val HALFWIDTH_KANA_WITHOUT_MARKS =
            "｡｢｣､･ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ"
        const val FULLWIDTH_KANA_WITHOUT_MARKS =
            "。「」、・ヲァィゥェォャュョッーアイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワン"
        const val EXPECTED_KANA =
            "。「」、・をぁぃぅぇぉゃゅょっーあいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわん"

        const val HALFWIDTH_VOICED = "ｳﾞｶﾞｷﾞｸﾞｹﾞｺﾞｻﾞｼﾞｽﾞｾﾞｿﾞﾀﾞﾁﾞﾂﾞﾃﾞﾄﾞﾊﾞﾋﾞﾌﾞﾍﾞﾎﾞﾜﾞｦﾞ"
        const val FULLWIDTH_VOICED = "ヴガギグゲゴザジズゼゾダヂヅデドバビブベボヷヺ"
        const val VOICED_BASES_HIRAGANA = "うかきくけこさしすせそたちつてとはひふへほわを"

        /** **末尾の `ヷ`・`ヺ` だけカタカナで残る**——合成済みのひらがなが Unicode に無い。 */
        const val EXPECTED_VOICED = "ゔがぎぐげござじずぜぞだぢづでどばびぶべぼヷヺ"
    }
}
