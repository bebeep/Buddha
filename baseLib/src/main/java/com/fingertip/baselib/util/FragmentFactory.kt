package com.fingertip.baselib.util

import android.os.Bundle
import com.lzlz.uselib.bianjava.PP_PersonData
import com.lzlz.uselib.cons.PP_Constants
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


    fun instanceRequestCallFragment(anchorId: Int, from: String): SupportFragment {
        val clazz = Class.forName("com.lzlz.agoralib.frag.RequestCallFragment")
        val fragment = clazz.newInstance() as SupportFragment
        fragment.apply {
            arguments = Bundle().apply {
                putInt(PP_Constants.Extra.AnchorId, anchorId)
                putString(PP_Constants.Extra.From, from)
            }
        }
        return fragment
    }

    fun instanceXQFragment(anchorId: Int, userType: Int = PP_Constants.UserType.ANCHOR, isFromLive: Boolean = false): SupportFragment {
        val clazz = Class.forName("com.lzlz.uilib.frag.XQFragment")
        val fragment = clazz.newInstance() as SupportFragment
        fragment.apply {
            arguments = Bundle().apply {
                putInt("AnchorId", anchorId)
                putInt("UserType", userType)
                putBoolean(PP_Constants.Extra.IsJumpFromLive, isFromLive)
            }
            return fragment
        }
    }

    fun instanceSVipFragment(): SupportFragment {
        val clazz = Class.forName("com.lzlz.uilib.frag.SJHYFragment")
        return clazz.newInstance() as SupportFragment
    }

    fun instanceMessageChatFragment(personData: PP_PersonData): SupportFragment {
        val clazz = Class.forName("com.lzlz.imlib.frag.MessageChatFragment")
        val fragment = clazz.newInstance() as SupportFragment
        fragment.apply {
            arguments = Bundle().apply {
                putSerializable(PP_Constants.Extra.YHData, personData)
            }
        }
        return fragment
    }

    fun instanceWebFragment(openUrl: String?): SupportFragment {
        val webFragment = Class.forName("com.lzlz.uilib.frag.WebFragment").newInstance() as SupportFragment
        webFragment.arguments = Bundle().apply {
            putString("openUrl", openUrl)
        }
        return webFragment
    }


    fun instanceMomentDetails(momentId: Int?): SupportFragment {
        val momentFragment = Class.forName("com.tigeer.moment_lib.frag.MomentDetailsFrag").newInstance() as SupportFragment
        momentFragment.arguments = Bundle().apply {
            putInt("MOMENT_ID", momentId?:0)
        }
        return momentFragment
    }

}