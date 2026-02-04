/*
 * Copyright 2017 Shobhit
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

package org.odk.collect.android.preferences.screens;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import org.jetbrains.annotations.NotNull;
import org.odk.collect.android.R;
import org.odk.collect.android.backgroundwork.FormUpdateScheduler;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.preferences.ScanButtonPreference;
import org.odk.collect.android.preferences.ServerPreferencesAdder;
import org.odk.collect.android.preferences.filters.ControlCharacterFilter;
import org.odk.collect.androidshared.ui.ToastUtils;
import org.odk.collect.androidshared.utils.Validator;
import org.odk.collect.settings.keys.ProjectKeys;
import org.odk.collect.shared.settings.Settings;

import au.smap.fieldTask.activities.SmapLoginQRActivity;

import javax.inject.Inject;

public class ServerPreferencesFragment extends BaseProjectPreferencesFragment {
    private EditTextPreference passwordPreference;
    private EditTextPreference serverUrlPreference;
    private EditTextPreference usernamePreference;
    private SwitchPreferenceCompat useTokenPreference;
    private ScanButtonPreference scanButton;
    private EditTextPreference authTokenPreference;

    private final ActivityResultLauncher<Intent> qrScanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (data != null) {
                    String url = data.getStringExtra("server_url");
                    if (url != null && serverUrlPreference != null) {
                        serverUrlPreference.setText(url);
                        serverUrlPreference.setSummary(url);
                    }
                    String user = data.getStringExtra("username");
                    if (user != null && usernamePreference != null) {
                        usernamePreference.setText(user);
                        usernamePreference.setSummary(user);
                    }
                    String token = data.getStringExtra("auth_token");
                    if (token != null && authTokenPreference != null) {
                        authTokenPreference.setText(token);
                        authTokenPreference.setSummary(token);
                    }
                }
            }
    );

    @Inject
    FormUpdateScheduler formUpdateScheduler;

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        DaggerUtils.getComponent(context).inject(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.server_preferences, rootKey);
        addServerPreferences();
    }

    public void addServerPreferences() {
        if (!new ServerPreferencesAdder(this).add()) {
            return;
        }
        serverUrlPreference = findPreference(ProjectKeys.KEY_SERVER_URL);
        usernamePreference = findPreference(ProjectKeys.KEY_USERNAME);
        passwordPreference = findPreference(ProjectKeys.KEY_PASSWORD);

        serverUrlPreference.setOnPreferenceChangeListener(createChangeListener());
        serverUrlPreference.setSummary(serverUrlPreference.getText());

        usernamePreference.setOnPreferenceChangeListener(createChangeListener());
        usernamePreference.setSummary(usernamePreference.getText());
        usernamePreference.setDialogTitle(getString(org.odk.collect.strings.R.string.change_username, getString(org.odk.collect.strings.R.string.app_name))); // smap
        passwordPreference.setDialogTitle(getString(org.odk.collect.strings.R.string.change_password, getString(org.odk.collect.strings.R.string.app_name))); // smap

        usernamePreference.setOnBindEditTextListener(editText -> {
            editText.setFilters(new InputFilter[]{new ControlCharacterFilter()});
        });

        passwordPreference.setOnPreferenceChangeListener(createChangeListener());
        maskPasswordSummary(passwordPreference.getText());

        passwordPreference.setOnBindEditTextListener(editText -> {
            editText.setFilters(new InputFilter[]{new ControlCharacterFilter()});
        });

        // smap - token authentication
        useTokenPreference = findPreference(ProjectKeys.KEY_SMAP_USE_TOKEN);
        scanButton = findPreference(ProjectKeys.KEY_SMAP_SCAN_TOKEN);
        authTokenPreference = findPreference(ProjectKeys.KEY_SMAP_AUTH_TOKEN);

        if (useTokenPreference != null) {
            Settings settings = settingsProvider.getUnprotectedSettings();
            boolean forceToken = settings.getBoolean(ProjectKeys.KEY_SMAP_FORCE_TOKEN);
            if (forceToken) {
                useTokenPreference.setChecked(true);
                useTokenPreference.setEnabled(false);
            }

            useTokenPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                useTokenChanged((Boolean) newValue);
                return true;
            });

            useTokenChanged(useTokenPreference.isChecked());
        }

        if (scanButton != null) {
            scanButton.setButtonClickListener(v ->
                    qrScanLauncher.launch(new Intent(requireContext(), SmapLoginQRActivity.class))
            );
        }

        if (authTokenPreference != null) {
            String currentToken = authTokenPreference.getText();
            authTokenPreference.setSummary(currentToken != null ? currentToken : "");
        }
    }

    private void useTokenChanged(boolean useToken) {
        if (serverUrlPreference != null) {
            serverUrlPreference.setEnabled(!useToken);
        }
        if (usernamePreference != null) {
            usernamePreference.setEnabled(!useToken);
        }
        if (passwordPreference != null) {
            passwordPreference.setVisible(!useToken);
        }
        if (scanButton != null) {
            scanButton.setVisible(useToken);
        }
        if (authTokenPreference != null) {
            authTokenPreference.setVisible(useToken);
        }
    }

    private Preference.OnPreferenceChangeListener createChangeListener() {
        return (preference, newValue) -> {
            switch (preference.getKey()) {
                case ProjectKeys.KEY_SERVER_URL:
                    String url = newValue.toString();

                    if (Validator.isUrlValid(url)) {
                        preference.setSummary(newValue.toString());
                    } else {
                        ToastUtils.showShortToast(org.odk.collect.strings.R.string.url_error);
                        return false;
                    }
                    break;

                case ProjectKeys.KEY_USERNAME:
                    String username = newValue.toString();

                    // do not allow leading and trailing whitespace
                    if (!username.equals(username.trim())) {
                        ToastUtils.showShortToast(org.odk.collect.strings.R.string.username_error_whitespace);
                        return false;
                    }

                    preference.setSummary(username);
                    return true;

                case ProjectKeys.KEY_PASSWORD:
                    String pw = newValue.toString();

                    // do not allow leading and trailing whitespace
                    if (!pw.equals(pw.trim())) {
                        ToastUtils.showShortToast(org.odk.collect.strings.R.string.password_error_whitespace);
                        return false;
                    }

                    maskPasswordSummary(pw);
                    break;
            }
            return true;
        };
    }

    private void maskPasswordSummary(String password) {
        passwordPreference.setSummary(password != null && password.length() > 0
                ? "********"
                : "");
    }
}
