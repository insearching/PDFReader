package com.sj.pdfreader.app;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.URLUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends Service {

    private static final String TAG = "PDFViewer";

    private DownloadListener downloadListener;
    private DownloadFileTask task;
    private Integer mProgress = 0;

    private IBinder mBinder = new FileDownloadBinder();

    public class FileDownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String requestUrl = intent.getStringExtra(DownloadActivity.REQUEST_URL);
        String path = intent.getStringExtra(DownloadActivity.PATH);

        task = new DownloadFileTask(startId, requestUrl, path);
        task.execute();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        task.cancel(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Downloads file from web by given direct url
     */
    class DownloadFileTask extends AsyncTask<String, Integer, String> {
        int startId;
        String requestUrl;
        String path;

        public DownloadFileTask(int startId, String requestUrl, String path) {
            this.startId = startId;
            this.requestUrl = requestUrl;
            this.path = path;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress = 0;
        }

        @Override
        protected String doInBackground(String... param) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(requestUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                File file = createFile(path);
                output = new FileOutputStream(file);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));

                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            if ((progress[0] % 5) == 0 && mProgress != progress[0]) {
                mProgress = progress[0];
                showDownloadNotification(requestUrl, getString(R.string.downloading), mProgress);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
                String fileName = URLUtil.guessFileName(requestUrl, null, null);
                showNotification(getString(R.string.app_name), fileName + " " + getString(R.string.downloaded));
                if (downloadListener != null)
                    downloadListener.onDownloadCompleted(requestUrl, path);
            } else {
                Log.e(TAG, result);
            }
            stopSelf(startId);
        }
    }

    private File createFile(String path) {
        File file = new File(path);
        if(!file.exists())
            file.mkdirs();

        file = null;
        try {
            // Create file or re-download if needest
            file = new File(path);

            if (!file.createNewFile()) {
                file.delete();
                if (!file.createNewFile()) {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return file;
    }

    private void showDownloadNotification(String title, String text, int progress) {
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        int smallIcon = R.drawable.ic_downloading;

        NotificationManager nm = (NotificationManager)
                getApplicationContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(text)
                .setLargeIcon(largeIcon)
                .setSmallIcon(smallIcon)
                .setProgress(100, progress, false);
        nm.notify(0, builder.build());
    }

    /**
     * Show a notification when file is downloaded
     */
    private void showNotification(String title, String text) {
        NotificationManager manager = (NotificationManager)
                getApplicationContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(text)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setSmallIcon(R.drawable.ic_downloading);
        manager.notify(0, builder.build());
    }

    public void attachListener(Context context) {
        downloadListener = (DownloadListener) context;
    }

    public interface DownloadListener {
        public void onDownloadCompleted(String url, String path);
    }
}
