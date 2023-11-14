package com.khush.cards

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.room.Room
import com.khush.cards.database.CardDao
import com.khush.cards.database.CardDatabase
import com.khush.cards.database.CardModel
import com.khush.cards.database.GroupDao
import com.khush.cards.database.GroupModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var frontAnim: AnimatorSet
    private lateinit var backAnim: AnimatorSet
    private lateinit var swipeAnim: AnimatorSet
    private lateinit var swipeAnimBack: AnimatorSet
    private lateinit var cardFront: CardView
    private lateinit var cardBack: CardView
    private lateinit var groupDropDown: Spinner
    private lateinit var clickField: ConstraintLayout
    private lateinit var addBtn: Button
    private lateinit var editBtn: Button
    private lateinit var deleteBtn: Button
    private lateinit var createGroupBtn: Button
    private lateinit var deleteGroupBtn: Button
    private lateinit var changeGroupBtn: Button
    private lateinit var importBtn: ImageButton
    private lateinit var englishWord: TextView
    private lateinit var transcription: TextView
    private lateinit var translation: TextView
    private lateinit var speakBtn: ImageButton
    private lateinit var cardDao: CardDao
    private lateinit var groupDao: GroupDao
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var cardModels: List<CardModel>
    private lateinit var items: MutableList<String>
    private lateinit var editor: SharedPreferences.Editor
    var isFront = true
    var xDown = 0
    var xUp = 0
    var cardIndex = 0
    private var groupIndex = 0
    private var tts: TextToSpeech? = null

    @SuppressLint("ClickableViewAccessibility", "CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        initApp()
        tts = TextToSpeech(applicationContext, this)

        clickField.setOnTouchListener(View.OnTouchListener { _, motionEvent ->
            when (motionEvent.action){
                MotionEvent.ACTION_DOWN -> {
                    xDown = motionEvent.x.toInt()
                }
                MotionEvent.ACTION_UP -> {
                    xUp = motionEvent.x.toInt()
                    val xMove = xDown - xUp
                    setSwipe(xMove)
                }
            }
            return@OnTouchListener true
        })

        addBtn.setOnClickListener {
            coroutineScope.launch {
                if (!groupDao.isEmpty()) {
                    val groups = groupDao.getGroups()
                    val groupId = groups[groupIndex].id
                    //Log.d("MyTag", groupId.toString())
                    val intent = Intent(this@MainActivity, CardActivity::class.java)
                    intent.putExtra("action", "add")
                    intent.putExtra("groupId", groupId)
                    startActivity(intent)
                }
            }
        }

        editBtn.setOnClickListener {
            if(cardModels.isNotEmpty()) {
                val intent = Intent(this, CardActivity::class.java)
                intent.putExtra("action", "edit")
                intent.putExtra("groupId", cardModels[cardIndex].groupId)
                intent.putExtra("cardId", cardModels[cardIndex].id)
                intent.putExtra("word", cardModels[cardIndex].word)
                intent.putExtra("transcription", cardModels[cardIndex].transcription)
                intent.putExtra("translation", cardModels[cardIndex].translation)
                startActivity(intent)
            }else{
                Toast.makeText(applicationContext, "There are no cards in this group!", Toast.LENGTH_SHORT).show()
            }
        }

        deleteBtn.setOnClickListener {
            if(cardModels.isNotEmpty()) {
                deletePopup("card")
            }else{
                Toast.makeText(applicationContext, "There are no cards in this group!", Toast.LENGTH_SHORT).show()
            }
        }

        deleteGroupBtn.setOnClickListener {
            deletePopup("group")
        }

        createGroupBtn.setOnClickListener {
            val intent = Intent(this, GroupActivity::class.java)
            startActivity(intent)
            this.finish()
        }

        groupDropDown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(groupIndex!=position){
                    cardIndex = 0
                }
                groupIndex = position
                saveGroupIndex()
                updateCardList()
            }
        }

        speakBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech(englishWord.text.toString())
            }
        }

        changeGroupBtn.setOnClickListener {
            if(cardModels.isNotEmpty()) {
                getGroupPopup()
            }else{
                Toast.makeText(applicationContext, "There are no cards in this group!", Toast.LENGTH_SHORT).show()
            }
        }

        importBtn.setOnClickListener {
            getImportPopup()
        }
    }

    private fun initApp(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor("#111212")
            window.navigationBarColor = Color.parseColor("#111212")
        }

        val scale:Float = applicationContext.resources.displayMetrics.density
        cardFront = findViewById(R.id.cardFront)
        cardBack = findViewById(R.id.cardBack)
        clickField = findViewById(R.id.clickField)
        addBtn = findViewById(R.id.add_btn)
        editBtn = findViewById(R.id.edit_btn)
        deleteBtn = findViewById(R.id.delete_btn)
        createGroupBtn = findViewById(R.id.create_group_btn)
        deleteGroupBtn = findViewById(R.id.delete_group_btn)
        groupDropDown = findViewById(R.id.group_drop_down)
        englishWord = findViewById(R.id.english)
        transcription = findViewById(R.id.transcription)
        translation = findViewById(R.id.translation)
        speakBtn = findViewById(R.id.speak_btn)
        changeGroupBtn = findViewById(R.id.change_group_btn)
        importBtn = findViewById(R.id.import_btn)

        cardFront.cameraDistance = 8000 * scale
        cardBack.cameraDistance = 8000 * scale

        frontAnim = AnimatorInflater.loadAnimator(applicationContext, R.animator.front) as AnimatorSet
        backAnim = AnimatorInflater.loadAnimator(applicationContext, R.animator.back) as AnimatorSet

        sharedPreference = applicationContext.getSharedPreferences("LocalMemory", Context.MODE_PRIVATE)
        editor = sharedPreference.edit()

        groupIndex = sharedPreference.getInt("groupIndex", 0)
        cardIndex = sharedPreference.getInt("cardIndex", 0)

        val db = Room.databaseBuilder(
            applicationContext,
            CardDatabase::class.java, "card_database"
        ).build()

        cardDao = db.cardDao()
        groupDao = db.groupDao()

        items = ArrayList()

        coroutineScope.launch {
            if(groupDao.isEmpty()){
                groupDao.insertGroup(
                    GroupModel(0, "new words")
                )
            }

            if(cardDao.isEmpty()){
                cardDao.insertCard(
                    CardModel(0, 1, "hello", "həˈləʊ", "привет")
                )
            }

            val groups = groupDao.getGroups()
            for(group in groups){
                items.add(group.group)
            }

            adapter = ArrayAdapter<String>(applicationContext, R.layout.spinner_item, items)
            groupDropDown.adapter = adapter
            groupDropDown.setSelection(groupIndex)

            val groupId = groups[groupIndex].id
            cardModels = cardDao.getCards(groupId)
            cardIndex = sharedPreference.getInt("cardIndex", 0)

            //Log.d("MyTag", cardModels.size.toString())
            initCard()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initCard(){
        if(cardModels.isNotEmpty()) {
            englishWord.text = cardModels[cardIndex].word
            if(cardModels[cardIndex].transcription.isNotEmpty()) {
                transcription.text = "[${cardModels[cardIndex].transcription}]"
            }else{
                transcription.text = ""
            }
            translation.text = cardModels[cardIndex].translation
        }else{
            englishWord.text = ""
            transcription.text = ""
            translation.text = ""
        }
    }

    private fun saveGroupIndex(){
        editor.putInt("groupIndex", groupIndex)
        editor.apply()
    }

    private fun saveCardIndex(){
        editor.putInt("cardIndex", cardIndex)
        editor.apply()
    }

    private fun updateCardList(){
        coroutineScope.launch {
            val groups = groupDao.getGroups()
            val groupId = groups[groupIndex].id
            cardModels = cardDao.getCards(groupId)

            MainScope().launch {
                initCard()
            }
        }
    }

    private fun setSwipe(x: Int){
        if(x<-50){
            //Log.d("MyTag", "left")
            setSwipeAnim("left")
        }
        else if(x>50){
            //Log.d("MyTag", "right")
            setSwipeAnim("right")
        }
        else if(x==0){
            //Log.d("MyTag", "click")
            cardFlip()
        }
    }

    private fun setSwipeAnim(swipeSide: String){
        if(swipeSide=="left") {
            swipeAnim = AnimatorInflater.loadAnimator(
                applicationContext,
                R.animator.swipeleft1
            ) as AnimatorSet
            swipeAnimBack = AnimatorInflater.loadAnimator(
                applicationContext,
                R.animator.swipeleft2
            ) as AnimatorSet

            swipeAnim.doOnEnd {
                if(cardIndex > 0){
                    cardIndex -= 1
                    initCard()
                }else{
                    cardIndex = cardModels.size - 1
                    initCard()
                }
                saveCardIndex()
                //Log.d("MyTag", sharedPreference.getInt("cardIndex", 0).toString())
            }
        }
        else if(swipeSide=="right") {
            swipeAnim = AnimatorInflater.loadAnimator(
                applicationContext,
                R.animator.swiperight1
            ) as AnimatorSet
            swipeAnimBack = AnimatorInflater.loadAnimator(
                applicationContext,
                R.animator.swiperight2
            ) as AnimatorSet

            swipeAnim.doOnEnd {
                val cardSize = cardModels.size-1
                if(cardIndex < cardSize){
                    cardIndex+=1
                    initCard()
                }else{
                    cardIndex = 0
                    initCard()
                }
                Log.d("MyTag", cardIndex.toString())
                saveCardIndex()
            }
        }

        clickField.isEnabled = false

        if(isFront) {
            swipeAnim.setTarget(cardFront)
            swipeAnimBack.setTarget(cardFront)
            cardBack.visibility = View.INVISIBLE
            swipeAnim.start()
            swipeAnim.doOnEnd {
                swipeAnimBack.start()
                swipeAnimBack.doOnEnd {
                    cardFront.alpha = 1F
                    cardFront.translationX = 0F
                    cardBack.visibility = View.VISIBLE
                    clickField.isEnabled = true
                }
            }

        }else{
            swipeAnim.setTarget(cardBack)
            swipeAnimBack.setTarget(cardBack)
            cardFront.visibility = View.INVISIBLE
            swipeAnim.start()
            swipeAnim.doOnEnd {
                swipeAnimBack.start()
                swipeAnimBack.doOnEnd {
                    cardBack.alpha = 1F
                    cardBack.translationX = 0F
                    cardFront.visibility = View.VISIBLE
                    clickField.isEnabled = true
                }
            }
        }
    }

    private fun cardFlip(){
        if(isFront){
            frontAnim.setTarget(cardFront)
            backAnim.setTarget(cardBack)
            frontAnim.start()
            backAnim.start()
            backAnim.doOnStart {
                clickField.isEnabled = false
            }
            backAnim.doOnEnd {
                clickField.isEnabled = true
            }
            isFront = false
            speakBtn.isEnabled = false
        }else{
            frontAnim.setTarget(cardBack)
            backAnim.setTarget(cardFront)
            backAnim.start()
            frontAnim.start()
            frontAnim.doOnStart {
                clickField.isEnabled = false
            }
            frontAnim.doOnEnd {
                clickField.isEnabled = true
            }
            isFront = true
            speakBtn.isEnabled = true
        }
    }

    private fun deletePopup(type: String){
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Removal process")
        if(type == "group") {
            builder.setMessage("Do you really want to delete the selected group with all its cards?")
        }
        if(type == "card") {
            builder.setMessage("Do you really want to delete the selected card?")
        }

        builder.setPositiveButton("YES") { dialog, _ ->
            if(type == "group") {
                deleteGroup()
            }
            if(type == "card") {
                deleteCard()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(
            "NO"
        ) { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    private fun deleteGroup(){
        coroutineScope.launch {
            if (!groupDao.isEmpty()) {
                val groups = groupDao.getGroups()
                val groupSize = groups.size
                if(groupSize > 1) {
                    groupDao.deleteGroup(groups[groupIndex])

                    MainScope().launch {
                        adapter.remove(items[groupIndex])
                        adapter.notifyDataSetChanged()
                        groupIndex = 0
                        groupDropDown.setSelection(0)
                        saveGroupIndex()
                        cardIndex = 0
                        saveCardIndex()
                        updateCardList()
                    }
                }
            }
        }
    }

    private fun deleteCard(){
        coroutineScope.launch {
            if(!cardDao.isEmpty()) {
                if(cardModels.isNotEmpty()) {
                    cardDao.deleteCard(cardModels[cardIndex])
                    if(cardIndex != 0) {
                        cardIndex -= 1
                        saveCardIndex()
                    }
                    updateCardList()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun textToSpeech(text: String){
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("MyTag","The Language not supported!")
            }else{
                tts!!.language = Locale.UK
                tts!!.setSpeechRate(0.8F)
//                val am = getSystemService(AUDIO_SERVICE) as AudioManager
//                am.setStreamVolume(AudioManager.STREAM_MUSIC, 12, 0)
            }
        }
    }

    public override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    @SuppressLint("InflateParams")
    private fun getGroupPopup(){
        val popUpView = layoutInflater.inflate(R.layout.group_popup, null);
        val popup = PopupWindow(popUpView, ConstraintLayout.LayoutParams.FILL_PARENT,
        ConstraintLayout.LayoutParams.WRAP_CONTENT, true)
        popup.animationStyle = android.R.style.Animation_Dialog
        popup.showAtLocation(popUpView, Gravity.CENTER, 0, 0)

        val popupGroupDropdown = popUpView.findViewById<Spinner>(R.id.popup_drop_down)
        val popupApplyBtn = popUpView.findViewById<Button>(R.id.popup_apply_btn)
        val popupCancelBtn = popUpView.findViewById<Button>(R.id.popup_cancel_btn)

//        var list: List<String> = items.toList()
//        val popupItems = ArrayList(list)
//        popupItems.removeAt(groupIndex)

        val popupGroupAdapter = ArrayAdapter<String>(applicationContext, R.layout.spinner_item, items)
        popupGroupDropdown.adapter = popupGroupAdapter
        popupGroupDropdown.setSelection(groupIndex)

        var popupGroupIndex = 0
        popupGroupDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                popupGroupIndex = position
            }
        }

        popupApplyBtn.setOnClickListener {
            moveCardToNewGroup(popupGroupIndex)
            popup.dismiss()
        }

        popupCancelBtn.setOnClickListener {
            popup.dismiss()
        }
    }

    private fun moveCardToNewGroup(popupGroupIndex: Int){
        coroutineScope.launch {
            if(!cardDao.isEmpty()) {
                val groups = groupDao.getGroups()
                val popupGroupId =  groups[popupGroupIndex].id

                cardDao.updateCard(
                    CardModel(cardModels[cardIndex].id, popupGroupId, cardModels[cardIndex].word, cardModels[cardIndex].transcription, cardModels[cardIndex].translation)
                )

                if(cardIndex != 0) {
                    cardIndex -= 1
                    saveCardIndex()
                }
                updateCardList()
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun getImportPopup(){
        val popUpView = layoutInflater.inflate(R.layout.import_popup, null);
        val popup = PopupWindow(popUpView, ConstraintLayout.LayoutParams.FILL_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT, true)
        popup.animationStyle = android.R.style.Animation_Dialog
        popup.showAtLocation(popUpView, Gravity.CENTER, 0, 0)

        val cancelImportBtn = popUpView.findViewById<Button>(R.id.import_cancel_btn)
        val importGroupBtn = popUpView.findViewById<Button>(R.id.popup_import_btn)
        val exportGroupBtn = popUpView.findViewById<Button>(R.id.popup_export_btn)

        cancelImportBtn.setOnClickListener {
            popup.dismiss()
        }

        importGroupBtn.setOnClickListener {

        }

        exportGroupBtn.setOnClickListener {

        }
    }
}