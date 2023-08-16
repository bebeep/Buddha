package com.fingertip.baselib.adapter

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class FragmentPagerTitleAdapter : FragmentPagerAdapter {
    private var homeFragments: List<Fragment>
    private var titles: List<String>? = null

    constructor(fm: FragmentManager?, fragments: List<Fragment>) : super(
        fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        homeFragments = fragments
    }

    constructor(fm: FragmentManager?, fragments: List<Fragment>, titles: List<String>?) : super(
        fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        homeFragments = fragments
        this.titles = titles
    }

    override fun getItem(arg0: Int): Fragment {
        return homeFragments[arg0]
    }

    override fun getCount(): Int {
        return homeFragments.size
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
    }

    override fun getItemPosition(`object`: Any): Int {
        return super.getItemPosition(`object`)
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return if (titles == null || titles!!.isEmpty()) super.getPageTitle(position) else titles!![position]
    }
}