package org.odk.collect.android.formentry

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ScrollView
import org.odk.collect.android.databinding.FormEntryEndBinding
import org.odk.collect.android.utilities.FormNameUtils

class FormEndView(
    context: Context,
    formTitle: String,
    defaultInstanceName: String,
    readOnly: Boolean,
    instanceComplete: Boolean,
    showInstanceName: Boolean,
    showMarkFinalized: Boolean,
    private val listener: Listener
) : SwipeHandler.View(context) {

    private val binding = FormEntryEndBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.description.text = context.getString(org.odk.collect.strings.R.string.save_enter_data_description, formTitle)

        // Setup save name edit text (smap: conditionally show based on setting)
        if (showInstanceName) {
            // Disallow carriage returns in the name
            val returnFilter = InputFilter { source, start, end, _, _, _ ->
                FormNameUtils.normalizeFormName(source.toString().substring(start, end), true)
            }
            binding.saveName.filters = arrayOf(returnFilter)
            binding.saveName.setText(defaultInstanceName)
            binding.saveName.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    listener.onSaveAsChanged(s.toString())
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            })
            binding.saveFormAs.visibility = android.view.View.VISIBLE
            binding.saveName.visibility = android.view.View.VISIBLE
        } else {
            binding.saveFormAs.visibility = android.view.View.GONE
            binding.saveName.visibility = android.view.View.GONE
        }

        // Setup mark finished checkbox (smap: conditionally show based on setting)
        if (showMarkFinalized) {
            binding.markFinished.isChecked = instanceComplete
            binding.markFinished.visibility = android.view.View.VISIBLE
        } else {
            binding.markFinished.visibility = android.view.View.GONE
            // When checkbox is hidden, set it to instanceComplete so it's used correctly
            binding.markFinished.isChecked = instanceComplete
        }

        // Setup save/exit button
        if (!readOnly) {
            // Note even instances that cannot be updated have to be saved as the comments are saved
            binding.saveExitButton.setOnClickListener {
                listener.onSaveClicked(binding.markFinished.isChecked)
            }
        } else {
            // Readonly do not save
            binding.saveExitButton.setText(org.odk.collect.strings.R.string.exit)
            binding.saveExitButton.setOnClickListener {
                listener.onExitClicked()
            }
        }
    }

    override fun shouldSuppressFlingGesture() = false

    override fun verticalScrollView(): androidx.core.widget.NestedScrollView? {
        return null
    }

    interface Listener {
        fun onSaveAsChanged(string: String)
        fun onSaveClicked(markAsFinalized: Boolean)
        fun onExitClicked()
    }
}
