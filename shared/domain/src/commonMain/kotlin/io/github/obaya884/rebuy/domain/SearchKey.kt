package io.github.obaya884.rebuy.domain

/**
 * 検索キー（画面定義書 §05 の「入力中」）。**打ち方の揺れを畳んでから比べる**ための正規化。
 *
 * 畳むのは 3 つ——ひらがなとカタカナ、全角と半角（半角カタカナを含む）、英字の大文字と小文字。
 * **濁点・半濁点は畳まない**ので「か」と「が」は別物のままで、漢字と読みの同一視もしない。
 *
 * **大小を畳むのは `lowercase()` なので、条項が言う「英字」より広く効く**（`Ä` も `ä` になる）。
 * 品目名で困る向きではないので狭めていないが、**畳み方が両 OS でずれうるのはここだけ**
 * ——他の枝は算術と自前の対応表なので構造上ずれない。`SearchKeyTest` が実値で固定する。
 *
 * **プラットフォームの正規化 API は使わない**（経緯は `log_13_画面定義書.md` 2026-09-06）。
 * JVM と Foundation で畳む範囲が完全には一致せず、同じ入力で両 OS の検索結果が変わりうる。
 */
object SearchKey {

    /** [raw] を検索キーへ畳む。**名前と入力の両方に同じ関数を通してから部分一致で比べる。** */
    fun of(raw: String): String = raw
        .mapChars(::align)
        .composeMarks()
        .mapChars(::toHiragana)
        .lowercase()

    /**
     * 同じ文字の別の書き方を 1 つに寄せる。行き先は**全角カタカナ・半角 ASCII・間隔の濁点**。
     *
     * **かなをカタカナ側へ寄せるのは合成のため。** `ｶﾞ` は 2 文字なので、寄せるだけでは 1 文字の
     * `ガ` に届かない。カタカナへ揃えておけば濁点の対応表を 1 組で持てて、`ｶﾞ`・`カ゛`・`か゛`・
     * 結合用濁点の NFD がすべて同じ経路を通る。
     *
     * **踊り字（`ゝゞ`・`ヽヾ`）と `ゟ`・`ヿ` は範囲の外**なので、かなとカナで別のキーになる。
     * かなとカナで並びが揃っておらず枝を分けることになるので、品目名で使われる見込みに
     * 対して割に合わないと見た。
     */
    private fun align(char: Char): Char = when (char) {
        in '！'..'～' -> char - FULLWIDTH_ASCII_OFFSET // U+FF01..U+FF5E の全角英数記号
        FULLWIDTH_SPACE -> ' '
        in 'ぁ'..'ゖ' -> char + KANA_OFFSET // U+3041..U+3096 のひらがな
        in '｡'..'ﾟ' -> FULLWIDTH_KANA[char - '｡'] // U+FF61..U+FF9F の半角カナと記号
        COMBINING_VOICED_MARK -> VOICED_MARK
        COMBINING_SEMI_VOICED_MARK -> SEMI_VOICED_MARK
        else -> char
    }

    /** 濁点・半濁点を直前の文字と合成する。合成できない濁点はそのまま残す。 */
    private fun String.composeMarks(): String {
        // **`buildString` の中から素の `getOrNull` を呼ばない**——受け手が StringBuilder になり、
        // 書きかけの出力を先読みしてしまう。読む先を局所変数で名指しする
        val source = this
        return buildString(source.length) {
            var index = 0
            while (index < source.length) {
                val mark = source.getOrNull(index + 1)
                val composed = mark?.let { composedOrNull(source[index], it) }
                append(composed ?: source[index])
                index += if (composed == null) 1 else 2
            }
        }
    }

    /**
     * [base] と続く [mark] が濁音・半濁音を作るならその 1 文字。作らないなら null。
     *
     * **`ヰ`・`ヱ` の濁音（`ヸ`・`ヹ`）は表に持たない**ので、`ゐ゛` と `ヸ` は別のキーになる。
     * 半角カナに `ヰ`・`ヱ` が無く、片側だけ揃えても半端になるため。
     */
    private fun composedOrNull(base: Char, mark: Char): Char? = when (mark) {
        VOICED_MARK -> VOICED.getOrNull(VOICED_BASES.indexOf(base))
        SEMI_VOICED_MARK -> SEMI_VOICED.getOrNull(SEMI_VOICED_BASES.indexOf(base))
        else -> null
    }

    /**
     * カタカナをひらがなへ。範囲の外の 2 種はカタカナのまま残る——**長音符「ー」**は
     * 落とすと「ケーキ」と「ケキ」が同じキーになり、**`ヷ`・`ヺ`** は対応するひらがなが
     * 無い。どちらも両辺が同じように残るので、当たり方は変わらない。
     */
    private fun toHiragana(char: Char): Char =
        if (char in 'ァ'..'ヶ') char - KANA_OFFSET else char

    private fun String.mapChars(transform: (Char) -> Char): String =
        buildString(length) { for (char in this@mapChars) append(transform(char)) }

    private const val FULLWIDTH_ASCII_OFFSET = 0xFEE0

    /** ひらがなとカタカナの距離。**向きは使う側が決める**（[align] は ＋、[toHiragana] は −）。 */
    private const val KANA_OFFSET = 0x60

    // **字形で見分けられないものはエスケープで書く。** 全角スペースは何も見えず、間隔の
    // 濁点（U+309B）と結合用の濁点（U+3099）はフォントによっては同じに見える。取り違えると
    // NFD（macOS・iOS 由来の貼り付け）だけが畳まれない、という気づきにくい穴が空く。
    private const val FULLWIDTH_SPACE = '\u3000'
    private const val VOICED_MARK = '\u309B'
    private const val SEMI_VOICED_MARK = '\u309C'
    private const val COMBINING_VOICED_MARK = '\u3099'
    private const val COMBINING_SEMI_VOICED_MARK = '\u309A'

    /**
     * U+FF61〜U+FF9F の 63 文字ぶんの対応表。**添字は `char - '｡'`** なので順を崩さない
     * ——1 文字落とすと以降がすべてずれ、末尾の `ﾟ` は範囲外になる（`SearchKeyTest` が
     * 全域で往復させて守る。**この添字がこのクラス唯一の素の添字アクセス**）。
     * **末尾 2 つは間隔の濁点**で、[composeMark] の照合相手そのものなのでエスケープで書く。
     */
    private const val FULLWIDTH_KANA =
        "。「」、・ヲァィゥェォャュョッーアイウエオカキクケコサシスセソタチツテトナニヌネノ" +
            "ハヒフヘホマミムメモヤユヨラリルレロワン\u309B\u309C"

    private const val VOICED_BASES = "ウカキクケコサシスセソタチツテトハヒフヘホワヲ"
    private const val VOICED = "ヴガギグゲゴザジズゼゾダヂヅデドバビブベボヷヺ"
    private const val SEMI_VOICED_BASES = "ハヒフヘホ"
    private const val SEMI_VOICED = "パピプペポ"
}
