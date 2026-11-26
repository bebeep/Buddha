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

    //IM 登录成功（冷启动同步完成）：会话列表首次填充靠它触发，避免切 tab 早于登录时空屏
    const val EVENT_IM_LOGIN_SUCCESS = 7

    //取消收藏成功：我的收藏各 tab 同步摘掉该条（object 为 CollectData）
    const val EVENT_COLLECT_CANCEL = 8

    //关注/取关成功：个人中心重拉用户信息，刷新粉丝/关注数（粘性，回退时不会漏）
    const val EVENT_FOLLOW_CHANGED = 9

}