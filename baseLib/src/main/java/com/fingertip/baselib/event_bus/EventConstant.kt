package com.fingertip.baselib.event_bus

object EventConstant {

    //注册软键盘监听
    const val EVENT_NOTIFY_REGISTER_KEYBOARD = 1
    //注销软键盘监听
    const val EVENT_NOTIFY_UNREGISTER_KEYBOARD = 2

    //动态详情本地操作（点赞、评论），更新列表
    const val EVENT_NOTIFY_MOMENT_LIST = 3

    //拉黑某人成功，各动态列表按 accountId 就地移除其内容（object 为被拉黑的 accountId）
    const val EVENT_BLOCK_ACCOUNT = 4

}