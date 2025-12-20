/*
 * Copyright 2018 Shobhit Agarwal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.odk.collect.android.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import org.odk.collect.android.R
import org.odk.collect.android.adapters.AboutItemClickListener
import org.odk.collect.android.adapters.AboutListAdapter
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.androidshared.system.IntentLauncher
import org.odk.collect.androidshared.ui.multiclicksafe.MultiClickGuard.allowClick
import org.odk.collect.strings.localization.LocalizedActivity
import org.odk.collect.webpage.WebPageService
import javax.inject.Inject

class AboutActivity : LocalizedActivity(), AboutItemClickListener {
    private lateinit var websiteUri: Uri

    @Inject
    lateinit var intentLauncher: IntentLauncher

    @Inject
    lateinit var webPageService: WebPageService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_layout)
        DaggerUtils.getComponent(this).inject(this)
        initToolbar()

        // smap: use smap website URL
        websiteUri = Uri.parse(getString(org.odk.collect.strings.R.string.app_url))

        // smap: create items array with Android version
        val itemsWithVersion = arrayOf(
            *ITEMS,
            intArrayOf(
                R.drawable.ic_phone,
                -2, // special marker for Android version (not a string resource)
                -1
            )
        )

        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@AboutActivity)
            adapter = AboutListAdapter(itemsWithVersion, this@AboutActivity,
                getString(R.string.smap_android_version, Build.VERSION.RELEASE))
            itemAnimator = DefaultItemAnimator()
        }
    }

    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(org.odk.collect.androidshared.R.id.toolbar)
        // smap: include version in title
        title = getString(org.odk.collect.strings.R.string.about_preferences) +
                " " +
                getString(org.odk.collect.strings.R.string.version) +
                " " +
                getString(org.odk.collect.strings.R.string.app_version)
        setSupportActionBar(toolbar)
    }

    override fun onClick(position: Int) {
        if (allowClick(javaClass.name)) {
            when (position) {
                0 -> webPageService.openWebPage(this, websiteUri) // smap: visit website
                1 -> addReview() // smap: leave a review
            }
        }
    }

    private fun addReview() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        )
        intentLauncher.launch(this, intent) {
            // Show a list of all available browsers if user doesn't have a default browser
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(GOOGLE_PLAY_URL + packageName)
                )
            )
        }
    }

    companion object {
        private const val GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id="
        // smap: only show visit website and leave a review
        private val ITEMS = arrayOf(
            intArrayOf(
                R.drawable.ic_outline_website_24,
                R.string.smap_visit_website,
                -1 // no summary
            ),
            intArrayOf(
                R.drawable.ic_outline_rate_review_24,
                org.odk.collect.strings.R.string.leave_a_review,
                -1 // no summary
            )
        )
    }
}
