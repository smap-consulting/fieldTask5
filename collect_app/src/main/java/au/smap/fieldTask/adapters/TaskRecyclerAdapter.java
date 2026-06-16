/*
 * Copyright (C) 2025 Smap Consulting Pty Ltd
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
 * smap - RecyclerView adapter for the task list. Replaces the ListView based TaskListArrayAdapter
 * for the Tasks tab so that swipe-to-reject/release can be implemented with ItemTouchHelper, the
 * standard Android swipe convention. The Forms tab still uses TaskListArrayAdapter.
 */
package au.smap.fieldTask.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.odk.collect.android.R;
import org.odk.collect.android.database.instances.DatabaseInstancesRepository;
import org.odk.collect.forms.instances.Instance;

import au.smap.fieldTask.listeners.OnTaskOptionsClickListener;
import au.smap.fieldTask.loaders.TaskEntry;
import au.smap.fieldTask.utilities.KeyValueJsonFns;
import au.smap.fieldTask.utilities.Utilities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaskRecyclerAdapter extends RecyclerView.Adapter<TaskRecyclerAdapter.ViewHolder> {

    public interface OnRowClickListener {
        void onRowClick(TaskEntry entry);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final OnTaskOptionsClickListener taskClickListener;
    private final OnRowClickListener rowClickListener;

    private boolean showReferences;        // Show read only references instead of actionable tasks
    private final List<TaskEntry> items = new ArrayList<>();

    public TaskRecyclerAdapter(Context context, OnTaskOptionsClickListener taskClickListener,
                               OnRowClickListener rowClickListener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.taskClickListener = taskClickListener;
        this.rowClickListener = rowClickListener;
    }

    public void setShowReferences(boolean showReferences) {
        this.showReferences = showReferences;
    }

    private static boolean isReference(TaskEntry item) {
        return item.taskType != null && item.taskType.equals("reference");
    }

    // A dereferenced (cancelled) reference is hidden from the list - it stays in the database
    // flagged for sync and is removed by the server on the next download.
    private static boolean isDereferenced(TaskEntry item) {
        return isReference(item) && Utilities.STATUS_T_CANCELLED.equals(item.taskStatus);
    }

    /*
     * Task/case rows can be swiped to reject/release; reference rows can be swiped to dereference.
     * Only form rows (never shown in the Tasks tab anyway) are not swipeable.
     */
    public boolean isSwipeable(int position) {
        if (position < 0 || position >= items.size()) {
            return false;
        }
        TaskEntry item = items.get(position);
        return !item.type.equals("form");
    }

    public TaskEntry getItem(int position) {
        return items.get(position);
    }

    /* Remove a row (used by swipe-to-dereference, paired with restoreItem for undo). */
    public TaskEntry removeItem(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        TaskEntry removed = items.remove(position);
        notifyItemRemoved(position);
        return removed;
    }

    /* Put a removed row back (undo). */
    public void restoreItem(int position, TaskEntry entry) {
        int target = Math.min(position, items.size());
        items.add(target, entry);
        notifyItemInserted(target);
    }

    /*
     * Filter the supplied list to the rows shown in the Tasks tab: non-form rows matching the
     * current references toggle.
     */
    public void setData(List<TaskEntry> data) {
        items.clear();
        if (data != null) {
            for (TaskEntry item : data) {
                if (item.type.equals("form") || isDereferenced(item)) {
                    continue;
                }
                if (showReferences == isReference(item)) {
                    items.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.task_row, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        View view = holder.itemView;
        TaskEntry item = items.get(position);

        boolean isReference = isReference(item);

        /*
         * Get icon drawable
         */
        Drawable d = null;
        if (isReference) {
            d = ContextCompat.getDrawable(context, R.drawable.form_state_readonly_circle);
        } else if (item.formDeleted) {
            d = ContextCompat.getDrawable(context, R.drawable.form_state_orphan);
        } else if (item.taskStatus != null) {
            if (item.taskStatus.equals(Utilities.STATUS_T_ACCEPTED)) {
                if (item.locationTrigger != null && !item.repeat) {
                    d = ContextCompat.getDrawable(context, R.drawable.form_state_triggered);
                } else if (item.locationTrigger != null && item.repeat) {
                    d = ContextCompat.getDrawable(context, R.drawable.form_state_triggered_repeat);
                } else if (item.repeat) {
                    d = ContextCompat.getDrawable(context, R.drawable.form_state_repeat);
                } else if (item.taskFinish != 0 && item.taskFinish < (new Date()).getTime()) {
                    d = ContextCompat.getDrawable(context, R.drawable.form_state_late);
                } else {
                    if (item.taskType != null && item.taskType.equals("case")) {
                        d = ContextCompat.getDrawable(context, R.drawable.case_open);
                    } else {
                        d = ContextCompat.getDrawable(context, R.drawable.form_state_saved_circle);
                    }
                }
            } else if (item.taskStatus.equals(Utilities.STATUS_T_COMPLETE)) {
                if (item.taskType != null && item.taskType.equals("case")) {
                    d = ContextCompat.getDrawable(context, R.drawable.case_updated);
                } else {
                    d = ContextCompat.getDrawable(context, R.drawable.form_state_finalized_circle);
                }
            } else if (item.taskStatus.equals(Utilities.STATUS_T_REJECTED) || item.taskStatus.equals(Utilities.STATUS_T_CANCELLED)) {
                d = ContextCompat.getDrawable(context, R.drawable.form_state_rejected);
            } else if (item.taskStatus.equals(Utilities.STATUS_T_SUBMITTED)) {
                d = ContextCompat.getDrawable(context, R.drawable.form_state_submitted_circle);
            } else if (item.taskStatus.equals(Utilities.STATUS_T_NEW)) {
                d = ContextCompat.getDrawable(context, R.drawable.form_state_new);
            }
        }
        ImageView icon = view.findViewById(R.id.icon);
        if (d != null) {
            icon.setImageDrawable(d);
            // References are shown in the reference colour (purple), matching the web console
            if (isReference) {
                icon.setColorFilter(Color.parseColor("#6F42C1"));
            } else {
                icon.clearColorFilter();
            }
        } else {
            icon.setImageDrawable(null);
            icon.clearColorFilter();
        }

        TextView taskNameText = view.findViewById(R.id.toptext);
        if (taskNameText != null) {
            taskNameText.setText(item.name + " (v:" + item.formVersion + ")");
        }

        TextView taskStartText = view.findViewById(R.id.middletext);
        if (taskStartText != null) {
            if (item.taskType != null && item.taskType.equals("case")) {
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

        TextView taskEndText = view.findViewById(R.id.bottomtext);
        if (taskEndText != null) {
            taskEndText.setVisibility(View.GONE);
            String addressText = KeyValueJsonFns.getValues(item.taskAddress);
            if (addressText != null && addressText.trim().length() > 0) {
                taskEndText.setVisibility(View.VISIBLE);
                taskEndText.setText(addressText);
            }
        }

        // smap: references are read only so they have no accept/reject/release menu
        View menuButton = view.findViewById(R.id.menu_button);
        if (isReference) {
            menuButton.setVisibility(View.GONE);
        } else {
            menuButton.setVisibility(View.VISIBLE);
            menuButton.setOnClickListener(v -> showOptionsPopup(item, taskNameText));
        }

        view.setOnClickListener(v -> rowClickListener.onRowClick(item));

        Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
        view.startAnimation(animation);
    }

    private void showOptionsPopup(TaskEntry item, TextView taskNameText) {
        DatabaseInstancesRepository di = new DatabaseInstancesRepository();
        Instance instance = di.getInstanceByTaskId(item.assId);

        View popupTaskView = inflater.inflate(R.layout.popup_task_window, null, false);
        TextView textView = popupTaskView.findViewById(R.id.task_name);
        textView.setText(taskNameText != null ? taskNameText.getText() : item.name);

        Button accept = popupTaskView.findViewById(R.id.accept);
        Button locate = popupTaskView.findViewById(R.id.locate);
        Button sms = popupTaskView.findViewById(R.id.sms);
        Button directions = popupTaskView.findViewById(R.id.directions);
        Button phone = popupTaskView.findViewById(R.id.phone);
        Button reject = popupTaskView.findViewById(R.id.reject);

        if (item.taskType != null && item.taskType.equals("case")) {
            reject.setText(context.getString(R.string.smap_release_case));
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

        androidx.appcompat.app.AlertDialog alertDialog = new MaterialAlertDialogBuilder(context)
                .setView(popupTaskView)
                .create();

        accept.setOnClickListener(v -> { alertDialog.dismiss(); taskClickListener.onAcceptClicked(item); });
        phone.setOnClickListener(v -> { alertDialog.dismiss(); taskClickListener.onPhoneClicked(item); });
        sms.setOnClickListener(v -> { alertDialog.dismiss(); taskClickListener.onSMSClicked(item); });
        directions.setOnClickListener(v -> { alertDialog.dismiss(); taskClickListener.onDirectionsClicked(item); });
        reject.setOnClickListener(v -> { alertDialog.dismiss(); taskClickListener.onRejectClicked(item); });
        locate.setOnClickListener(v -> { alertDialog.dismiss(); taskClickListener.onLocateClick(item); });

        alertDialog.show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
