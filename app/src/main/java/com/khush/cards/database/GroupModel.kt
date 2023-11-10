package com.khush.cards.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_table")
data class GroupModel(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "group")
    var group: String
)
