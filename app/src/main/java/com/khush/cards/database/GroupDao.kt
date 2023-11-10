package com.khush.cards.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GroupDao {
    @Insert
    suspend fun insertGroup(groupModel: GroupModel)

    @Query("SELECT * FROM group_table")
    suspend fun getGroups(): List<GroupModel>

    @Query("SELECT * FROM group_table WHERE 'id'=:id")
    suspend fun getGroup(id: Int): GroupModel

    @Delete
    suspend fun deleteGroup(groupModel: GroupModel)

    @Query("SELECT (SELECT COUNT(*) FROM group_table) == 0")
    fun isEmpty(): Boolean
}