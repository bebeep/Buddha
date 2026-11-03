package com.fingertip.baselib.bean

/**
 * 收藏记录，对应服务端 user_collect_record（userTool/getCollectList）。
 *
 * 三类目标共用一个收藏表，targetType 区分：1佛经 2感悟 3寺庙。
 * targetTitle / targetCover 是服务端按类型分别 join 出来的展示字段，
 * 客户端拿到就能直接渲染，不必为三种类型各写一套反查逻辑。
 */
class CollectData {

    companion object {
        const val TYPE_ALL = 0
        const val TYPE_SUTRA = 1
        const val TYPE_MOMENT = 2
        const val TYPE_TEMPLE = 3
    }

    var id: Int = 0
    var accountId: Int = 0
    var targetType: Int = 0
    var targetId: Int = 0
    var createDate: String? = null

    //展示字段：佛经取经名，感悟取正文摘要，寺庙取寺名
    var targetTitle: String? = null
    //封面：感悟取图片/视频封面，寺庙取首图；佛经为空
    var targetCover: String? = null
}
