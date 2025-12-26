package au.smap.fieldTask.activities;

/*
 * Copyright (C) 2019 Smap Consulting Pty Ltd
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

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.projects.ProjectsDataService;
import org.odk.collect.settings.keys.ProjectKeys;
import au.smap.fieldTask.preferences.GeneralSharedPreferencesSmap;
import org.odk.collect.projects.ProjectsRepository;
import org.odk.collect.settings.SettingsProvider;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Splash/Launcher activity that checks if login is required before launching the main activity.
 * This follows the fieldTask4 pattern where login checking happens at app entry point
 * rather than within SmapMain.
 */
public class SplashScreenActivity extends AppCompatActivity {

    @Inject
    ProjectsRepository projectsRepository;

    @Inject
    ProjectsDataService projectsDataService;

    @Inject
    SettingsProvider settingsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inject dependencies
        DaggerUtils.getComponent(this).inject(this);

        // Ensure a default project exists before checking login
        ensureDefaultProject();

        // Check if login is required
        if (checkLoginRequired()) {
            Timber.i("Login required - launching SmapLoginActivity");
            startActivity(new Intent(this, SmapLoginActivity.class));
            finish();
        } else {
            Timber.i("Login not required - launching SmapMain");
            startActivity(new Intent(this, SmapMain.class));
            finish();
        }
    }

    /**
     * Ensure a default project exists (create if needed).
     * This mirrors the logic from SmapMain.ensureDefaultProject()
     */
    private void ensureDefaultProject() {
        // Check if any projects exist
        if (projectsRepository.getAll().isEmpty()) {
            // Create a default project
            org.odk.collect.projects.Project.New defaultProject = new org.odk.collect.projects.Project.New(
                    "Default",  // name
                    "D",        // icon
                    "#3e9fcc"   // color (ODK blue)
            );
            org.odk.collect.projects.Project.Saved savedProject = projectsRepository.save(defaultProject);
            projectsDataService.setCurrentProject(savedProject.getUuid());
            Timber.i("Created default project");
        } else if (projectsDataService.getCurrentProject() == null) {
            // Projects exist but no current project is set - set the first one as current
            org.odk.collect.projects.Project.Saved firstProject = projectsRepository.getAll().get(0);
            projectsDataService.setCurrentProject(firstProject.getUuid());
            Timber.i("Set current project to first project");
        }
    }

    /**
     * Check if login is required based on password policy and credentials.
     * This mirrors the logic from SmapMain.checkLoginRequired()
     *
     * @return true if login screen should be shown
     */
    private boolean checkLoginRequired() {
        try {
            GeneralSharedPreferencesSmap prefs = GeneralSharedPreferencesSmap.getInstance();

            String url = (String) prefs.get(ProjectKeys.KEY_SERVER_URL);
            String user = (String) prefs.get(ProjectKeys.KEY_USERNAME);
            String password = (String) prefs.get(ProjectKeys.KEY_PASSWORD);

            String pwPolicyStr = (String) prefs.get(ProjectKeys.KEY_SMAP_PASSWORD_POLICY);
            String lastLoginStr = (String) prefs.get(ProjectKeys.KEY_SMAP_LAST_LOGIN);

            // Default values if not set
            int pwPolicy = -1;
            long lastLogin = 0;

            if (pwPolicyStr != null && !pwPolicyStr.trim().isEmpty()) {
                pwPolicy = Integer.parseInt(pwPolicyStr);
            }

            if (lastLoginStr != null && !lastLoginStr.trim().isEmpty()) {
                lastLogin = Long.parseLong(lastLoginStr);
            }

            Timber.i("Login check - pwPolicy: %d, lastLogin: %d, url: %s, user: %s, password: %s",
                    pwPolicy, lastLogin, url, user, password != null ? "***" : "null");

            // Show the login screen if required by password policy
            // 0 - always login, > 0 is number of days before login is required
            // Alternatively show the login screen if any of the login details are empty
            boolean loginRequired = pwPolicy == 0 ||
                   (pwPolicy > 0 && (System.currentTimeMillis() - lastLogin) > pwPolicy * 24 * 3600 * 1000) ||
                   password == null || user == null || url == null ||
                   password.trim().isEmpty() || user.trim().isEmpty() || url.trim().isEmpty();

            Timber.i("Login required: %b", loginRequired);
            return loginRequired;
        } catch (Exception e) {
            Timber.e(e, "Error checking login required");
            // If there's an error, require login to be safe
            return true;
        }
    }
}
