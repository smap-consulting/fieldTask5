package org.odk.collect.android.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.odk.collect.android.R;

/**
 * smap - Preference that renders as a MaterialButton for QR code scanning.
 */
public class ScanButtonPreference extends Preference {
    private View.OnClickListener buttonClickListener;

    public ScanButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_scan_button);
    }

    public void setButtonClickListener(View.OnClickListener listener) {
        this.buttonClickListener = listener;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View btn = holder.findViewById(R.id.scan_button);
        if (btn != null) {
            btn.setOnClickListener(buttonClickListener);
        }
        // Let the button handle clicks, not the preference row
        holder.itemView.setClickable(false);
    }
}
