package com.khush.cards.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card_table")
data class CardModel(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "groupId")
    var groupId: Int,

    @ColumnInfo(name = "word")
    var word: String,

    @ColumnInfo(name = "transcription")
    var transcription: String,

    @ColumnInfo(name = "translation")
    var translation: String
)
