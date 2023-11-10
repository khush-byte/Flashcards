package com.khush.cards

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.khush.cards.database.CardDatabase
import com.khush.cards.database.GroupModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group)

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor("#111212")
            window.navigationBarColor = Color.parseColor("#111212")
        }

        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val groupField = findViewById<EditText>(R.id.group_field)
        val createBtn = findViewById<Button>(R.id.create_btn)
        groupField.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        val sharedPreference = applicationContext.getSharedPreferences("LocalMemory", Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()

        val db = Room.databaseBuilder(
            applicationContext,
            CardDatabase::class.java, "card_database"
        ).build()
        val groupDao = db.groupDao()

        createBtn.setOnClickListener {
            if(groupField.text.isNotEmpty()){
                coroutineScope.launch {
                    groupDao.insertGroup(
                        GroupModel(0, groupField.text.toString())
                    )
                    val groups = groupDao.getGroups()
                    val index = groups.size - 1

                    editor.putInt("groupIndex", index)
                    editor.putInt("cardIndex", 0)
                    editor.apply()
                }

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                this.finish()
            }else{
                Toast.makeText(applicationContext, "Group name field is empty!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        this.finish()
    }
}