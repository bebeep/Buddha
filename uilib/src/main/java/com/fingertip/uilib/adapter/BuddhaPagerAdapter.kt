package com.fingertip.uilib.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.uilib.databinding.ItemBuddhaPageBinding
import com.fingertip.uilib.view.SceneGeometry
import kotlin.math.abs

/**
 * 佛龛轮播适配器（ViewPager2）
 *
 * 无限循环：itemCount 放大 [LOOPS] 倍，position 对 items.size 取模还原真实佛像；
 * 初始定位到中间一圈（[HALF_LOOPS]），单次会话滑不到头。
 * 单页是完整分层结构（佛龛/佛光/佛像/净瓶/贡品/香烛/佛灯，见 item_buddha_page.xml），
 * 渲染由 [pageBinder] 注入，各层随轮播一体滑动。
 *
 * 本类负责两件与"分层"强相关的事：
 * 1. 场景点击派发：所有层满屏，点击统一由最上层 [Holder.touch] 收口，
 *    按画布坐标判定落在佛灯/佛龛上再回调（否则上层会把下层点击全吃掉）。
 * 2. 局部缩放定位：灯焰、香烛的 pivot 要用控件实际尺寸换算画布锚点，
 *    layout 之前拿不到宽高，所以把比例存在 Holder 上，[applyGeometry] 在
 *    bind 与每次 layout 变化时统一落一次。
 */
class BuddhaPagerAdapter : RecyclerView.Adapter<BuddhaPagerAdapter.Holder>() {

    companion object {
        private const val LOOPS = 500
        private const val HALF_LOOPS = LOOPS / 2
    }

    private var items: List<BuddhaConfig> = emptyList()

    /** 单页渲染回调：Holder + 佛像配置 */
    var pageBinder: ((Holder, BuddhaConfig) -> Unit)? = null

    fun submit(list: List<BuddhaConfig>) {
        items = list
        notifyDataSetChanged()
    }

    /** 真实佛像数量 */
    fun size(): Int = items.size

    /** 真实佛像列表（点击跳装扮需要当前佛像 id） */
    fun buddhaAt(position: Int): BuddhaConfig? =
        if (items.isEmpty()) null else items[position % items.size]

    /** 某尊佛像在无限列表里的全局坐标；单尊时不放大直接用真实下标，找不到返回 -1 */
    fun globalPositionOf(buddhaId: Int): Int {
        val idx = items.indexOfFirst { it.id == buddhaId }
        if (idx < 0) return -1
        return if (items.size <= 1) idx else idx + items.size * HALF_LOOPS
    }

    class Holder(val binding: ItemBuddhaPageBinding) : RecyclerView.ViewHolder(binding.root) {
        val shrine: ImageView = binding.ivShrine
        val shrineAnim: LottieAnimationView = binding.lottieShrine
        val back: LottieAnimationView = binding.lottieBack
        val iv: ImageView = binding.ivBuddha
        val front: LottieAnimationView = binding.lottieFront
        val offering: ImageView = binding.ivOffering
        val incenseGroup: FrameLayout = binding.incenseGroup
        val incense: ImageView = binding.ivIncense
        val burner: ImageView = binding.ivBurner
        val smokeAnim: LottieAnimationView = binding.lottieSmoke
        val lampLeft: ImageView = binding.ivLampLeft
        val lampRight: ImageView = binding.ivLampRight
        val flameLeft: LottieAnimationView = binding.lottieFlameLeft
        val flameRight: LottieAnimationView = binding.lottieFlameRight
        val touch: View = binding.vSceneTouch

        /** 佛龛/佛灯点击，Fragment 注入（已拥有→我的装扮，未拥有→商城分类） */
        var onShrineClick: (() -> Unit)? = null
        var onLampClick: (() -> Unit)? = null

        // ---- 待落地的缩放参数（layout 前拿不到控件宽高，先存着） ----
        /** 香烛纵向比例：供佛剩余时长/总时长，1=满长 */
        var incenseRatio = 1f
        /** 灯焰比例：灯油剩余比例换算，油越少火越小 */
        var flameScale = 1f

