package org.ligi.passandroid.ui

import android.content.Intent
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.util.LinkifyCompat
import androidx.fragment.app.Fragment
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_pass_view.*
import kotlinx.android.synthetic.main.barcode.*
import kotlinx.android.synthetic.main.pass_list_item.*
import kotlinx.android.synthetic.main.pass_view_extra_data.*
import org.ligi.compat.HtmlCompat
import org.ligi.kaxt.startActivityFromClass
import org.ligi.passandroid.App
import org.ligi.passandroid.R
import org.ligi.passandroid.maps.PassbookMapsFacade
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.ui.pass_view_holder.VerbosePassViewHolder

class PassViewFragment : Fragment() {

    private val passViewHelper by lazy { PassViewHelper(activity) }
    private var passStore : PassStore = App.kodein.instance()
    lateinit var currentPass : Pass

    private fun processImage(view: ImageView, name: String, pass: Pass) {
        val bitmap = pass.getBitmap(passStore, name)
        if (bitmap != null && bitmap.width > 300) {
            view.setOnClickListener {
                val intent = Intent(view.context, TouchImageActivity::class.java)
                intent.putExtra("IMAGE", name)
                startActivity(intent)
            }
        }
        passViewHelper.setBitmapSafe(view, bitmap)
    }

    override fun onResume() {
        super.onResume()

        moreTextView.setOnClickListener {
            if (back_fields.visibility == View.VISIBLE) {
                back_fields.visibility = View.GONE
                moreTextView.setText(R.string.more)
            } else {
                back_fields.visibility = View.VISIBLE
                moreTextView.setText(R.string.less)
            }
        }

        barcode_img.setOnClickListener {
            activity?.startActivityFromClass(FullscreenBarcodeActivity::class.java)
        }

        BarcodeUIController(view!!, currentPass.barCode, activity!!, passViewHelper)

        processImage(logo_img_view, PassBitmapDefinitions.BITMAP_LOGO, currentPass)
        processImage(footer_img_view, PassBitmapDefinitions.BITMAP_FOOTER, currentPass)
        processImage(thumbnail_img_view, PassBitmapDefinitions.BITMAP_THUMBNAIL, currentPass)
        processImage(strip_img_view, PassBitmapDefinitions.BITMAP_STRIP, currentPass)

        if (map_container != null) {
            if (!(currentPass.locations.isNotEmpty() && PassbookMapsFacade.init(activity!!))) {
                @Suppress("PLUGIN_WARNING")
                map_container.visibility = View.GONE
            }
        }

        val backStrBuilder = StringBuilder()

        front_field_container.removeAllViews()

        for (field in currentPass.fields) {
            if (field.hide) {
                backStrBuilder.append(field.toHtmlSnippet())
            } else {
                val v = activity!!.layoutInflater.inflate(R.layout.main_field_item, front_field_container, false)
                val key = v.findViewById(R.id.key) as TextView
                key.text = field.label
                val value = v.findViewById(R.id.value) as TextView
                value.text = field.value

                front_field_container.addView(v)
                LinkifyCompat.addLinks(key, Linkify.ALL)
                LinkifyCompat.addLinks(value, Linkify.ALL)
            }
        }

        if (backStrBuilder.isNotEmpty()) {
            back_fields.text = HtmlCompat.fromHtml(backStrBuilder.toString())
            moreTextView.visibility = View.VISIBLE
        } else {
            moreTextView.visibility = View.GONE
        }

        LinkifyCompat.addLinks(back_fields, Linkify.ALL)

        val passViewHolder = VerbosePassViewHolder(pass_card)
        passViewHolder.apply(currentPass, passStore, activity!!)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        val rootView = inflater.inflate(R.layout.activity_pass_view_page, container, false)
        arguments?.takeIf { it.containsKey(PassViewActivityBase.EXTRA_KEY_UUID) }?.apply {
            val uuid = getString(PassViewActivityBase.EXTRA_KEY_UUID)

            if (uuid != null) {
                val passbookForId = passStore.getPassbookForId(uuid)
                passStore.currentPass = passbookForId
            }

            currentPass = passStore.currentPass!!
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val passExtrasView = activity?.layoutInflater?.inflate(R.layout.pass_view_extra_data, passExtrasContainer, false)
        passExtrasContainer.addView(passExtrasView)
    }
}