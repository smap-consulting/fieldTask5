/*
 * Copyright (C) 2011 Smap Consulting Pty Ltd
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

/*
 * Responsible for displaying tasks in a list view
 *
 * @author Neil Penman (neilpenman@gmail.com)
 */
package au.smap.fieldTask.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.odk.collect.android.R;
import org.odk.collect.android.database.instances.DatabaseInstancesRepository;
import org.odk.collect.forms.instances.Instance;

import au.smap.fieldTask.listeners.OnTaskOptionsClickListener;
import au.smap.fieldTask.loaders.TaskEntry;
import au.smap.fieldTask.utilities.KeyValueJsonFns;
import au.smap.fieldTask.utilities.Utilities;

import java.util.Date;
import java.util.List;

public class TaskListArrayAdapter extends ArrayAdapter<TaskEntry> {

    private int mLayout;
    boolean mFormView;
    OnTaskOptionsClickListener taskClickLisener;
    LayoutInflater mInflater;
    static String TAG = "TaskListArrayAdapter";

    public TaskListArrayAdapter(Context context, boolean formView, OnTaskOptionsClickListener taskClickLisener) {
        super(context, R.layout.main_list);
        mLayout = R.layout.task_row;
        this.taskClickLisener = taskClickLisener;
        mInflater = LayoutInflater.from(context);
        mFormView = formView;
    }


    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view;

        if (convertView == null) {
            view = mInflater.inflate(mLayout, parent, false);
        } else {
            view = convertView;
        }

        TaskEntry item = getItem(position);

