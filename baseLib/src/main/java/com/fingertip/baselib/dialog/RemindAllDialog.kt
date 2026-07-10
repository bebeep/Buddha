package com.fingertip.baselib.dialog

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.fingertip.baseLib.R
import com.fingertip.baselib.top.TopDialog
import com.lzlz.toplib.extention.visible

/**
 * 通用的建议弹窗
 */
class RemindAllDialog(
    context: Context,
    private val endAction: View.OnClickListener,
    private val tvContent:String?= "content",
    private val tvTitle:String?= "title",
    private val startAction: View.OnClickListener?=null,
    private val drawbleId:Int?=0,
    private val textNo: String? = "no",
    private val textYes: String? = "yes"
) : TopDialog(context) {
    override fun attachLayoutRes(): Int = R.layout.dialog_remind

    override fun onViewInit(view: View?) {
        val tv_content = view?.findViewById<android.widget.TextView>(R.id.tv_content)
        val tv_title = view?.findViewById<android.widget.TextView>(R.id.tv_title)
        val iv_hint = view?.findViewById<android.widget.ImageView>(R.id.iv_hint)
        val btn_no = view?.findViewById<android.widget.Button>(R.id.btn_no)
        val btn_yes = view?.findViewById<android.widget.Button>(R.id.btn_yes)

        tv_content?.text = tvContent
        tvTitle?.run {
            tv_title?.text=this
        }

        if (drawbleId!=0){
            iv_hint?.setImageDrawable(ContextCompat.getDrawable(context,drawbleId?:R.mipmap.icon_sc_account))
        }

        textNo?.let {
            btn_no?.visible()
            btn_no?.text = it
        }

        textYes?.let {
            btn_yes?.visible()
            btn_yes?.text = it
        }

        btn_no?.setOnClickListener {
            dismiss()
            startAction?.onClick(it)
        }

        btn_yes?.setOnClickListener {
            dismiss()
            endAction.onClick(it)
        }
    }
}