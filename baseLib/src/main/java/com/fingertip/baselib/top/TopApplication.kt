package com.fingertip.baselib.top

import android.app.Activity
import android.app.Application
import com.blankj.utilcode.util.Utils
import iknow.android.utils.BaseUtils
import java.util.HashMap

open class TopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Utils.init(instance)
        BaseUtils.init(this)
    }

    companion object {
        @JvmStatic
        lateinit var instance: Application


        /*************************** activity 管理 start ************************/
        val map = HashMap<String, Activity?>()
        fun addActivity(name:String,activity: Activity){
            map[name] = activity
        }
        private val momentActivities = arrayOf("PostActivity","PreviewVideoActivity","EasyPhotosActivity")
        fun removeMomentActivities(){
            for (momentActivity in momentActivities){
                map[momentActivity]?.finish()
                map[momentActivity] = null
            }
            map.clear()
        }
        /*************************** activity 管理 end ************************/
    }
}