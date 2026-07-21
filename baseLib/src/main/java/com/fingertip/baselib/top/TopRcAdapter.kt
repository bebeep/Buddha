package com.fingertip.baselib.top

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

@Suppress("UNCHECKED_CAST")
abstract class TopRcAdapter<T, VH : TopRcAdapter.TopRcViewHolder>(
    val context: Context
) : RecyclerView.Adapter<VH>() {

    val TAG by lazy { javaClass.simpleName }

    val mlist = ArrayList<T>()

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
            notifyDataSetChanged()
        }
    }

    open fun addData(list: List<T>?) {
        list?.let {
            mlist.addAll(list)
            notifyDataSetChanged()
        }
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
        return try {
            val res = context.resources
            val layoutName = res.getResourceEntryName(layoutId)
            val className = layoutName.split("_")
                .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + "Binding"
            val resPackage = res.getResourcePackageName(layoutId)
            val subClassPackage = javaClass.`package`?.name
            findBindingClass(className, resPackage, subClassPackage)
                ?.getMethod("bind", View::class.java)
                ?.invoke(null, view) as? ViewBinding
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
