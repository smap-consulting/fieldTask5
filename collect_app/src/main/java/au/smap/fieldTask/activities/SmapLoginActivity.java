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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SwitchCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.configure.qr.QRCodeTabsActivity;
import au.smap.fieldTask.listeners.SmapLoginListener;
import org.odk.collect.settings.keys.ProjectKeys;
import au.smap.fieldTask.tasks.SmapLoginTask;
import au.smap.fieldTask.preferences.GeneralSharedPreferencesSmap;
import au.smap.fieldTask.utilities.KeyValueString;
import org.odk.collect.androidshared.ui.SnackbarUtils;
import org.odk.collect.androidshared.utils.Validator;

import java.util.ArrayList;

import org.odk.collect.android.databinding.SmapActivityLoginBinding;
import timber.log.Timber;

public class SmapLoginActivity extends CollectAbstractActivity implements SmapLoginListener {

    private SmapActivityLoginBinding binding;

    private String url;
    private final ActivityResultLauncher<Intent> formLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        gotResult(RESULT_OK, result.getData());
    });

    private Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm").create();

    private void gotResult(int resultOk, Intent data) {
        if(data != null) {
            String url = data.getStringExtra("server_url");
            if (url == null) {
                url = "";
            }
            binding.inputUrl.setText(url);

            String user = data.getStringExtra("username");
            if (user == null) {
                user = "";
            }
            binding.inputUsername.setText(user);

            String token = data.getStringExtra("auth_token");
            if (token == null) {
                token = "";
            }
            binding.authToken.setText(token);
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setTheme(R.style.DarkAppTheme);     // override theme for login
        binding = SmapActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        boolean forceToken = (Boolean) GeneralSharedPreferencesSmap.getInstance().get(ProjectKeys.KEY_SMAP_FORCE_TOKEN);
        if(forceToken) {
            binding.smapUseToken.setChecked(true);
            binding.smapUseToken.setEnabled(false);
        }

        // Responds to switch being checked/unchecked
        useTokenChanged(binding.smapUseToken.isChecked());
        binding.smapUseToken.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton var, boolean b) {
                useTokenChanged(b);
            }

        });

        url = (String) GeneralSharedPreferencesSmap.getInstance().get(ProjectKeys.KEY_SERVER_URL);
        binding.inputUrl.setText(url);

        binding.inputUsername.setText((String) GeneralSharedPreferencesSmap.getInstance().get(ProjectKeys.KEY_USERNAME));

        binding.inputPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    login();
                }
                return false;
            }
        });

        binding.btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                formLauncher.launch(new Intent(SmapLoginActivity.this, QRCodeTabsActivity.class));
            }
        });

        binding.authToken.setEnabled(false);

        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    public void login() {
        Timber.i("Login started");

        boolean useToken = binding.smapUseToken.isChecked();
        url = binding.inputUrl.getText().toString();
        String username = binding.inputUsername.getText().toString();
        String password = binding.inputPassword.getText().toString();
        String token = binding.authToken.getText().toString();

        if (!validate(useToken, url, username, password, token)) {
            return;
        }

        binding.btnLogin.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        SmapLoginTask smapLoginTask = new SmapLoginTask();
        smapLoginTask.setListener(this);
        smapLoginTask.execute(String.valueOf(useToken), url, username, password, token);

    }

    @Override
    public void loginComplete(String status) {
        Timber.i("---------- %s", status);

        binding.progressBar.setVisibility(View.GONE);
        binding.btnLogin.setEnabled(true);

        if(status == null || status.startsWith("error")) {
            loginFailed(status);
        } else if(status.equals("success")) {
            loginSuccess();
        } else if (status.equals("unauthorized")) {
            loginNotAuthorized(null);
        } else {
            loginFailed(null);
        }
    }

    public void loginSuccess() {

        // Update preferences with login values
        GeneralSharedPreferencesSmap prefs = GeneralSharedPreferencesSmap.getInstance();
        prefs.save(ProjectKeys.KEY_SERVER_URL, url);
        prefs.save(ProjectKeys.KEY_USERNAME, binding.inputUsername.getText().toString());
        prefs.save(ProjectKeys.KEY_SMAP_USE_TOKEN, binding.smapUseToken.isChecked());

        if(binding.smapUseToken.isChecked()) {
            prefs.save(ProjectKeys.KEY_SMAP_AUTH_TOKEN, binding.authToken.getText().toString());
        } else {
            prefs.save(ProjectKeys.KEY_PASSWORD, binding.inputPassword.getText().toString());
            saveUserHistory(prefs, binding.inputUsername.getText().toString(), binding.inputPassword.getText().toString());      // Save logon details for multiple users to allow logon offline
        }

        // Save the login time in case the password policy is set to periodic
        prefs.save(ProjectKeys.KEY_SMAP_LAST_LOGIN, String.valueOf(System.currentTimeMillis()));

        // Start Main Activity and initiate a refresh
        Intent i = new Intent(SmapLoginActivity.this, SmapMain.class);
        i.putExtra(SmapMain.EXTRA_REFRESH, "yes");
        i.putExtra(SmapMain.LOGIN_STATUS, "success");
        startActivity(i);  //smap
        finish();
    }

    public void loginFailed(String status) {

        // Attempt to login by comparing values against stored preferences
        GeneralSharedPreferencesSmap prefs = GeneralSharedPreferencesSmap.getInstance();
        boolean useToken = binding.smapUseToken.isChecked();
        String username = binding.inputUsername.getText().toString();
        String password = binding.inputPassword.getText().toString();
        String token = binding.authToken.getText().toString();

        String prefUrl = (String) prefs.get(ProjectKeys.KEY_SERVER_URL);
        String prefUsername = (String) prefs.get(ProjectKeys.KEY_USERNAME);
        String prefPassword = (String) prefs.get(ProjectKeys.KEY_PASSWORD);
        String prefToken = (String) prefs.get(ProjectKeys.KEY_SMAP_AUTH_TOKEN);
        if(url.equals(prefUrl)) {
            if((useToken && username.equals(prefUsername) && token.equals(prefToken))
                    || (!useToken && username.equals(prefUsername) && password.equals(prefPassword))
                    || (!useToken && offlineLogonCheck(prefs, username, password))) {

                // Save the preferences
                prefs.save(ProjectKeys.KEY_SERVER_URL, url);
                prefs.save(ProjectKeys.KEY_USERNAME, binding.inputUsername.getText().toString());
                prefs.save(ProjectKeys.KEY_SMAP_USE_TOKEN, binding.smapUseToken.isChecked());

                // Start Main Activity no refresh as presumably there is no network
                Intent i = new Intent(SmapLoginActivity.this, SmapMain.class);
                i.putExtra(SmapMain.EXTRA_REFRESH, "no");
                i.putExtra(SmapMain.LOGIN_STATUS, "failed");
                startActivity(i);  //smap
                finish();
            } else {
                loginNotAuthorized(status); // Credentials do not match
            }
        } else {
            loginNotAuthorized(status);   // User previously logged into a different server
        }

    }

    public void loginNotAuthorized(String status) {
        String msg = Collect.getInstance().getString(R.string.smap_login_unauthorized);
        if(status != null && status.startsWith("error:")) {
            msg += "; " + status.substring(5);
        }
        SnackbarUtils.showSnackbar(binding.getRoot(), msg, SnackbarUtils.DURATION_SHORT);

    }

    public boolean validate(boolean useToken, String url, String username, String pw, String token) {
        boolean valid = true;

        // remove all trailing "/"s
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        if (!Validator.isUrlValid(url)) {
            binding.inputUrl.setError(Collect.getInstance().getString(org.odk.collect.strings.R.string.url_error));
            valid = false;
        } else {
            binding.inputUrl.setError(null);
        }

        if(useToken) {
            if (token.isEmpty()) {
                binding.inputPassword.setError(Collect.getInstance().getString(org.odk.collect.strings.R.string.password_error_whitespace));
                valid = false;
            } else {
                binding.authToken.setError(null);
            }
        } else {
            if (pw.isEmpty() || !pw.equals(pw.trim())) {
                binding.inputPassword.setError(Collect.getInstance().getString(org.odk.collect.strings.R.string.password_error_whitespace));
                valid = false;
            } else {
                binding.inputPassword.setError(null);
            }

            if (username.isEmpty() || !username.equals(username.trim())) {
                binding.inputUsername.setError(Collect.getInstance().getString(org.odk.collect.strings.R.string.username_error_whitespace));
                valid = false;
            } else {
                binding.inputUsername.setError(null);
            }
        }

        return valid;
    }

    private void useTokenChanged(boolean useToken) {
        // show or hide basic authentication preferences
        binding.inputUrl.setEnabled(!useToken);
        binding.inputUsername.setEnabled(!useToken);
        binding.inputPasswordLayout.setVisibility(useToken ? View.GONE : View.VISIBLE);

        // show or hide token authentication preferences
        binding.btnScan.setVisibility(!useToken ? View.GONE : View.VISIBLE);
        binding.authTokenLayout.setVisibility(!useToken ? View.GONE : View.VISIBLE);

    }

    private CharSequence[] getChoices() {
        CharSequence[] choices = new CharSequence[2];
        choices[0] = "https://app.kontrolid.org";
        choices[1] = "https://app.kontrolid.com";
        return choices;
    }

    /*
     * Save logon details of last 5 users to allow for offline logout and logon
     */
    private void saveUserHistory(GeneralSharedPreferencesSmap prefs, String user, String password) {
        // Get existing logon array
        String savedUsersString = (String) prefs.get(ProjectKeys.KEY_SAVED_USERS);
        ArrayList<KeyValueString> savedUsers = new ArrayList<> ();
        if(savedUsersString != null && !savedUsersString.trim().isEmpty()) {
            savedUsers = gson.fromJson(savedUsersString, new TypeToken<ArrayList<KeyValueString>>() {}.getType());
        }

        // Remove any existing entries for this user
        for(KeyValueString p : savedUsers) {
            if(p.key.equals(user)) {
                savedUsers.remove(p);
                break;
            }
        }

        // Add the new logon details to the set
        savedUsers.add(new KeyValueString(user, password));

        // Remove any old credentials should be a maximum of 5
        if(savedUsers.size() > 5) {
            for(int i = savedUsers.size(); i > 5; i--) {
                savedUsers.remove(0);
            }
        }

        prefs.save(ProjectKeys.KEY_SAVED_USERS, gson.toJson(savedUsers));
    }

    private boolean offlineLogonCheck(GeneralSharedPreferencesSmap prefs, String user, String password) {
        boolean credsFound = false;

        String savedUsersString = (String) prefs.get(ProjectKeys.KEY_SAVED_USERS);
        ArrayList<KeyValueString> savedUsers = new ArrayList<> ();
        if(savedUsersString != null && !savedUsersString.trim().isEmpty()) {
            savedUsers = gson.fromJson(savedUsersString, new TypeToken<ArrayList<KeyValueString>>() {}.getType());
        }

        for(KeyValueString p : savedUsers) {
            if(p.key.equals(user) && p.value.equals(password)) {
                credsFound = true;
                break;
            }
        }

        return credsFound;
    }
}
