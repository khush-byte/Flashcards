package com.khush.cards

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart


class MainActivity : AppCompatActivity() {
    private lateinit var frontAnim: AnimatorSet
    private lateinit var backAnim: AnimatorSet
    private lateinit var swipeAnim: AnimatorSet
    private lateinit var swipeAnimBack: AnimatorSet
    private lateinit var cardFront: CardView
    private lateinit var cardBack: CardView
    private lateinit var groupDropDown: Spinner
    private lateinit var clickField: ConstraintLayout
    var isFront = true
    var xDown = 0
    var xUp = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initApp()
        initGroupDropDown()

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
    }

    private fun setSwipe(x: Int){
        if(x<-100){
            //Log.d("MyTag", "left")
            setSwipeAnim("left")
        }
        else if(x>150){
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

    private fun initGroupDropDown() {
        val items = arrayOf("test1", "test2", "test3")
        val adapter = ArrayAdapter<String>(
            this,
            R.layout.spinner_item,
            items
        )
        groupDropDown.adapter = adapter
    }

    private fun initApp(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK
        }

        val scale:Float = applicationContext.resources.displayMetrics.density
        cardFront = findViewById(R.id.cardFront)
        cardBack = findViewById(R.id.cardBack)
        clickField = findViewById(R.id.clickField)
        groupDropDown = findViewById(R.id.groupDropDown)

        cardFront.cameraDistance = 8000 * scale
        cardBack.cameraDistance = 8000 * scale

        frontAnim = AnimatorInflater.loadAnimator(applicationContext, R.animator.front) as AnimatorSet
        backAnim = AnimatorInflater.loadAnimator(applicationContext, R.animator.back) as AnimatorSet
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
        }
    }
}