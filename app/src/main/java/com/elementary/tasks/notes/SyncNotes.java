package com.elementary.tasks.notes;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import com.elementary.tasks.R;
import com.elementary.tasks.core.cloud.Dropbox;
import com.elementary.tasks.core.cloud.GoogleDrive;
import com.elementary.tasks.core.utils.BackupTool;
import com.elementary.tasks.core.utils.Module;
import com.elementary.tasks.core.utils.SuperUtil;

import java.io.IOException;

/**
 * Copyright 2017 Nazar Suhovich
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

public class SyncNotes extends AsyncTask<Void, Void, Boolean> {

    private Context mContext;
    private NotificationManagerCompat mNotifyMgr;
    private NotificationCompat.Builder builder;
    private SyncListener mListener;

    public SyncNotes(Context context, SyncListener mListener){
        this.mContext = context;
        builder = new NotificationCompat.Builder(context);
        this.mListener = mListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        builder.setContentTitle(mContext.getString(R.string.notes));
        builder.setContentText(mContext.getString(R.string.syncing_notes));
        builder.setSmallIcon(R.drawable.ic_cached_white_24dp);
        mNotifyMgr = NotificationManagerCompat.from(mContext);
        mNotifyMgr.notify(2, builder.build());
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            BackupTool.getInstance().importNotes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BackupTool.getInstance().exportNotes();
        if (SuperUtil.isConnected(mContext)) {
            new Dropbox(mContext).downloadNotes();
            new Dropbox(mContext).uploadNotes();
            try {
                new GoogleDrive(mContext).downloadNotes(false);
                new GoogleDrive(mContext).saveNotesToDrive();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean aVoid) {
        super.onPostExecute(aVoid);
        builder.setContentTitle(mContext.getString(R.string.done));
        builder.setSmallIcon(R.drawable.ic_done_white_24dp);
        if (Module.isPro()){
            builder.setContentText(mContext.getString(R.string.app_name_pro));
        } else builder.setContentText(mContext.getString(R.string.app_name));
        builder.setWhen(System.currentTimeMillis());
        mNotifyMgr.notify(2, builder.build());
        mListener.endExecution(aVoid);
//        UpdatesHelper.getInstance(mContext).updateNotesWidget();
    }

    public interface SyncListener {
        void endExecution(boolean b);
    }
}