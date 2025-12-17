package au.smap.fieldTask.listeners;

import au.smap.fieldTask.loaders.TaskEntry;

public interface OnTaskOptionsClickListener {
     void onAcceptClicked(TaskEntry taskEntry);
     void onSMSClicked(TaskEntry taskEntry);
     void onPhoneClicked(TaskEntry taskEntry);
     void onDirectionsClicked(TaskEntry taskEntry);
     void onRejectClicked(TaskEntry taskEntry);
     void onLocateClick(TaskEntry taskEntry);
}
