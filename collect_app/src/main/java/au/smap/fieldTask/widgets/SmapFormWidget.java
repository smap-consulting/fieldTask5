/*
 * Copyright (C) 2018 Smap Consulting
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

package au.smap.fieldTask.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.activities.FormFillingActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.dynamicpreload.ExternalAppsUtils;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.javarosawrapper.FormController;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.utilities.SoftKeyboardController;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import au.smap.fieldTask.models.FormLaunchDetail;
import au.smap.fieldTask.utilities.ManageForm;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Launch another form
 *
 * @author neilpenman@smap.com.au
 */
@SuppressLint("ViewConstructor")
public class SmapFormWidget extends QuestionWidget implements WidgetDataReceiver {
    // If an extra with this key is specified, it will be parsed as a URI and used as intent data
    private static final String URI_KEY = "uri_data";

    protected EditText answer;
    private boolean hasExApp = true;
    public final MaterialButton launchIntentButton;
    public EditText launching;
    private final Drawable textBackground;

    private ManageForm mf;
    private String initialData;
    private ManageForm.ManageFormDetails mfd;
    private FormFillingActivity mFormFillingActivity;

    private long formId;

    private final String appearance;
    private final boolean readOnlyOverride;
    private boolean validForm = true;


    public SmapFormWidget(Context context, QuestionDetails questionDetails,
                         String appearance, Dependencies dependencies) {
        super(context, dependencies, questionDetails);

        this.appearance = appearance;
        this.readOnlyOverride = false;

        // Initialize form management
        mf = new ManageForm();
        mFormFillingActivity = (FormFillingActivity) context;

        // Get the details on the form to be launched
        String formIdent = questionDetails.getPrompt().getQuestion().getAdditionalAttribute(null, "form_identifier");
        initialData = questionDetails.getPrompt().getQuestion().getAdditionalAttribute(null, "initial");

        // Update initial data with dynamic values from current form
        if (initialData != null) {
            FormController fc = Collect.getInstance().getFormController();
            if (fc != null) {
                FormDef fd = fc.getFormDef();
                HashMap<String, String> dynVariables = getDynamicVariables(initialData);
                if (dynVariables.size() > 0) {
                    fd.populateLaunchModel(dynVariables);       // Get matching values from the current form
                    for (String k : dynVariables.keySet()) {
                        String v = dynVariables.get(k);
                        if (v == null) {
                            v = "";
                        }
                        initialData = initialData.replace("${" + k + "}", v);
                    }
                }
            }
        }

        if (formIdent == null) {
            validForm = false;
            Toast.makeText(getContext(),
                    Collect.getInstance().getString(R.string.smap_form_not_specified),
                    Toast.LENGTH_SHORT)
                    .show();
        } else {
            mfd = mf.getFormDetailsNoVersion(formIdent);
            validForm = mfd.exists;
            if (!validForm) {
                Toast.makeText(getContext(),
                        Collect.getInstance().getString(R.string.smap_form_not_found).replace("%s", formIdent),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }

        // Initialize textBackground storage
        EditText tempEdit = new EditText(context);
        textBackground = tempEdit.getBackground();

        // Create button
        String v = getFormEntryPrompt().getSpecialFormQuestionText("buttonText");
        String buttonText = (v != null) ? v : context.getString(org.odk.collect.strings.R.string.launch_app);

        if (validForm) {
            buttonText += " " + mfd.formName;
        }

        launchIntentButton = new MaterialButton(context);
        launchIntentButton.setId(View.generateViewId());
        launchIntentButton.setText(buttonText);
        launchIntentButton.setEnabled(validForm && !readOnlyOverride && !getFormEntryPrompt().isReadOnly());
        launchIntentButton.setOnClickListener(v1 -> onButtonClick());

        render();
    }

    @Override
    protected View onCreateWidgetView(@NonNull Context context,
                                     @NonNull FormEntryPrompt prompt,
                                     int answerFontSize) {
        TableLayout.LayoutParams params = new TableLayout.LayoutParams();
        params.setMargins(7, 5, 7, 5);

        // set text formatting
        answer = new EditText(context);
        answer.setId(View.generateViewId());
        answer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);
        answer.setLayoutParams(params);
        answer.setBackground(null);
        answer.setTextColor(themeUtils.getColorOnSurface());

        // capitalize nothing
        answer.setKeyListener(new TextKeyListener(Capitalize.NONE, false));

        // needed to make long read only text scroll
        answer.setHorizontallyScrolling(false);
        answer.setSingleLine(false);

        String s = prompt.getAnswerText();
        if (s != null && s.startsWith("::")) {
            validForm = false;
            Toast.makeText(getContext(),
                    Collect.getInstance().getString(R.string.smap_form_completed, mfd.formName),
                    Toast.LENGTH_SHORT)
                    .show();
            answer.setText(s);
        }

        if (prompt.isReadOnly() || readOnlyOverride || !validForm) {
            answer.setFocusable(false);
            answer.setEnabled(false);
        }

        // set text formatting for launching indicator
        launching = new EditText(context);
        launching.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);
        launching.setLayoutParams(params);
        launching.setBackground(null);
        launching.setTextColor(themeUtils.getColorOnSurface());
        launching.setGravity(Gravity.CENTER);
        launching.setVisibility(GONE);
        if (validForm && mfd != null) {
            String launchingText = context.getString(R.string.smap_starting_form).replace("%s", mfd.formName);
            launching.setText(launchingText);
        }

        // finish complex layout
        LinearLayout answerLayout = new LinearLayout(getContext());
        answerLayout.setOrientation(LinearLayout.VERTICAL);
        answerLayout.addView(launchIntentButton);
        answerLayout.addView(answer);
        answerLayout.addView(launching);

        return answerLayout;
    }

