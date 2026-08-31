package io.github.obaya884.rebuy.data.destination

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 買い物の行き先（店）。カテゴリーと同じ形を持つ。
 *
 * [sortOrder] は手動の並び順で、新規作成は末尾に置く（採番は
 * `DestinationRepository` が行う。データモデル定義書 §6）。
 */
@Entity(
    tableName = "destinations",
    indices = [Index(value = ["name"], unique = true)]
)
data class Destination(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
