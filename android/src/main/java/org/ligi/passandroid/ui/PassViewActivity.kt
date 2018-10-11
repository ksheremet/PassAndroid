package org.ligi.passandroid.ui

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_pass_view_base.*
import org.ligi.kaxt.disableRotation
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.model.pass.Pass

class PassViewActivity : PassViewActivityBase() {
    private lateinit var pagerAdapter: CollectionPagerAdapter
    private lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        disableRotation()
        setContentView(R.layout.activity_pass_view)

        pagerAdapter = CollectionPagerAdapter(supportFragmentManager, PassStoreProjection(passStore,
                passStore.classifier.getTopic(currentPass, ""),
                settings.getSortOrder()))
        viewPager = pager
        viewPager.adapter = pagerAdapter
        viewPager.currentItem = pagerAdapter.getPos(currentPass)
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(i: Int) {
                currentPass = pagerAdapter.getPass(i)
                passStore.currentPass = currentPass
            }
        })
    }

    override fun refresh() {
        super.refresh()

        pagerAdapter.refresh()
    }

    inner class CollectionPagerAdapter(fm: FragmentManager, private var passStoreProjection: PassStoreProjection) : FragmentStatePagerAdapter(fm) {

        override fun getCount(): Int = passStoreProjection.passList.count()

        override fun getItem(i: Int): Fragment {

            val fragment = PassViewFragment()
            fragment.arguments = Bundle().apply {
                putString(PassViewActivityBase.EXTRA_KEY_UUID, getPass(i).id)
            }
            return fragment
        }

        fun getPass(i: Int): Pass {
            return passStoreProjection.passList[i]
        }

        fun getPos(pass: Pass): Int {
            return passStoreProjection.passList.indexOf(pass)
        }

        fun refresh() {
            passStoreProjection.refresh()
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()

        setSupportActionBar(toolbar)

        configureActionBar()
        refresh()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_map).isVisible = !currentPass.locations.isEmpty()
        menu.findItem(R.id.menu_update).isVisible = PassViewActivityBase.mightPassBeAbleToUpdate(currentPass)
        menu.findItem(R.id.install_shortcut).isVisible = (23..25).contains(Build.VERSION.SDK_INT)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.map_item, menu)
        menuInflater.inflate(R.menu.update, menu)
        menuInflater.inflate(R.menu.shortcut, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            val upIntent = NavUtils.getParentActivityIntent(this)
            if (NavUtils.shouldUpRecreateTask(this, upIntent!!)) {
                TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities()
                finish()
            } else {
                NavUtils.navigateUpTo(this, upIntent)
            }
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onAttachedToWindow() = window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)

}