package com.fingertip.uilib.app

import com.blankj.utilcode.util.LanguageUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.language.LanguageUtil


/**
 * 主application
 */
class UIApplication: AppApplication() {

    override fun onCreate() {
        super.onCreate()
        val local = LanguageUtils.getSystemLanguage()//当前系统语言
        val customLan = SPUtils.getInstance().getString("customLanguage")
        LogUtils.e("language===============${local.language}  |  $customLan")
        if (customLan.isNullOrEmpty()) {
            if (LanguageUtil.languages.contains(local.language))SPUtils.getInstance().put("customLanguage",local.language)
            else SPUtils.getInstance().put("customLanguage",LanguageUtil.languages[0])
        }

        LogUtils.e("language===============${LanguageUtils.getAppContextLanguage().language}")

    }
}