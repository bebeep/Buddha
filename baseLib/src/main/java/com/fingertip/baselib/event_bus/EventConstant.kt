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

    //IM 未读总数变化，MainFragment 更新消息 tab 角标（object 为未读数 Long）
    const val EVENT_IM_UNREAD = 5

    //IM 新消息到达，会话列表增量刷新 + 前台本地通知（object 为 V2TIMMessage）
    const val EVENT_IM_NEW_MESSAGE = 6

}