        // ---- 手势状态：区分"点击"与"滑动换佛" ----
        var downX = 0f
        var downY = 0f
        var gestureMoved = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemBuddhaPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val slop = ViewConfiguration.get(parent.context).scaledTouchSlop
        return Holder(binding).also { h ->
            //点击收口在这一层：佛灯压在佛龛上面，分层挂单击会互相吃事件
            h.touch.setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        h.downX = e.x
                        h.downY = e.y
                        h.gestureMoved = false
                    }
                    MotionEvent.ACTION_MOVE -> if (!h.gestureMoved) {
                        //横向滑动是切佛，不能当成点击（纵向留给外层滚不动，忽略）
                        if (abs(e.x - h.downX) > slop || abs(e.y - h.downY) > slop) {
                            h.gestureMoved = true
                        }
                    }
                    MotionEvent.ACTION_UP -> if (!h.gestureMoved) {
                        when (SceneGeometry.hitScene(v.width, v.height, e.x, e.y)) {
                            SceneGeometry.HIT_LAMP -> h.onLampClick?.invoke()
                            SceneGeometry.HIT_SHRINE -> h.onShrineClick?.invoke()
                        }
                    }
                    else -> Unit
                }
                //吃掉事件但不能不给父容器机会：ViewPager2 的 RecyclerView 在
                //onInterceptTouchEvent 里判定滑动后会接管手势，并给我们发 CANCEL
                true
            }
            //控件尺寸第一次就绪时补落一次：bind 时 width/height 还是 0
            h.binding.root.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
                val resized = (r - l != or - ol) || (b - t != ob - ot)
                if (resized) applyGeometry(h)
            }
        }
    }

    override fun getItemCount(): Int = when {
        items.isEmpty() -> 0
        items.size == 1 -> 1
        else -> items.size * LOOPS
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        buddhaAt(position)?.let {
            pageBinder?.invoke(holder, it)
            applyGeometry(holder)
        }
    }

    /**
     * 把 [Holder.incenseRatio] 与 [Holder.flameScale] 落到控件上。
     * pivot 必须换算到画布锚点：
     * - 灯焰 pivot 在灯口(LAMP_MOUTH_Y=1916)，否则油少时火焰会从画布中心往下缩、脱离灯口；
     * - 香杆 pivot 在香底/炉口(INCENSE_BASE_Y=2090)，压缩时香头往下走，烟跟着一起降；
     *   香炉层(iv_burner)不在容器里，不随香缩放。
     * pivotX 保持默认(控件中线)：右侧灯焰靠 scaleX=-1 整画布镜像，镜像轴就在中线。
     */
    fun applyGeometry(h: Holder) {
        val w = h.touch.width
        val vh = h.touch.height
        if (w <= 0 || vh <= 0) return

        h.incenseGroup.pivotX = w / 2f
        h.incenseGroup.pivotY = SceneGeometry.toViewY(w, vh, SceneGeometry.INCENSE_BASE_Y)
        h.incenseGroup.scaleY = h.incenseRatio

        val flamePivotY = SceneGeometry.toViewY(w, vh, SceneGeometry.LAMP_MOUTH_Y)
        //注意：setPivotY 会把 pivot 标记为"显式设置"，pivotX 从此不再随尺寸自动更新，
        //停在初始值 0——flameRight 靠 scaleX=-1 镜像，轴必须显式放到中线，否则整块翻出屏幕
        val midX = w / 2f
        h.flameLeft.pivotX = midX
        h.flameLeft.pivotY = flamePivotY
        h.flameLeft.scaleX = 1f
        h.flameLeft.scaleY = h.flameScale
        h.flameRight.pivotX = midX
        h.flameRight.pivotY = flamePivotY
        h.flameRight.scaleX = -1f
        h.flameRight.scaleY = h.flameScale
    }
}
