/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package au.smap.fieldTask.preferences

import android.os.Bundle
import au.smap.fieldTask.activities.SmapMain
import org.odk.collect.android.R
import org.odk.collect.android.activities.ActivityUtils
import org.odk.collect.android.formmanagement.FormsDataService
import org.odk.collect.android.fragments.dialogs.MovingBackwardsDialog.MovingBackwardsDialogListener
import org.odk.collect.android.fragments.dialogs.ResetSettingsResultDialog.ResetSettingsResultDialogListener
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.instancemanagement.InstancesDataService
import org.odk.collect.android.preferences.dialogs.DeleteProjectDialog
import org.odk.collect.android.preferences.screens.AccessControlPreferencesFragment
import org.odk.collect.android.preferences.screens.FormEntryAccessPreferencesFragment
import org.odk.collect.android.projects.ProjectDeleter
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.androidshared.ui.FragmentFactoryBuilder
import org.odk.collect.async.Scheduler
import org.odk.collect.metadata.PropertyManager
import org.odk.collect.strings.localization.LocalizedActivity
import javax.inject.Inject

/**
 * Smap version of admin preferences activity that redirects to SmapMain instead of MainMenuActivity
 * after dialogs are closed.
 */
class AdminPreferencesActivitySmap :
    LocalizedActivity(),
    ResetSettingsResultDialogListener,
    MovingBackwardsDialogListener {

    private var isInstanceStateSaved = false

    @Inject
    lateinit var propertyManager: PropertyManager

    @Inject
    lateinit var projectDeleter: ProjectDeleter

    @Inject
    lateinit var projectsDataService: ProjectsDataService

    @Inject
    lateinit var formsDataService: FormsDataService

    @Inject
    lateinit var instancesDataService: InstancesDataService

    @Inject
    lateinit var scheduler: Scheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        DaggerUtils.getComponent(this).inject(this)
        supportFragmentManager.fragmentFactory = FragmentFactoryBuilder()
            .forClass(AccessControlPreferencesFragment::class.java) {
                AccessControlPreferencesFragment()
            }
            .forClass(DeleteProjectDialog::class) {
                DeleteProjectDialog(
                    projectDeleter,
                    projectsDataService,
                    formsDataService,
                    instancesDataService,
                    scheduler
                )
            }
            .build()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences_layout)
        setTitle(org.odk.collect.strings.R.string.admin_preferences)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.preferences_fragment_container, AccessControlPreferencesFragment(), TAG)
                .commit()
        }
    }

    override fun onPause() {
        super.onPause()
        propertyManager.reload()
    }

    override fun onPostResume() {
        super.onPostResume()
        isInstanceStateSaved = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        isInstanceStateSaved = true
        super.onSaveInstanceState(outState)
    }

    override fun onDialogClosed() {
        // Smap customization: redirect to SmapMain instead of MainMenuActivity
        ActivityUtils.startActivityAndCloseAllOthers(this, SmapMain::class.java)
    }

    override fun preventOtherWaysOfEditingForm() {
        val fragment = supportFragmentManager.findFragmentById(R.id.preferences_fragment_container) as FormEntryAccessPreferencesFragment
        fragment.preventOtherWaysOfEditingForm()
    }

    fun isInstanceStateSaved() = isInstanceStateSaved

    companion object {
        const val TAG = "AdminPreferencesFragment"
        const val ADMIN_PREFERENCES = "admin_prefs"
    }
}
