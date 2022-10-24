package com.fingertip.buddha

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.unity3d.player.MyUnityActivity
import me.yokeyword.fragmentation.SupportActivity

class MainActivity : SupportActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_game).setOnClickListener {
            startActivity(Intent(this, MyUnityActivity::class.java))
        }
    }
}