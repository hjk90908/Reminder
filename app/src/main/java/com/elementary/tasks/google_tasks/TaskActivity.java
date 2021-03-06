package com.elementary.tasks.google_tasks;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.elementary.tasks.R;
import com.elementary.tasks.core.ThemedActivity;
import com.elementary.tasks.core.app_widgets.UpdatesHelper;
import com.elementary.tasks.core.cloud.Google;
import com.elementary.tasks.core.controller.EventControlFactory;
import com.elementary.tasks.core.utils.Constants;
import com.elementary.tasks.core.utils.Dialogues;
import com.elementary.tasks.core.utils.LogUtil;
import com.elementary.tasks.core.utils.Module;
import com.elementary.tasks.core.utils.RealmDb;
import com.elementary.tasks.core.utils.TimeUtil;
import com.elementary.tasks.core.views.roboto.RoboEditText;
import com.elementary.tasks.core.views.roboto.RoboTextView;
import com.elementary.tasks.databinding.ActivityCreateGoogleTaskBinding;
import com.elementary.tasks.groups.GroupItem;
import com.elementary.tasks.reminder.models.Reminder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Copyright 2016 Nazar Suhovich
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class TaskActivity extends ThemedActivity {

    private static final String TAG = "TaskActivity";

    private ActivityCreateGoogleTaskBinding binding;
    private RoboEditText editField, noteField;
    private RoboTextView dateField;
    private RoboTextView timeField;
    private RoboTextView listText;

    private int mHour = 0;
    private int mMinute = 0;
    private int mYear = 0;
    private int mMonth = 0;
    private int mDay = 1;
    private String listId = null;
    private String action;
    private boolean isReminder = false;
    private boolean isDate = false;

    @Nullable
    private TaskItem mItem;
    @Nullable
    private ProgressDialog mDialog;

    private static final int MENU_ITEM_DELETE = 12;
    private static final int MENU_ITEM_MOVE = 14;
    @NonNull
    private TasksCallback mSimpleCallback = new TasksCallback() {
        @Override
        public void onFailed() {
            hideDialog();
        }

        @Override
        public void onComplete() {
            hideDialog();
            finish();
        }
    };

    private void hideDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            try {
                mDialog.dismiss();
            } catch (IllegalArgumentException e) {
                LogUtil.d(TAG, "hideDialog: " + e.getLocalizedMessage());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_google_task);
        initToolbar();
        initFields();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH);
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
        Intent intent = getIntent();
        String tmp = intent.getStringExtra(Constants.INTENT_ID);
        action = intent.getStringExtra(TasksConstants.INTENT_ACTION);
        if (action == null) action = TasksConstants.CREATE;
        if (action.matches(TasksConstants.CREATE)) {
            initNewTask(tmp);
        } else {
            initTaskEdit(tmp);
        }
        switchDate();
    }

    private void initTaskEdit(String id) {
        binding.toolbar.setTitle(R.string.edit_task);
        mItem = RealmDb.getInstance().getTask(id);
        if (mItem != null) {
            editField.setText(mItem.getTitle());
            listId = mItem.getListId();
            String note = mItem.getNotes();
            if (note != null) {
                noteField.setText(note);
                noteField.setSelection(noteField.getText().length());
            }
            long time = mItem.getDueDate();
            if (time != 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(time);
                mHour = calendar.get(Calendar.HOUR_OF_DAY);
                mMinute = calendar.get(Calendar.MINUTE);
                mYear = calendar.get(Calendar.YEAR);
                mMonth = calendar.get(Calendar.MONTH);
                mDay = calendar.get(Calendar.DAY_OF_MONTH);
                isDate = true;
                dateField.setText(TimeUtil.getDate(calendar.getTime()));
            }
            TaskListItem listItem = RealmDb.getInstance().getTaskList(mItem.getListId());
            if (listItem != null) {
                listText.setText(listItem.getTitle());
                setColor(listItem.getColor());
            }
            showReminder();
        }
    }

    private void showReminder() {
        if (mItem != null) {
            Reminder item = RealmDb.getInstance().getReminder(mItem.getUuId());
            if (item != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(TimeUtil.getDateTimeFromGmt(item.getEventTime()));
                timeField.setText(TimeUtil.getTime(calendar.getTime(), getPrefs().is24HourFormatEnabled()));
                isReminder = true;
            }
        }
    }

    private void initNewTask(String id) {
        binding.toolbar.setTitle(R.string.new_task);
        if (id == null) {
            TaskListItem listItem = RealmDb.getInstance().getDefaultTaskList();
            if (listItem != null) {
                listId = listItem.getListId();
                listText.setText(listItem.getTitle());
                setColor(listItem.getColor());
            }
        } else {
            TaskListItem listItem = RealmDb.getInstance().getTaskList(id);
            if (listItem != null) {
                listId = listItem.getListId();
                listText.setText(listItem.getTitle());
                setColor(listItem.getColor());
            }
        }
    }

    private void initFields() {
        editField = binding.editField;
        noteField = binding.noteField;
        listText = binding.listText;
        listText.setOnClickListener(v -> selectList(false));
        dateField = binding.dateField;
        dateField.setOnClickListener(v -> selectDateAction(1));
        timeField = binding.timeField;
        timeField.setOnClickListener(v -> selectDateAction(2));
    }

    private void initToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void selectDateAction(final int type) {
        AlertDialog.Builder builder = Dialogues.getDialog(this);
        String[] types = new String[]{getString(R.string.no_date), getString(R.string.select_date)};
        if (type == 2) {
            types = new String[]{getString(R.string.no_reminder), getString(R.string.select_time)};
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_single_choice, types);
        int selection = 0;
        if (type == 1) {
            if (isDate) selection = 1;
            else selection = 0;
        }
        if (type == 2) {
            if (isReminder) selection = 1;
            else selection = 0;
        }
        builder.setSingleChoiceItems(adapter, selection, (dialog, which) -> {
            if (which != -1) {
                dialog.dismiss();
                if (type == 1) {
                    switch (which) {
                        case 0:
                            isDate = false;
                            switchDate();
                            break;
                        case 1:
                            isDate = true;
                            dateDialog();
                            break;
                    }
                }
                if (type == 2) {
                    switch (which) {
                        case 0:
                            isReminder = false;
                            switchDate();
                            break;
                        case 1:
                            isReminder = true;
                            timeDialog();
                            break;
                    }
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void switchDate() {
        if (!isDate) dateField.setText(getString(R.string.no_date));
        if (!isReminder) timeField.setText(getString(R.string.no_reminder));
    }

    private void moveTask(String listId) {
        if (mItem != null) {
            String initListId = mItem.getListId();
            if (!listId.matches(initListId)) {
                mItem.setListId(listId);
                showProgressDialog(getString(R.string.moving_task));
                new TaskAsync(TaskActivity.this, TasksConstants.MOVE_TASK, initListId, mItem, new TasksCallback() {
                    @Override
                    public void onFailed() {
                        hideDialog();
                    }

                    @Override
                    public void onComplete() {
                        RealmDb.getInstance().saveObject(mItem);
                        hideDialog();
                        finish();
                    }
                }).execute();
            } else {
                Toast.makeText(this, getString(R.string.this_is_same_list), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showProgressDialog(String title) {
        mDialog = ProgressDialog.show(this, null, title, true, false);
    }

    private void selectList(final boolean move) {
        List<TaskListItem> list = RealmDb.getInstance().getTaskLists();
        List<String> names = new ArrayList<>();
        int position = 0;
        for (int i = 0; i < list.size(); i++) {
            TaskListItem item = list.get(i);
            names.add(item.getTitle());
            if (listId != null && item.getListId() != null && item.getListId().matches(listId)) {
                position = i;
            }
        }
        AlertDialog.Builder builder = Dialogues.getDialog(this);
        builder.setTitle(R.string.choose_list);
        builder.setSingleChoiceItems(new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, names),
                position, (dialog, which) -> {
                    dialog.dismiss();
                    if (move) moveTask(list.get(which).getListId());
                    else {
                        listId = list.get(which).getListId();
                        listText.setText(list.get(which).getTitle());
                        reloadColor(list.get(which).getListId());
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void reloadColor(String listId) {
        TaskListItem item = RealmDb.getInstance().getTaskList(listId);
        if (item != null) {
            setColor(item.getColor());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mItem != null && getPrefs().isAutoSaveEnabled()) {
            saveTask();
        }
    }

    private void saveTask() {
        String taskName = editField.getText().toString().trim();
        if (taskName.matches("")) {
            editField.setError(getString(R.string.must_be_not_empty));
            return;
        }
        String note = noteField.getText().toString().trim();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(mYear, mMonth, mDay, 12, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long due = 0;
        if (isDate) due = calendar.getTimeInMillis();
        String uuId = null;
        if (isReminder) uuId = saveReminder(taskName);
        if (action.matches(TasksConstants.EDIT) && mItem != null) {
            String initListId = mItem.getListId();
            mItem.setListId(listId);
            mItem.setStatus(Google.TASKS_NEED_ACTION);
            mItem.setTitle(taskName);
            mItem.setNotes(note);
            mItem.setUuId(uuId);
            mItem.setDueDate(due);
            if (listId != null) {
                showProgressDialog(getString(R.string.saving));
                RealmDb.getInstance().saveObject(mItem);
                new TaskAsync(TaskActivity.this, TasksConstants.UPDATE_TASK, null, mItem, new TasksCallback() {
                    @Override
                    public void onFailed() {
                        hideDialog();
                    }

                    @Override
                    public void onComplete() {
                        if (!listId.matches(initListId)) {
                            new TaskAsync(TaskActivity.this, TasksConstants.MOVE_TASK, initListId, mItem, mSimpleCallback).execute();
                        } else {
                            hideDialog();
                        }
                    }
                }).execute();
            } else {
                showProgressDialog(getString(R.string.saving));
                RealmDb.getInstance().saveObject(mItem);
                new TaskAsync(TaskActivity.this, TasksConstants.UPDATE_TASK, null, mItem, mSimpleCallback).execute();
            }
        } else {
            mItem = new TaskItem();
            mItem.setListId(listId);
            mItem.setStatus(Google.TASKS_NEED_ACTION);
            mItem.setTitle(taskName);
            mItem.setNotes(note);
            mItem.setDueDate(due);
            mItem.setUuId(uuId);
            showProgressDialog(getString(R.string.saving));
            new TaskAsync(TaskActivity.this, TasksConstants.INSERT_TASK, null, mItem, mSimpleCallback).execute();
        }
    }

    private String saveReminder(String task) {
        GroupItem group = RealmDb.getInstance().getDefaultGroup();
        String groupId = "";
        if (group != null) {
            groupId = group.getUuId();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(mYear, mMonth, mDay, mHour, mMinute);
        long due = calendar.getTimeInMillis();
        Reminder reminder = new Reminder();
        reminder.setType(Reminder.BY_DATE);
        reminder.setSummary(task);
        reminder.setGroupUuId(groupId);
        reminder.setStartTime(TimeUtil.getGmtFromDateTime(due));
        reminder.setEventTime(TimeUtil.getGmtFromDateTime(due));
        RealmDb.getInstance().saveReminder(reminder, () -> EventControlFactory.getController(TaskActivity.this, reminder).start());
        return reminder.getUuId();
    }

    private void deleteDialog() {
        AlertDialog.Builder builder = Dialogues.getDialog(this);
        builder.setMessage(getString(R.string.delete_this_task));
        builder.setPositiveButton(getString(R.string.yes), (dialog, which) -> {
            dialog.dismiss();
            deleteTask();
        });
        builder.setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteTask() {
        if (mItem != null) {
            showProgressDialog(getString(R.string.deleting_task));
            new TaskAsync(TaskActivity.this, TasksConstants.DELETE_TASK, null, mItem, new TasksCallback() {
                @Override
                public void onFailed() {
                    hideDialog();
                }

                @Override
                public void onComplete() {
                    RealmDb.getInstance().deleteTask(mItem);
                    hideDialog();
                    finish();
                }
            }).execute();
        }
    }

    private void setColor(int i) {
        binding.appBar.setBackgroundColor(getThemeUtil().getNoteColor(i));
        if (Module.isLollipop()) {
            getWindow().setStatusBarColor(getThemeUtil().getNoteDarkColor(i));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_create_task, menu);
        if (mItem != null) {
            menu.add(Menu.NONE, MENU_ITEM_DELETE, 100, R.string.delete_task);
            menu.add(Menu.NONE, MENU_ITEM_MOVE, 100, R.string.move_to_another_list);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE:
                deleteDialog();
                return true;
            case MENU_ITEM_MOVE:
                selectList(true);
                return true;
            case R.id.action_add:
                saveTask();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void dateDialog() {
        TimeUtil.showDatePicker(this, myDateCallBack, mYear, mMonth, mDay);
    }

    DatePickerDialog.OnDateSetListener myDateCallBack = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(year, monthOfYear, dayOfMonth);
            dateField.setText(TimeUtil.getDate(calendar.getTime()));
        }
    };

    protected void timeDialog() {
        TimeUtil.showTimePicker(this, myCallBack, mHour, mMinute);
    }

    TimePickerDialog.OnTimeSetListener myCallBack = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mHour = hourOfDay;
            mMinute = minute;
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            timeField.setText(TimeUtil.getTime(c.getTime(), getPrefs().is24HourFormatEnabled()));
        }
    };

    @Override
    protected void onDestroy() {
        UpdatesHelper.getInstance(this).updateTasksWidget();
        super.onDestroy();
    }
}
