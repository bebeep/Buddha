package com.fingertip.baselib.bean

/**
 * 统一搜索结果：佛友 + 动态。
 * 动态直接复用 MomentEntity，搜索页可以直接用 MomentAdapter 渲染。
 */
class SearchResultEntity {
    var users: List<SearchUserEntity>? = null
    var moments: List<MomentEntity>? = null
}
