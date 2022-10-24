package com.unity3d.player

import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.fingertip.baselib.event_bus.MessageEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class MyUnityActivity:UnityPlayerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        senMsg()
    }


    @Subscribe
    fun onMessage(event: MessageEvent){
        when(event.what){
            1->{

            }
        }
    }

    fun callUnity(obj:String,func:String,params:String){
        //第一个参数是挂载脚本的gameobject，第二个参数是方法名，最后是方法参数
        UnityPlayer.UnitySendMessage(obj,func,params)
    }


    fun callAndroid(params: String){
        Log.e("TAG","从Unity过来：$params")
    }

    fun callFinish(){
        finish()
    }

    private var num = 0
    private val handler = Handler{
        if (it.what == 1){
            senMsg()
            callUnity("GameManager","callUnity","从安卓中传递过来：${num++}")
        }
        false
    }

    private fun senMsg(){
        handler.sendEmptyMessageDelayed(1,2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}