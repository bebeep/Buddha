package com.fingertip.baselib.util

import android.os.Bundle
import me.yokeyword.fragmentation.ISupportFragment
import me.yokeyword.fragmentation.SupportFragment


/**
 * 一些fragment的反射调用
 */
object FragmentFactory {

    fun instanceRechargeFragment(): ISupportFragment {
        val clazz = Class.forName("com.lzlz.uilib.frag.RechargeFragment")
        return clazz.newInstance() as ISupportFragment
    }


    fun instanceXQFragment(anchorId: Int): SupportFragment {
        val clazz = Class.forName("com.lzlz.uilib.frag.XQFragment")
        val fragment = clazz.newInstance() as SupportFragment
        fragment.apply {
            arguments = Bundle().apply {
                putInt("AnchorId", anchorId)
            }
            return fragment
        }
    }

    fun instanceWebFragment(openUrl: String?): SupportFragment {
        val webFragment = Class.forName("com.lzlz.uilib.frag.WebFragment").newInstance() as SupportFragment
        webFragment.arguments = Bundle().apply {
            putString("openUrl", openUrl)
        }
        return webFragment
    }



}