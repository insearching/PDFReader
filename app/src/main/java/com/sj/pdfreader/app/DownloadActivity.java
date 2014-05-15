package com.sj.pdfreader.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.webkit.URLUtil;

import net.sf.andpdf.pdfviewer.PdfViewerActivity;


public class DownloadActivity extends ActionBarActivity implements DownloadService.DownloadListener {

    private static final String url = "http://fs40.www.ex.ua/get/10c6aa7afe390ffd39c2db21071c9d75/93706495/Java.pdf";

    private String path = null;

    private DownloadService mService;

    public final static String REQUEST_URL = "request_url";
    public final static String PATH = "path";

    public final static String TAG = "PDFViewer";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String fileName = URLUtil.guessFileName(url, null, null);
        path = Environment.getExternalStorageDirectory().getPath() + "/" + getString(R.string.app_name)+"/" + fileName;

        downloadFile(url, path);

    }

    @Override
    public void onDownloadCompleted(String url, String path) {
        Intent intent = new Intent(DownloadActivity.this, PdfReaderActivity.class);
        intent.putExtra(PdfViewerActivity.EXTRA_PDFFILENAME, path);
        startActivity(intent);
    }

    public void downloadFile(String requestUrl, String path) {
        ServiceConnection mConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                mService = ((DownloadService.FileDownloadBinder) binder).getService();
                mService.attachListener(DownloadActivity.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent intent = new Intent(this, DownloadService.class)
                .putExtra(REQUEST_URL, requestUrl)
                .putExtra(PATH, path);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        startService(intent);
    }
}
