package com.khush.cards

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.khush.cards.database.CardDao
import com.khush.cards.database.CardDatabase
import com.khush.cards.database.CardModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CardActivity : AppCompatActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var cardDao: CardDao
    private lateinit var addBtn: Button
    private lateinit var wordField: EditText
    private lateinit var typeField: Spinner
    private lateinit var transcriptionField: EditText
    private lateinit var translationField: EditText
    private lateinit var sharedPreference: SharedPreferences
    private var groupId: Int = 0
    private var cardId: Int = 0
    private lateinit var items: Array<String>
    private var type = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        initApp()

        val action =  intent.getStringExtra("action")

        if(action.equals("add")){
            addBtn.text = "ADD"
        }
        else if(action.equals("edit")){
            addBtn.text = "APPLY"
        }

        addBtn.setOnClickListener {
            if(wordField.text.isNotEmpty()&&translationField.text.isNotEmpty()){
                var text = transcriptionField.text.toString()
                val transcription = text.replace("[","").replace("]","")
                if(action.equals("add")) {
                    insertCardToDatabase(
                        groupId,
                        wordField.text.toString(),
                        transcription,
                        translationField.text.toString(),
                        type
                    )
                }
                else if(action.equals("edit")){
                    updateCard(
                        cardId,
                        groupId,
                        wordField.text.toString(),
                        transcription,
                        translationField.text.toString(),
                        type
                    )
                }
                addBtn.isEnabled = false
            }else {
                Toast.makeText(applicationContext, "Please enter a word and the translation!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        typeField.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("MyTag", "hello")
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val value = parent!!.getItemAtPosition(position).toString()
                if(value == items[0]){
                    (view as TextView).setTextColor(Color.GRAY)
                }
                type = if(position==0){
                    ""
                }else{
                    value
                }
                Log.d("MyTag", type)
            }
        }
    }

    private fun initApp(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor("#111212")
            window.navigationBarColor = Color.parseColor("#111212")
        }
        wordField = findViewById(R.id.word_field)
        transcriptionField = findViewById(R.id.transcription_field)
        translationField = findViewById(R.id.translation_field)
        addBtn = findViewById(R.id.add_word_btn)
        typeField = findViewById(R.id.type_field)

        groupId = intent.getIntExtra("groupId", 0)
        cardId = intent.getIntExtra("cardId", 0)
        val word = intent.getStringExtra("word")
        val transcription = intent.getStringExtra("transcription")
        val translation = intent.getStringExtra("translation")
        val type = intent.getStringExtra("type")
        Log.d("MyTag",cardId.toString()+groupId.toString()+word+transcription+translation+type)

        sharedPreference = applicationContext.getSharedPreferences("LocalMemory", Context.MODE_PRIVATE)

        wordField.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        val db = Room.databaseBuilder(
            applicationContext,
            CardDatabase::class.java, "card_database"
        ).build()
        cardDao = db.cardDao()

        items = resources.getStringArray(R.array.type_name)

        val spinnerAdapter = object : ArrayAdapter<String>(applicationContext, R.layout.spinner_word_type, items){
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view: TextView = super.getDropDownView(position, convertView, parent) as TextView
                if(position == 0) {
                    view.setTextColor(Color.GRAY)
                } else {
                    view.setTextColor(Color.WHITE)
                }
                return view
            }
        }

        typeField.adapter = spinnerAdapter

        if (word != null) {
            wordField.setText(word)
            transcriptionField.setText(transcription)
            translationField.setText(translation)
            if (type != null) {
                val position = spinnerAdapter.getPosition(type)
                typeField.setSelection(position)
            }
        }
    }

    private fun insertCardToDatabase(group: Int, word: String, transcription: String, translation: String, type: String) {
        //Log.d("MyTag",group+word+transcription+translation)
        val editor = sharedPreference.edit()

        coroutineScope.launch {
            cardDao.insertCard(
                CardModel(0, group, word, transcription, translation, type)
            )

            val cards = cardDao.getCards(groupId)
            val index = cards.size - 1
            editor.putInt("cardIndex", index)
            editor.apply()
            //Log.d("MyTag", cards.size.toString())
        }

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        this.finish()
    }

    private fun updateCard(cardId: Int, groupId: Int, word: String, transcription: String, translation: String, type: String) {
        //Log.d("MyTag",group+word+transcription+translation)

        coroutineScope.launch {
            cardDao.updateCard(
                CardModel(cardId, groupId, word, transcription, translation, type)
            )
        }

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        this.finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        this.finish()
    }
}