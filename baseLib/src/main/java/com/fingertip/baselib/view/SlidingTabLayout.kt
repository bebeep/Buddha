package com.fingertip.baselib.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.blankj.utilcode.util.SizeUtils

class SlidingTabLayout : AxSlidingTabLayout {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
//        try {
//            Field field = SlidingTabLayout.class.getDeclaredField("mTabsContainer");
//            field.setAccessible(true);
//            LinearLayout mTabsContainer = (LinearLayout)(field.get(this));
//            mTabsContainer.setGravity(Gravity.BOTTOM);
//        } catch (IllegalAccessException | NoSuchFieldException e) {
//            e.printStackTrace();
//        }
    }

    override fun setViewPager(vp: ViewPager, titles: Array<String>) {
        try {
            val field = AxSlidingTabLayout::class.java.getDeclaredField("mCurrentTab")
            field.isAccessible = true
            field[this] = 0
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        }
        super.setViewPager(vp, titles)
        vp.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                setSelectTabSize(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        if (titles.size > 0) {
            setSelectTabSize(0)
        }
    }

    private var lastSelectTitle: TextView? = null
    private fun setSelectTabSize(index: Int) {
        val normalTextSize = 18f
        val selectTextSize = 24f
        if (lastSelectTitle != null) {
            var str= SpannableString(lastSelectTitle?.text);
            str.setSpan( ForegroundColorSpan(Color.parseColor("#767B7B")), 1,2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            lastSelectTitle?.text=str
            lastSelectTitle?.layoutParams=getLp(8f)
            setAnim(lastSelectTitle!!, selectTextSize, normalTextSize)
        }
        val titleView = getTitleView(index)
        var str= SpannableString(titleView.text);
        str.setSpan( ForegroundColorSpan(Color.parseColor("#3EEBD5")), 1,2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        titleView.text = str
        titleView.layoutParams=getLp(7f)
        setAnim(titleView, normalTextSize, selectTextSize)
        lastSelectTitle = titleView
    }

    private fun setAnim(textView: TextView, start: Float, end: Float) {
        val animator = ValueAnimator.ofFloat(start, end)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            textView.textSize = value
        }
        animator.duration = 200
        animator.start()
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        val count = tabCount
        for (i in 0 until count) {
            val titleView = getTitleView(i)
//            titleView.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "gilroy-bold-4.otf"));

            titleView.layoutParams = getLp(8f)
        }
    }

    fun getLp(margin:Float):RelativeLayout.LayoutParams{
        val lp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lp.bottomMargin = SizeUtils.dp2px(margin)
        return lp
    }
}