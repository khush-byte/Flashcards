package com.khush.cards.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CardDao {
    @Insert
    suspend fun insertCard(cardModel: CardModel)

    @Query("SELECT * FROM card_table WHERE groupId=:id")
    fun getCards(id: Int): List<CardModel>

    @Update
    suspend fun updateCard(cardModel: CardModel)

    @Delete
    suspend fun deleteCard(cardModel: CardModel)

    @Query("DELETE FROM card_table WHERE groupId=:id")
    suspend fun deleteCardByGroup(id: Int)

    @Query("SELECT (SELECT COUNT(*) FROM card_table) == 0")
    fun isEmpty(): Boolean
}