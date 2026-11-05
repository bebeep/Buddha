package com.fingertip.uilib.utils

import java.util.Calendar

/**
 * 首页「佛友圈精选」的兜底内容：佛经里的传世短句，按天轮换。
 *
 * 为什么要这个兜底：冷启动阶段社区可能一条动态都没有，精选区块开天窗
 * 比"显示一句禅语"难看得多。服务端 featuredMoments 为空时用这里的句子补位，
 * 保证首页 never 空态，且不增加任何请求。
 *
 * 内容全部出自通行本原文，出处一并给出——不编造，也不做二次改写。
 */
object DailyQuotes {

    class Quote(val text: String, val source: String)

    private val QUOTES = listOf(
        Quote("一切有为法，如梦幻泡影，如露亦如电，应作如是观。", "《金刚经》"),
        Quote("凡所有相，皆是虚妄。若见诸相非相，即见如来。", "《金刚经》"),
        Quote("应无所住，而生其心。", "《金刚经》"),
        Quote("过去心不可得，现在心不可得，未来心不可得。", "《金刚经》"),
        Quote("若菩萨不住相布施，其福德不可思量。", "《金刚经》"),
        Quote("色不异空，空不异色；色即是空，空即是色。", "《心经》"),
        Quote("心无罣碍，无罣碍故，无有恐怖。", "《心经》"),
        Quote("照见五蕴皆空，度一切苦厄。", "《心经》"),
        Quote("诸恶莫作，众善奉行，自净其意，是诸佛教。", "《法句经》"),
        Quote("莫轻小恶，以为无罪；水滴虽微，渐盈大器。", "《法句经》"),
        Quote("无爱即无忧，不忧即无畏。", "《四十二章经》"),
        Quote("人命在呼吸间。", "《四十二章经》"),
        Quote("慎勿信汝意，汝意不可信。", "《四十二章经》"),
        Quote("四大苦空，五阴无我。", "《八大人觉经》"),
        Quote("少欲无为，身心自在。", "《八大人觉经》"),
        Quote("多欲为苦，生死疲劳，从贪欲起。", "《八大人觉经》"),
        Quote("心是恶源，形为罪薮。", "《八大人觉经》"),
        Quote("菩提本无树，明镜亦非台；本来无一物，何处惹尘埃。", "《六祖坛经》"),
        Quote("不是风动，不是幡动，仁者心动。", "《六祖坛经》"),
        Quote("佛法在世间，不离世间觉。", "《六祖坛经》"),
        Quote("何期自性，本自清净。", "《六祖坛经》"),
        Quote("若真修道人，不见世间过。", "《六祖坛经》"),
        Quote("一切众生，皆具如来智慧德相。", "《华严经》"),
        Quote("若人欲了知，三世一切佛，应观法界性，一切唯心造。", "《华严经》"),
        Quote("假使百千劫，所作业不亡；因缘会遇时，果报还自受。", "《大宝积经》"),
        Quote("此有故彼有，此生故彼生；此无故彼无，此灭故彼灭。", "《杂阿含经》")
    )

    /**
     * 取当天的一条（按一年中的第几天取模，同一天多次进入首页结果稳定，
     * 不会每次刷新都换句子）
     */
    fun today(): Quote {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return QUOTES[dayOfYear % QUOTES.size]
    }

    /**
     * 补位用：从今天这条开始往后取 count 条，避免同屏出现重复句子
     */
    fun wrap(count: Int): List<Quote> {
        if (count <= 0) return emptyList()
        val start = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % QUOTES.size
        return (0 until count).map { QUOTES[(start + it) % QUOTES.size] }
    }
}
