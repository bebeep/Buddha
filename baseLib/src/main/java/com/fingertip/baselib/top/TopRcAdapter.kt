package com.fingertip.baselib.top

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.fingertip.baselib.R
import java.lang.reflect.Method

@Suppress("UNCHECKED_CAST")
abstract class TopRcAdapter<T, VH : TopRcAdapter.TopRcViewHolder>(
    val context: Context
) : RecyclerView.Adapter<VH>() {

    val TAG by lazy { javaClass.simpleName }

    val mlist = ArrayList<T>()

    /** layoutId -> Binding.bind(View) 方法，找不到时也缓存 null，避免反复 Class.forName */
    private val bindMethodCache = HashMap<Int, Method?>()

    //region 通用空态占位（图 + 文）
    // 原理：adapter attach 到 RecyclerView 时，把 layout_common_empty 挂到 RV 的父容器上，
    // 复制 RV 的布局参数（ConstraintLayout 会复制约束锚点），位置与 RV 完全重合。
    // 数据变化通过 AdapterDataObserver 感知：首次灌过数据且列表为空时显示。

    /** 空态文案；设为 null 表示不显示占位 */
    var emptyText: CharSequence? = context.getString(R.string.empty_common_tip)
        set(value) {
            field = value
            refreshEmptyTip()
        }

    /** 是否启用空态占位（聊天页、子列表等场景可关掉） */
    var showEmptyTip: Boolean = true
        set(value) {
            field = value
            refreshEmptyTip()
        }

    private var emptyTipView: View? = null
    private var emptyTipTextView: android.widget.TextView? = null
    private var attachedParent: ViewGroup? = null

    /** 首次灌数据后才开始显示空态，避免加载中闪现"暂无内容" */
    private var dataLoaded = false

    init {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = refreshEmptyTip()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = refreshEmptyTip()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = refreshEmptyTip()
        })
    }

    private fun refreshEmptyTip() {
        val overlay = emptyTipView ?: return
        val show = showEmptyTip && dataLoaded && mlist.isEmpty()
        if (show) {
            emptyTipTextView?.text = emptyText ?: context.getString(R.string.empty_common_tip)
        }
        overlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val parent = recyclerView.parent as? ViewGroup ?: return
        //ScrollView 只允许一个直接子 View，不能挂占位层——这类场景直接禁用空态
        if (parent is android.widget.ScrollView ||
            parent is androidx.core.widget.NestedScrollView ||
            parent is android.widget.HorizontalScrollView
        ) {
            showEmptyTip = false
            return
        }
        //重复 attach（页面重建复用 adapter）先移旧的
        emptyTipView?.let { old -> (old.parent as? ViewGroup)?.removeView(old) }
        attachedParent = parent
        val overlay = LayoutInflater.from(recyclerView.context)
            .inflate(R.layout.layout_common_empty, parent, false)
        emptyTipTextView = overlay.findViewById(R.id.tv_empty_text)
        overlay.layoutParams = buildOverlayParams(recyclerView)
        parent.addView(overlay)
        emptyTipView = overlay
        refreshEmptyTip()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        emptyTipView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        emptyTipView = null
        emptyTipTextView = null
        attachedParent = null
    }

    /** 占位层布局参数：与 RV 同区域。ConstraintLayout 复制约束锚点，其余容器居中铺满 */
    private fun buildOverlayParams(rv: RecyclerView): ViewGroup.LayoutParams {
        val src = rv.layoutParams
        val parent = attachedParent
        return when {
            parent is ConstraintLayout && src is ConstraintLayout.LayoutParams -> {
                ConstraintLayout.LayoutParams(src.width, src.height).apply {
                    topToBottom = src.topToBottom; topToTop = src.topToTop
                    bottomToBottom = src.bottomToBottom; bottomToTop = src.bottomToTop
                    startToStart = src.startToStart; startToEnd = src.startToEnd
                    endToEnd = src.endToEnd; endToStart = src.endToStart
                    horizontalBias = src.horizontalBias; verticalBias = src.verticalBias
                    matchConstraintDefaultWidth = src.matchConstraintDefaultWidth
                    matchConstraintDefaultHeight = src.matchConstraintDefaultHeight
                    topMargin = src.topMargin; bottomMargin = src.bottomMargin
                    leftMargin = src.leftMargin; rightMargin = src.rightMargin
                }
            }
            parent is FrameLayout -> {
                FrameLayout.LayoutParams(src.width, src.height, Gravity.CENTER)
            }
            parent is LinearLayout -> {
                LinearLayout.LayoutParams(src.width, src.height).apply { gravity = Gravity.CENTER }
            }
            else -> {
                ViewGroup.MarginLayoutParams(src.width, src.height)
            }
        }
    }
    //endregion

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layoutId = initLayoutId(viewType)
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)
        val binding = createBindingFromView(view, layoutId)
        return TopRcViewHolder(view, binding) as VH
    }

    override fun getItemCount(): Int = mlist.size

    /**
     * 布局文件ID
     */
    protected abstract fun initLayoutId(viewType: Int): Int

    fun data() = mlist

    open fun initData(list: List<T>?) {
        list?.let {
            mlist.clear()
            mlist.addAll(list)
            dataLoaded = true
            notifyDataSetChanged()
        }
    }

    open fun addData(t: T?,position: Int = -1) {
        t?.let {
            if (position  == -1)
            {
                mlist.add(t)
            }
            else
            {
                mlist.add(0,t)
            }
            dataLoaded = true
            notifyDataSetChanged()
        }
    }

    open fun addData(list: List<T>?) {
        list?.let {
            mlist.addAll(list)
            dataLoaded = true
            notifyDataSetChanged()
        }
    }

    /**
     * 请求失败/本地无数据时强制显示空态占位（清空列表并标记已加载）。
     * 正常数据流不需要调它——initData/addData 会自动处理。
     */
    fun showEmptyNow(text: CharSequence? = null) {
        text?.let { emptyText = it }
        mlist.clear()
        dataLoaded = true
        notifyDataSetChanged()
    }

    fun get(position: Int) = if (position >= 0 && position < mlist.size) mlist[position] else null

    open class TopRcViewHolder(view: View, private var binding: ViewBinding? = null) :
        RecyclerView.ViewHolder(view) {

        /**
         * 获取ViewBinding实例
         * @throws IllegalStateException 如果该ViewHolder未启用ViewBinding
         */
        fun <VB : ViewBinding> getBinding(): VB {
            return binding as? VB
                ?: throw IllegalStateException(
                    "ViewBinding is not available. Ensure ViewBinding is enabled in build.gradle."
                )
        }
    }

    /**
     * 从已inflate的View创建ViewBinding实例
     * 使用 XxxBinding.bind(view) 方式，比 inflate 反射更可靠
     * 搜索策略: 资源所在包 → 子类所在包 → 应用包名
     */
    private fun createBindingFromView(view: View, layoutId: Int): ViewBinding? {
        //Class.forName + getMethod 比较贵，滑动时每个 item 都做一次会掉帧，按 layoutId 缓存结果
        //（用 containsKey 判断，找不到时也把 null 缓存下来，否则每次都要重新 Class.forName 一次失败）
        val bindMethod = if (bindMethodCache.containsKey(layoutId)) {
            bindMethodCache[layoutId]
        } else {
            findBindMethod(context, layoutId).also { bindMethodCache[layoutId] = it }
        } ?: return null
        return try {
            bindMethod.invoke(null, view) as? ViewBinding
        } catch (e: Exception) {
            null
        }
    }

    private fun findBindMethod(context: Context, layoutId: Int): Method? {
        return try {
            val res = context.resources
            val layoutName = res.getResourceEntryName(layoutId)
            val className = layoutName.split("_")
                .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + "Binding"
            val resPackage = res.getResourcePackageName(layoutId)
            val subClassPackage = javaClass.`package`?.name
            findBindingClass(className, resPackage, subClassPackage)
                ?.getMethod("bind", View::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun findBindingClass(className: String, vararg searchPackages: String?): Class<*>? {
        val tried = mutableSetOf<String>()
        // 1. 搜索提供的包名（资源包、子类包），逐级向上
        for (startPkg in searchPackages) {
            if (startPkg == null) continue
            var pkg: String? = startPkg
            while (pkg != null && pkg !in tried) {
                tried.add(pkg)
                try { return Class.forName("$pkg.databinding.$className") } catch (_: Exception) {}
                pkg = if ('.' in pkg) pkg.substringBeforeLast('.') else null
            }
        }
        // 2. 回退到应用包名
        val appPkg = context.packageName
        if (appPkg !in tried) {
            try { return Class.forName("$appPkg.databinding.$className") } catch (_: Exception) {}
        }
        return null
    }
}
