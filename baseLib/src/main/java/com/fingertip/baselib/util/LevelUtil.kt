package com.fingertip.baselib.util

import com.fingertip.baselib.bean.UserLevelConfig
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R

/**
 * 修行等级工具（全 App 唯一出口）
 *
 * 数据源：服务端 user_level_config，随 globalParam 一次下发，缓存在
 * GlobalConfig.globalParam.userLevelConfig。所有"显示等级"的地方都走这里，
 * 不要在界面里手拼 "lv$x"，否则头衔/徽章口径会各处不一致。
 *
 * 语义提醒：config.exp 是"达到该等级所需的累计总经验"，不是升级增量。
 * 所以当前等级进度 = (总经验 - 本等级 exp) / (下一等级 exp - 本等级 exp)。
 */
object LevelUtil {

    /**
     * 境界段（大境界）
     *
     * 与 user_level_config 现有数据一致：凡夫单独 0 级，之后每 9 级一段，妙觉 91-100。
     * [color] 是本地兜底配色——服务端 tagUrl 下发了背景框图时优先用图（见 [tagUrlOf]），
     * 没有图时按段上色，保证任何等级都有个稳定的视觉标识。
     * 配色思路：石灰灰 → 主金 → 白金，境界越高越亮。
     */
    data class Stage(val name: String, val startLevel: Int, val endLevel: Int, val color: Int)

    val STAGES: List<Stage> = listOf(
        Stage("凡夫", 0, 0, TopApplication.instance.getColor(R.color.c_stage_fanfu)),
        Stage("善信", 1, 9, TopApplication.instance.getColor(R.color.c_gold)),
        Stage("居士", 10, 18, TopApplication.instance.getColor(R.color.c_stage_jushi)),
        Stage("沙弥", 19, 27, TopApplication.instance.getColor(R.color.c_stage_shami)),
        Stage("比丘", 28, 36, TopApplication.instance.getColor(R.color.c_stage_biqiu)),
        Stage("须陀洹", 37, 45, TopApplication.instance.getColor(R.color.c_stage_xutuohuan)),
        Stage("斯陀含", 46, 54, TopApplication.instance.getColor(R.color.c_stage_situohan)),
        Stage("阿那含", 55, 63, TopApplication.instance.getColor(R.color.c_stage_anahan)),
        Stage("阿罗汉", 64, 72, TopApplication.instance.getColor(R.color.c_stage_aluohan)),
        Stage("菩萨", 73, 81, TopApplication.instance.getColor(R.color.c_stage_pusa)),
        Stage("等觉", 82, 90, TopApplication.instance.getColor(R.color.c_stage_dengjue)),
        Stage("妙觉", 91, Int.MAX_VALUE, TopApplication.instance.getColor(R.color.c_stage_miaojue))
    )

    /**
     * 服务端下发的等级表（按 level 升序）。配置没拉到或为空时返回空表，调用方需自行回退
     *
     * 列表页一次要 bind 上百行、每行都要查头衔和配色，所以这里按 globalParam 的引用做缓存：
     * globalParam 只在启动时整体替换一次，引用变了才重建，其余时候都是直接读缓存。
     */
    private var cachedSource: List<UserLevelConfig>? = null
    private var cachedList: List<UserLevelConfig> = emptyList()
    private var cachedMap: Map<Int, UserLevelConfig> = emptyMap()

    private fun ensureCache() {
        val raw = GlobalConfig.globalParam?.userLevelConfig
        if (raw === cachedSource) return
        cachedSource = raw
        cachedList = raw?.filter { it.level >= 0 }?.sortedBy { it.level }.orEmpty()
        cachedMap = cachedList.associateBy { it.level }
    }

    val configs: List<UserLevelConfig>
        get() {
            ensureCache()
            return cachedList
        }

    val hasConfig: Boolean get() = configs.isNotEmpty()

    /** 最高等级（配置缺失时按 100 兜底，避免进度条算出负数） */
    val maxLevel: Int get() = configs.lastOrNull()?.level ?: 100

    fun configOf(level: Int): UserLevelConfig? {
        ensureCache()
        return cachedMap[level]
    }

    /** 等级头衔，如「三阶·善信」；配置缺失返回空串（界面只显示 lvN） */
    fun tagOf(level: Int): String = configOf(level)?.levelTag.orEmpty()

    /** 大境界名，如「善信」。优先取头衔里「·」后半段，取不到再回退到本地分段表 */
    fun stageNameOf(level: Int): String {
        val tag = tagOf(level)
        if (tag.contains("·")) return tag.substringAfter("·")
        if (tag.isNotBlank()) return tag
        return stageOf(level)?.name.orEmpty()
    }

    fun stageOf(level: Int): Stage? = STAGES.firstOrNull { level in it.startLevel..it.endLevel }

    /** 等级徽章的主色（描边/文字用），服务端没配图时也用它做底色 */
    fun colorOf(level: Int): Int = stageOf(level)?.color ?: TopApplication.instance.getColor(R.color.c_gold)

    /** 徽章底的填充色：主色 18% 透明度，深底上不抢文字 */
    fun badgeFillOf(level: Int): Int = (colorOf(level) and 0x00FFFFFF) or 0x2E000000

    /** 等级徽章上的文字，如「lv3」 */
    fun badgeTextOf(level: Int): String = "lv$level"

    /**
     * 昵称后面跟的那串等级文字，如「lv3 三阶·善信」
     * 配置没下发时退化为「lv3」，不会出现多余的空白或分隔符
     */
    fun nameOf(level: Int): String {
        val tag = tagOf(level)
        return if (tag.isBlank()) "lv$level" else "lv$level $tag"
    }

    /** 等级背景框图片地址；为空表示用本地分段配色 */
    fun tagUrlOf(level: Int): String = configOf(level)?.tagUrl.orEmpty()

    /** 按累计总经验反查等级（与服务端 GlobalParamMapper.getUserLevelConfigByExp 同语义） */
    fun levelOfExp(exp: Int): Int =
        configs.lastOrNull { it.exp <= exp }?.level ?: 0

    /** 当前等级的经验起点（该等级的累计门槛） */
    fun expStartOf(level: Int): Int = configOf(level)?.exp ?: 0

    /** 升到下一级所需的累计总经验；已满级返回 -1 */
    fun expEndOf(level: Int): Int = configOf(level + 1)?.exp ?: -1

    /**
     * 当前等级内的进度 0f..1f
     * 已满级固定 1f；配置缺失时 0f（不显示进度更诚实，但这里给 0 让页面自行判断）
     */
    fun progressOf(exp: Int): Float {
        val level = levelOfExp(exp)
        if (level >= maxLevel) return 1f
        val start = expStartOf(level)
        val end = expEndOf(level)
        if (end <= start) return 1f
        return ((exp - start).toFloat() / (end - start)).coerceIn(0f, 1f)
    }

    /** 距下一级还差多少经验；已满级返回 0 */
    fun expToNextOf(exp: Int): Int {
        val level = levelOfExp(exp)
        if (level >= maxLevel) return 0
        val end = expEndOf(level)
        if (end < 0) return 0
        return (end - exp).coerceAtLeast(0)
    }

    /** 等级表按境界分组，详情页"等级体系"区块用 */
    fun stagesWithLevels(): List<Pair<Stage, List<UserLevelConfig>>> =
        STAGES.map { stage ->
            stage to configs.filter { it.level in stage.startLevel..stage.endLevel }
        }.filter { it.second.isNotEmpty() }
}