    @Override
    public void clearAnswer() {
        answer.setText(null);
        widgetValueChanged();
    }

    @Override
    public IAnswerData getAnswer() {
        String s = answer.getText().toString();
        return !s.isEmpty() ? new StringData(s) : null;
    }

    /**
     * Allows answer to be set externally in {@link FormEntryActivity}.
     */
    @Override
    public void setData(Object answer) {
        StringData stringData = ExternalAppsUtils.asStringData(answer);
        this.answer.setText(stringData == null ? null : stringData.getValue().toString());
        widgetValueChanged();
    }

    @Override
    public void setFocus(Context context) {
        if (hasExApp) {
            softKeyboardController.hideSoftKeyboard(answer);
            // focus on launch button
            launchIntentButton.requestFocus();
        } else {
            if (!getFormEntryPrompt().isReadOnly()) {
                softKeyboardController.showSoftKeyboard(answer);
            } else {
                softKeyboardController.hideSoftKeyboard(answer);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return !event.isAltPressed() && super.onKeyDown(keyCode, event);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        answer.setOnLongClickListener(l);
        launchIntentButton.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        answer.cancelLongPress();
        launchIntentButton.cancelLongPress();
    }

    private void onButtonClick() {
        // 1. Save restore information in collect app
        String instancePath = Collect.getInstance().getFormController().getInstanceFile().getAbsolutePath();
        FormIndex formIndex = Collect.getInstance().getFormController().getFormIndex();

        String title = (String) mFormFillingActivity.getTitle();
        Collect.getInstance().pushToFormStack(new FormLaunchDetail(new StoragePathProvider().getInstanceDbPath(instancePath), formIndex, title));

        // 2. Set form details to be launched in collect app
        Collect.getInstance().pushToFormStack(new FormLaunchDetail(mfd.id, mfd.formName, initialData));

        // 3. Save and exit current form
        mFormFillingActivity.saveForm(true, false,
                null, false);
    }

    private void focusAnswer() {
        softKeyboardController.showSoftKeyboard(answer);
    }

    /*
     * Extract question placeholders out of XML
     */
    private HashMap<String, String> getDynamicVariables(String input) {
        HashMap<String, String> dynVariables = new HashMap<>();
        Pattern pattern = Pattern.compile("\\$\\{.+?\\}");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String matched = matcher.group();
            String qname = matched.substring(2, matched.length() - 1).trim();
            dynVariables.put(qname, "");
        }
        return dynVariables;
    }
}
