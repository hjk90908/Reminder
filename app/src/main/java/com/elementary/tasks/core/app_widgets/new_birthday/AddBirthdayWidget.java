package com.elementary.tasks.core.app_widgets.new_birthday;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.elementary.tasks.R;
import com.elementary.tasks.birthdays.AddBirthdayActivity;
import com.elementary.tasks.core.app_widgets.WidgetUtils;

/**
 * Copyright 2015 Nazar Suhovich
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

public class AddBirthdayWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        SharedPreferences sp = context.getSharedPreferences(
                AddBirthdayWidgetConfig.ADD_BIRTHDAY_WIDGET_PREF, Context.MODE_PRIVATE);
        for (int i : appWidgetIds) {
            updateWidget(context, appWidgetManager, sp, i);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager,
                                    SharedPreferences sp, int widgetID) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.add_birthday_widget_layout);
        int widgetColor = sp.getInt(AddBirthdayWidgetConfig.ADD_BIRTHDAY_WIDGET_COLOR + widgetID, 0);
        rv.setInt(R.id.widgetBg, "setBackgroundResource", widgetColor);
        Intent configIntent = new Intent(context, AddBirthdayActivity.class);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);
        rv.setOnClickPendingIntent(R.id.imageView, configPendingIntent);
        WidgetUtils.setIcon(context, rv, R.drawable.ic_cake_white_24dp, R.id.imageView);
        appWidgetManager.updateAppWidget(widgetID, rv);
    }
}
