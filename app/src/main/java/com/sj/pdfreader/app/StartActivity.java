package com.sj.pdfreader.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import net.sf.andpdf.pdfviewer.PdfViewerActivity;


public class StartActivity extends ActionBarActivity implements DownloadService.DownloadListener{

    private String url = "http://fs40.www.ex.ua/get/10c6aa7afe390ffd39c2db21071c9d75/93706495/Java.pdf";

    private String path = "/mnt/sdcard/Download/Java.pdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Button b = (Button)findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                intent.putExtra(PdfViewerActivity.EXTRA_PDFFILENAME, path);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDownloadStarted(String url) {
        Toast.makeText(this, "Starting to download " + url, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProgressChanged(String url) {

    }

    @Override
    public void onDownloadCompleted(String url) {
        Toast.makeText(this, "File " + url + " downloaded.", Toast.LENGTH_LONG).show();
    }
}
