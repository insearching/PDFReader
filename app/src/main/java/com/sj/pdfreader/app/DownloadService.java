package com.sj.pdfreader.app;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends Service {
    //Application info on service
    private static final String TAG = "DOWNLOAD SERVICE";
    private static final String EXT_STORAGE_PATH = Environment.getExternalStorageDirectory().getPath() + "/PDFReader";
    private Context mContext = this;
    private DownloadListener downloadListener;
    private Integer mProgress = 0;

    IBinder mBinder = new FileDownloadBinder();

    public class FileDownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        android.os.Debug.waitForDebugger();
        Log.d(TAG, "CREATED");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        String requestUrl = intent.getStringExtra("request_url");

        DownloadFile dd = new DownloadFile(startId);
        dd.execute(requestUrl);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DESTORY");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    /**
     * Downloads file from service
     * param[0] - request URL
     */
    class DownloadFile extends AsyncTask<String, Integer, String> {
        int startId;
        String requestUrl = null;

        public DownloadFile(int startId) {
            this.startId = startId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress = 0;
            if (downloadListener != null)
                downloadListener.onDownloadStarted(requestUrl);

        }

        @Override
        protected String doInBackground(String... param) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(param[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream("/sdcard/file.pdf");

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
                showDownloadNotification(requestUrl, "Downloading", mProgress);
                if (downloadListener != null) {
                    downloadListener.onProgressChanged(requestUrl);
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.i(TAG, String.valueOf(result));
            if (result == null) {
                showNotification(requestUrl, getString(R.string.downloaded));
                Toast.makeText(mContext, getString(R.string.download_completed), Toast.LENGTH_LONG).show();
                if (downloadListener != null)
                    downloadListener.onDownloadCompleted(requestUrl);
            } else {
                Toast.makeText(mContext, getString(R.string.download_failed), Toast.LENGTH_LONG).show();
            }
            stopSelf(startId);
        }
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
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
        File file = new File(EXT_STORAGE_PATH + "/" + title);
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString().toLowerCase());
        String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        notificationIntent.setDataAndType(Uri.fromFile(file), mimetype);

        PendingIntent contentIntent = PendingIntent.getActivity(mContext,
                1, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationManager nm = (NotificationManager)
                getApplicationContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(text)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setSmallIcon(R.drawable.ic_downloading);
        nm.notify(0, builder.build());
    }

    private File createFile(String fileName) {
        String sFolder = EXT_STORAGE_PATH + "/";
        File file = new File(sFolder);
        if (!file.exists())
            file.mkdirs();

        file = null;
        try {
            // Create file or re-download if needest
            file = new File(sFolder + fileName);

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

    @SuppressLint("InlinedApi")
    private static void saveFileData(Context context, String fileName, String fileIdent) {
        SharedPreferences downloadPrefs = context.getSharedPreferences("downloaded_files", Context.MODE_MULTI_PROCESS);
        Editor edit = downloadPrefs.edit();
        edit.putString(fileIdent, fileName);
        edit.commit();
    }

    public void attachListener(Context context) {
        downloadListener = (DownloadListener) context;
    }

    public interface DownloadListener {

        public void onDownloadStarted(String url);

        public void onProgressChanged(String url);

        public void onDownloadCompleted(String url);

    }
}
