@file:Suppress("DEPRECATION")

package io.legado.app.ui.main.readrecord

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.tabs.TabLayout
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.databinding.FragmentReadRecordTabsBinding
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ReadRecordFragment() : BaseFragment(R.layout.fragment_read_record_tabs),
    MainFragmentInterface {

    constructor(position: Int) : this() {
        arguments = Bundle().apply {
            putInt("position", position)
        }
    }

    override val position: Int? get() = arguments?.getInt("position")
    private val binding by viewBinding(FragmentReadRecordTabsBinding::bind)
    private val adapter by lazy { TabFragmentPageAdapter(childFragmentManager) }
    private val tabLayout: TabLayout by lazy {
        binding.titleBar.findViewById(R.id.tab_layout)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val background = requireContext().bottomBackground
        binding.root.setBackgroundColor(background)
        binding.titleBar.setBackgroundColor(background)
        setSupportToolbar(binding.titleBar.toolbar)
        initView()
    }

    private fun initView() {
        binding.viewPagerReadRecord.setEdgeEffectColor(primaryColor)
        tabLayout.isTabIndicatorFullWidth = false
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.setSelectedTabIndicatorColor(requireContext().accentColor)
        tabLayout.setupWithViewPager(binding.viewPagerReadRecord)
        binding.viewPagerReadRecord.offscreenPageLimit = 1
        binding.viewPagerReadRecord.adapter = adapter
    }

    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> getString(R.string.read_record)
                else -> getString(R.string.rating_overview)
            }
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> ReadRecordOverviewFragment()
                else -> RatingOverviewFragment()
            }
        }

        override fun getCount(): Int = 2

    }

}
