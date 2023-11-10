package com.khush.cards.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CardModel::class, GroupModel::class], version = 1)
abstract class CardDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun groupDao(): GroupDao
}