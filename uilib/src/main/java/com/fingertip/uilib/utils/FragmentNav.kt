package com.fingertip.uilib.utils

import androidx.fragment.app.Fragment
import com.fingertip.uilib.fragment.MainFragment
import com.weikaiyun.fragmentation.SupportFragment

/**
 * 页面跳转工具
 *
 * 全站页面都压在 MainFragment 之下（全屏盖住底部 tab），但编写入口时直接
 * `parentFragment as? MainFragment` 有一个隐患：只要当前页面不是直接挂在 MainFragment 下
 * （例如「我的装扮」是从商城再打开的、商城又是从拜佛页打开的），强转就是 null，
 * 点了完全没反应，也看不到任何报错。
 *
 * 这里统一成「向上逐级找 MainFragment，找不到就退化为挂在自己身上」，
 * 保证任何入口点下去都有反应。
 */

/** 向上逐级找 MainFragment；找不到再从 Activity 的 fragment 树里兜底找一次 */
fun Fragment.findMainFragment(): MainFragment? {
    var p: Fragment? = parentFragment
    while (p != null) {
        if (p is MainFragment) return p
        p = p.parentFragment
    }
    return activity?.supportFragmentManager?.fragments?.firstOrNull { it is MainFragment } as? MainFragment
}

/**
 * 打开新页面。
 *
 * 优先压到 MainFragment 下（全屏、盖住底部 tab）；
 * 拿不到 MainFragment 时挂到当前页面自身，宁可层级深一层，也不能点了没反应。
 */
fun SupportFragment.openPage(page: SupportFragment) {
    val main = findMainFragment()
    if (main != null && main !== this) {
        main.start(page)
    } else {
        start(page)
    }
}
