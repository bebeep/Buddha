package com.fingertip.uilib.widgets.bottom_menu

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.lzlz.toplib.extention.gone
import com.lzlz.toplib.extention.visible

class BottomMenu @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr){

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        initTabBarUrl()
    }

    private var msgMenu: View? = null
    private var momentMenu: View? = null

    private var lastSelectMenu: View? = null

    private lateinit var tabBarNormal :ArrayList<String>
    private lateinit var tabBarSelect :ArrayList<String>
    private lateinit var imageViewList :ArrayList<ImageView>
    // 本地兜底图标，顺序必须与 MainFragment 的 fragmentList 一致
    // 海外版 5 tab：首页 / 消息 / 拜佛 / 佛友圈 / 我的（排行榜已下线）
    private val localIcon = listOf(
        R.drawable.tab_host_selector,
        R.drawable.tab_chat_selector,
        R.drawable.tab_baifo_selector,
        R.drawable.tab_post_selector,
        R.drawable.tab_me_selector
    )

    //当前 tab 数：服务端下发的 tabBarConfig URL 列表长度与此不符时整组弃用，
    //防止 tab 插入后下发列表错位加载错图
    private val tabCount = localIcon.size

    fun initTabBarUrl(){
        tabBarNormal = ArrayList()
        tabBarSelect = ArrayList()
        imageViewList = ArrayList()
        val normal = GlobalConfig.tabBarConfig?.normal
        val selected = GlobalConfig.tabBarConfig?.selected
        //数量不匹配（旧版 4 tab 配置）则整体忽略，全用本地兜底图标
        if (normal?.size == tabCount) tabBarNormal.addAll(normal)
        if (selected?.size == tabCount) tabBarSelect.addAll(selected)
    }

    var menuSelectCallback: MenuSelectCallback? = null
    fun addMenuItem(menuItem: BottomMenuItem) {
        val menuItemView = LayoutInflater.from(context).inflate(R.layout.bottom_menu_item, this, false)
        val param = LayoutParams(0, LayoutParams.MATCH_PARENT)
        param.weight = 1f
        addView(menuItemView, param)
        val count = childCount
        if (tabBarNormal.size > count-1){
            menuItemView.findViewById<ImageView>(R.id.iv_tab_icon).loadImg(tabBarNormal[count-1], holder = localIcon[count-1])
        }else menuItemView.findViewById<ImageView>(R.id.iv_tab_icon).setImageResource(menuItem.menuRes)
        imageViewList.add(menuItemView.findViewById(R.id.iv_tab_icon))

        menuItemView.setOnClickListener {
            if (lastSelectMenu == it && it != momentMenu)
                return@setOnClickListener
            setSelect(count-1)
            lastSelectMenu = it
            menuSelectCallback?.onMenuSelect(count - 1)
        }

        if (menuItem.hasMsg) {
            msgMenu = menuItemView
        }

        if (menuItem.hasRedPoint) {
            momentMenu = menuItemView
        }

        if (menuItem.initSelect) {
            menuItemView.postDelayed({menuItemView.performClick()},500)
        }
    }


    private fun setSelect(selectIndex:Int){
        for (iv in imageViewList.withIndex()){
            if (iv.index == selectIndex){
                if (tabBarSelect.size > iv.index)iv.value.loadImg(tabBarSelect[iv.index], holder = localIcon[iv.index])
                else  iv.value.setImageResource(localIcon[iv.index])
            }else{

                if (tabBarNormal.size > iv.index) iv.value.loadImg(tabBarNormal[iv.index], holder = localIcon[iv.index])
                else  iv.value.setImageResource(localIcon[iv.index])
            }
            iv.value.isSelected = selectIndex == iv.index
        }
    }

    fun setUnReadMsgCount(count: Int) {
        if (count > 0) {
            msgMenu?.findViewById<android.widget.TextView>(R.id.tv_tab_msg_count)?.let {
                it.text = if (count > 99) "99+" else count.toString()
                it.visible()
            }
        } else {
            msgMenu?.findViewById<View>(R.id.tv_tab_msg_count)?.gone()
        }
    }


    fun showRedPoint(visible:Boolean = false){
        if (visible)momentMenu?.findViewById<View>(R.id.v_num_moment)?.visible() else momentMenu?.findViewById<View>(R.id.v_num_moment)?.gone()
    }

    fun isMomentPointShow() = momentMenu?.findViewById<View>(R.id.v_num_moment)?.isShown?:false

    fun selectTab(tabIndex: Int) {
        getChildAt(tabIndex)?.performClick()
    }

    interface MenuSelectCallback {
        fun onMenuSelect(pos: Int)
    }
}