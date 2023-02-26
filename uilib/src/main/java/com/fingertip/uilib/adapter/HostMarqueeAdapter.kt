package com.fingertip.uilib.adapter

import android.view.LayoutInflater
import android.view.View
import com.fingertip.baselib.log
import com.fingertip.baselib.util.loadHead
import com.fingertip.baselib.view.MyTextView
import com.fingertip.baselib.view.shapeview.ShapeImageView
import com.fingertip.uilib.R
import com.stx.xmarqueeview.XMarqueeView
import com.stx.xmarqueeview.XMarqueeViewAdapter

class HostMarqueeAdapter(list:List<String>): XMarqueeViewAdapter<String>(list) {
    override fun onCreateView(parent: XMarqueeView?): View {
        return LayoutInflater.from(parent?.context).inflate(R.layout.item_host_marquee, null)
    }

    override fun onBindView(parent: View?, view: View?, position: Int) {
        view?.findViewById<MyTextView>(R.id.tv_title)?.text = "呉彦祖 供灯 成功 ${mDatas[position]}"
        view?.findViewById<MyTextView>(R.id.tv_content)?.text = "修行值 +$position"
        view?.findViewById<MyTextView>(R.id.tv_time)?.text = "刚刚 $position"

        view?.findViewById<ShapeImageView>(R.id.iv_head)?.apply {
            loadHead("111")
            setOnClickListener {
                log(value = "点击头像 $position")
            }
        }

        view?.setOnClickListener {
            log(value = "点击item $position")
        }
    }
}