/*
 * Copyright (C) 2016 Smap Consulting
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import au.smap.fieldTask.activities.NFCActivity;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;


/**
 * Widget that allows user to scan NFC Id's and add them to the form.
 *
 * @author Neil Penman (neilpenman@gmail.com)
 * Based on BarcodeWidget by Yaw Anokwa (yanokwa@gmail.com)
 */
public class NfcWidget extends QuestionWidget implements WidgetDataReceiver {
	private MaterialButton mGetNfcButton;
	private TextView mStringAnswer;
    private NfcAdapter mNfcAdapter;
    private final WaitingForDataRegistry waitingForDataRegistry;

	public NfcWidget(Context context, QuestionDetails questionDetails,
	                 Dependencies dependencies, WaitingForDataRegistry waitingForDataRegistry) {
		super(context, dependencies, questionDetails);

        this.waitingForDataRegistry = waitingForDataRegistry;

		// Initialize NFC adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);

		// Create button
		String buttonText = mNfcAdapter == null || !mNfcAdapter.isEnabled()
		    ? context.getString(R.string.smap_nfc_not_available)
		    : context.getString(R.string.smap_read_nfc);

		String s = questionDetails.getPrompt().getAnswerText();
		if (s != null && !s.isEmpty()) {
			buttonText = context.getString(R.string.smap_replace_nfc);
		}

		mGetNfcButton = new MaterialButton(context);
		mGetNfcButton.setId(View.generateViewId());
		mGetNfcButton.setText(buttonText);
        mGetNfcButton.setEnabled(!questionDetails.getPrompt().isReadOnly() && mNfcAdapter != null && mNfcAdapter.isEnabled());
		mGetNfcButton.setOnClickListener(v -> onButtonClick());

		render();
	}

	@Override
	protected View onCreateWidgetView(@NonNull Context context,
	                                 @NonNull FormEntryPrompt prompt,
	                                 int answerFontSize) {
		TableLayout.LayoutParams params = new TableLayout.LayoutParams();
		params.setMargins(7, 5, 7, 5);

		// set text formatting
		mStringAnswer = new TextView(context);
		mStringAnswer.setId(View.generateViewId());
		mStringAnswer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize);
		mStringAnswer.setLayoutParams(params);
		mStringAnswer.setTextColor(themeUtils.getColorOnSurface());

		String s = prompt.getAnswerText();
		if (s != null) {
			mStringAnswer.setText(s);
		}

		// finish complex layout
        LinearLayout answerLayout = new LinearLayout(getContext());
        answerLayout.setOrientation(LinearLayout.VERTICAL);
        answerLayout.addView(mGetNfcButton);
        answerLayout.addView(mStringAnswer);

        return answerLayout;
	}

	@Override
	public void clearAnswer() {
		mStringAnswer.setText(null);
		mGetNfcButton.setText(getContext().getString(R.string.smap_read_nfc));
		widgetValueChanged();
	}

	@Override
	public IAnswerData getAnswer() {
		String s = mStringAnswer.getText().toString();
		return !s.isEmpty() ? new StringData(s) : null;
	}

	/**
	 * Allows answer to be set externally in {@Link FormFillingActivity}.
	 */
    @Override
    public void setData(Object answer) {
        mStringAnswer.setText((String) answer);
		mGetNfcButton.setText(getContext().getString(R.string.smap_replace_nfc));
        widgetValueChanged();
    }

	@Override
	public void setFocus(Context context) {
		// Hide the soft keyboard if it's showing.
		softKeyboardController.hideSoftKeyboard(mStringAnswer);
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		mStringAnswer.setOnLongClickListener(l);
		mGetNfcButton.setOnLongClickListener(l);
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		mGetNfcButton.cancelLongPress();
		mStringAnswer.cancelLongPress();
	}

    private void onButtonClick() {
        waitingForDataRegistry.waitForData(getFormEntryPrompt().getIndex());
        Intent i = new Intent(getContext(), NFCActivity.class);
        ((Activity) getContext()).startActivityForResult(i,
                ApplicationConstants.RequestCodes.NFC_CAPTURE);
    }
}
