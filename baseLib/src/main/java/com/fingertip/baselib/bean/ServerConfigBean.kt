package com.fingertip.baselib.bean

class ServerConfigBean {


    var openAdvertising: OpenAdvertising? = null
    var openAdvertisings: OpenAdvertising? = null//随机显示开屏广告
    var tabBarConfig: TabBarConfig? = null
    inner class OpenAdvertising {
        /**
         * addressUrl : https://hiyaa.s3.amazonaws.com/Hiyaa_terms.html
         * enable : 1
         * imageUrl : https://hiyaa.s3.amazonaws.com/openAdvertising.png
         * desc : 开屏广告，开启之后，自动登录用户展示/跳转用，1开启
         */
        var addressUrl: String? = null
        var enable = 0
        var imageUrl: String? = null
        var desc: String? = null
        var activities = ArrayList<OpenAdvertising>()
        override fun toString(): String {
            return "OpenAdvertising{" +
                    "addressUrl='" + addressUrl + '\'' +
                    ", enable=" + enable +
                    ", imageUrl='" + imageUrl + '\'' +
                    ", desc='" + desc + '\'' +
                    '}'
        }
    }

    inner class TabBarConfig {
        var normal: MutableList<String>? = null
        var selected: MutableList<String>? = null
        var enable = 0  //0禁用 1启用
    }

    override fun toString(): String {
        return "ServerConfigBean(openAdvertising=$openAdvertising)"
    }

}