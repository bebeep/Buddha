package com.fingertip.baselib.bean

class LoginRspData: TopData() {
    var roleType: Int = 0
    var user: PersonData? = null
    var session: String? = null
    override fun toString(): String {
        return "LoginRspData(user=$user)"
    }
}