        /*
         * Get icon drawable
         */
        Drawable d = null;
        if (item.type.equals("form")) {
            if (item.readOnly) {
                d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_readonly_circle);
            } else {
                d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_blank_circle);
            }
        } else if (item.formDeleted) {
            d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_orphan);
        } else if (item.taskStatus != null) {
            if (item.taskStatus.equals(Utilities.STATUS_T_ACCEPTED)) {
                if (item.locationTrigger != null && !item.repeat) {
                    d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_triggered);
                } else if (item.locationTrigger != null && item.repeat) {
                    d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_triggered_repeat);
                } else if (item.repeat) {
                    d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_repeat);
                } else if (item.taskFinish != 0 && item.taskFinish < (new Date()).getTime()) {
                    d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_late);
                } else {
                    if (item.taskType != null && item.taskType.equals("case")) {
                        d = ContextCompat.getDrawable(getContext(), R.drawable.case_open);
                    } else {
                        d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_saved_circle);
                    }
                }
            } else if (item.taskStatus.equals(Utilities.STATUS_T_COMPLETE)) {
                if (item.taskType != null && item.taskType.equals("case")) {
                    d = ContextCompat.getDrawable(getContext(), R.drawable.case_updated);
                } else {
                    d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_finalized_circle);
                }
            } else if (item.taskStatus.equals(Utilities.STATUS_T_REJECTED) || item.taskStatus.equals(Utilities.STATUS_T_CANCELLED)) {
                d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_rejected);
            } else if (item.taskStatus.equals(Utilities.STATUS_T_SUBMITTED)) {
                d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_submitted_circle);
            } else if (item.taskStatus.equals(Utilities.STATUS_T_NEW)) {
                d = ContextCompat.getDrawable(getContext(), R.drawable.form_state_new);
            }
        }
        if (d != null) {
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            icon.setImageDrawable(d);
        }

        TextView taskNameText = view.findViewById(R.id.toptext);
        if (taskNameText != null) {
            taskNameText.setText(item.name + " (v:" + item.formVersion + ")");
        }

        TextView taskStartText = (TextView) view.findViewById(R.id.middletext);
        if (taskStartText != null) {
            if (item.type.equals("form")) {
                taskStartText.setText(getContext().getString(R.string.smap_project) + ": " + item.project);
            } else if (item.taskType != null && item.taskType.equals("case")) {
                taskStartText.setText(item.displayName);
            } else {
                String line2 = Utilities.getTaskTime(item.taskStatus, item.actFinish, item.taskStart);
                if (item.taskFinish > 0 && item.taskStatus != null && !item.taskStatus.equals(Utilities.STATUS_T_COMPLETE) &&
                        !item.taskStatus.equals(Utilities.STATUS_T_SUBMITTED)) {
                    line2 += " - " + Utilities.getTime(item.taskFinish);
                }
                taskStartText.setText(line2);
            }
        }

        TextView taskEndText = (TextView) view.findViewById(R.id.bottomtext);
        if (taskEndText != null) {
            taskEndText.setVisibility(View.GONE);
            if (!item.type.equals("form")) {
                String addressText = KeyValueJsonFns.getValues(item.taskAddress);
                if (addressText != null && addressText.trim().length() > 0) {
                    taskEndText.setVisibility(View.VISIBLE);
                    taskEndText.setText(addressText);
                }
            }
        }

        // smap: show menu button for task rows only; restore visibility on recycled views
        View imageButton = view.findViewById(R.id.menu_button);
        if (item.type.equals("form")) {
            imageButton.setVisibility(View.GONE);
        } else {
            imageButton.setVisibility(View.VISIBLE);
            imageButton.setOnClickListener(view16 -> { // smap
                DatabaseInstancesRepository di = new DatabaseInstancesRepository();
                Instance instance = di.getInstanceByTaskId(item.assId);

                View popupTaskView = mInflater.inflate(R.layout.popup_task_window, null, false);
                TextView textView = popupTaskView.findViewById(R.id.task_name);
                textView.setText(taskNameText != null ? taskNameText.getText() : item.name);

                Button accept = popupTaskView.findViewById(R.id.accept);
                Button locate = popupTaskView.findViewById(R.id.locate);
                Button sms = popupTaskView.findViewById(R.id.sms);
                Button directions = popupTaskView.findViewById(R.id.directions);
                Button phone = popupTaskView.findViewById(R.id.phone);
                Button reject = popupTaskView.findViewById(R.id.reject);

                if (item.taskType != null && item.taskType.equals("case")) {
                    reject.setText(getContext().getString(R.string.smap_release_case));
                    accept.setVisibility(View.GONE);
                }

                if (instance == null || instance.getPhone() == null) {
                    sms.setEnabled(false);
                    phone.setEnabled(false);
                }

                if (item.schedLat == 0 && item.schedLon == 0) {
                    locate.setEnabled(false);
                    directions.setEnabled(false);
                }

                androidx.appcompat.app.AlertDialog alertDialog = new MaterialAlertDialogBuilder(getContext())
                        .setView(popupTaskView)
                        .create();

                accept.setOnClickListener(v -> { alertDialog.dismiss(); taskClickLisener.onAcceptClicked(item); });
                phone.setOnClickListener(v -> { alertDialog.dismiss(); taskClickLisener.onPhoneClicked(item); });
                sms.setOnClickListener(v -> { alertDialog.dismiss(); taskClickLisener.onSMSClicked(item); });
                directions.setOnClickListener(v -> { alertDialog.dismiss(); taskClickLisener.onDirectionsClicked(item); });
                reject.setOnClickListener(v -> { alertDialog.dismiss(); taskClickLisener.onRejectClicked(item); });
                locate.setOnClickListener(v -> { alertDialog.dismiss(); taskClickLisener.onLocateClick(item); });

                alertDialog.show();
            });
        }

        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        view.startAnimation(animation);

        return view;
    }

    public int setData(List<TaskEntry> data) {
        clear();
        int count = 0;
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                if (mFormView && data.get(i).type.equals("form")) {
                    add(data.get(i));
                    count++;
                } else if (!mFormView && !data.get(i).type.equals("form")) {
                    add(data.get(i));
                    count++;
                }
            }
        }
        return count;
    }

}
