package com.fingertip.baselib.top

import android.app.Activity
import android.app.Application
import com.blankj.utilcode.util.Utils
import com.fingertip.baselib.bean.SensitiveWordsBean
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

        //敏感词
        var sensitiveWordsBean: SensitiveWordsBean? = null

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