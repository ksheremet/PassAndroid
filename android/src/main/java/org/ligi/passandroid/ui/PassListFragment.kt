package org.ligi.passandroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.pass_recycler.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.ligi.passandroid.App
import org.ligi.passandroid.R
import org.ligi.passandroid.events.PassStoreChangeEvent
import org.ligi.passandroid.events.ScanFinishedEvent
import org.ligi.passandroid.functions.moveWithUndoSnackbar
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.model.Settings

class PassListFragment : Fragment() {


    private lateinit var passStoreProjection: PassStoreProjection
    private lateinit var adapter: PassAdapter

    val passStore: PassStore = App.kodein.instance()
    val settings: Settings = App.kodein.instance()
    val bus: EventBus = App.kodein.instance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val inflate = inflater.inflate(R.layout.pass_recycler, container, false)

        passStoreProjection = PassStoreProjection(passStore, arguments!!.getString(BUNDLE_KEY_TOPIC)!!, settings.getSortOrder())
        adapter = PassAdapter(activity as AppCompatActivity, passStoreProjection)

        inflate.pass_recyclerview.adapter = adapter

        inflate.pass_recyclerview.layoutManager = LinearLayoutManager(activity)

        val simpleItemTouchCallback = object : SimpleCallback(0, LEFT or RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder)
                    = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                this@PassListFragment.onSwiped(viewHolder.adapterPosition, swipeDir)
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(inflate.pass_recyclerview)

        bus.register(this)
        return inflate
    }

    @VisibleForTesting
    fun onSwiped(pos: Int, swipeDir: Int) {
        val pass = passStoreProjection.passList[pos]
        val nextTopic = passStore.classifier.getTopicWithOffset(pass, if (swipeDir == LEFT) -1 else 1)

        if (nextTopic != null) {
            moveWithUndoSnackbar(passStore.classifier, pass, nextTopic, activity as AppCompatActivity)
        } else {
            MoveToNewTopicUI(activity as AppCompatActivity, passStore, pass).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bus.unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPassStoreChangeEvent(passStoreChangeEvent: PassStoreChangeEvent) {
        passStoreProjection.refresh()
        adapter.notifyDataSetChanged()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onScanFinishedEvent(scanFinishedEvent: ScanFinishedEvent) {
        passStoreProjection.refresh()
        adapter.notifyDataSetChanged()

    }

    companion object {

        private val BUNDLE_KEY_TOPIC = "topic"

        fun newInstance(topic: String) = PassListFragment().apply {
            arguments = Bundle()
            arguments!!.putString(BUNDLE_KEY_TOPIC, topic)
        }
    }

}
