package io.github.obaya884.rebuy.domain

/**
 * 品目・カテゴリー・行き先の名前の検証（データモデル定義書 §5）。
 *
 * **UI とデータ層の二重の網の、データ層側。** UI は 31 文字目以降を打ち切り、確定時に
 * エラーを出す（画面定義書 §2）。ここは UI を通らない経路も含めて最後に守る段で、
 * さらにその後ろに DB の UNIQUE インデックスがいる。
 */
object NameRule {
    /** 上限。長さは `String.length`（UTF-16 単位）で数え、絵文字が 2 文字ぶんに数えられるのは許容する。 */
    const val MAX_LENGTH = 30

    /**
     * 前後の空白を除く。**全角スペースも空白として扱う**——`Char.isWhitespace()` は
     * Unicode の空白判定なので U+3000 も落ちる。
     */
    fun normalize(rawName: String): String = rawName.trim()

    /**
     * トリム済みの名前を、DB を引かずに見られる規則で検査する。
     * 重複だけは同種の他の行を見る必要があるので Repository が担う。
     */
    /**
     * 上限までに切り詰める。**サロゲートペアを割らない**——`take` だけだと
     * 「29 文字＋絵文字」で上位サロゲートだけが残り、壊れた 1 文字が保存される。
     */
    fun truncate(rawName: String): String {
        val taken = rawName.take(MAX_LENGTH)
        return if (taken.lastOrNull()?.isHighSurrogate() == true) taken.dropLast(1) else taken
    }

    fun validate(normalizedName: String): NameError? = when {
        normalizedName.isEmpty() -> NameError.BLANK
        normalizedName.length > MAX_LENGTH -> NameError.TOO_LONG
        else -> null
    }
}

/** 名前が弾かれた理由。文字種は制限しないので、この 3 つで尽きる。 */
enum class NameError {
    BLANK,

    /**
     * 上限超え。**UI が打ち切るので通常は届かない**が、届いたときのために文言を持つ
     * （画面定義書 §2。文言の無いエラーは赤枠だけが出て理由が分からない）。
     */
    TOO_LONG,

    DUPLICATE
}

/**
 * 保存の結果。
 *
 * `NameError?` を返さないのは、**呼び出し側が結果を無視しても気づけないため**。
 * 名前を伴う保存はすべてこの型を返し、UI は [Rejected] を入力欄の下に出す。
 */
sealed interface SaveResult {
    data object Saved : SaveResult
    data class Rejected(val error: NameError) : SaveResult
}

/** 新規保存のときに [saveWithValidatedName] へ渡す `exceptId`。id は 1 から振られる。 */
internal const val NEW_RECORD_ID = 0

/**
 * 名前を検証してから書き込む。**3 種類とも同じ順序で同じ規則を通す**ために、手順は
 * ここ 1 か所だけに置く（トリム → 空・上限 → 同種内の重複 → 書き込み）。
 *
 * @param exceptId 更新では自分自身を重複から除く。新規は [NEW_RECORD_ID]
 */
internal suspend fun saveWithValidatedName(
    rawName: String,
    exceptId: Int,
    existsName: suspend (name: String, exceptId: Int) -> Boolean,
    write: suspend (normalizedName: String) -> Unit
): SaveResult {
    val normalized = NameRule.normalize(rawName)
    NameRule.validate(normalized)?.let { return SaveResult.Rejected(it) }
    if (existsName(normalized, exceptId)) return SaveResult.Rejected(NameError.DUPLICATE)
    write(normalized)
    return SaveResult.Saved
}
