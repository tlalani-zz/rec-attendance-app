package com.tanwir.qrcodescanner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class TardyActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private ArrayAdapter<CharSequence> reasons;
    private String[] options;
    private Spinner tardyReason;
    private TextView comments;
    private String SELECT_TEXT;
    private static final int REQUEST_TARDY_INFORMATION = 102;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tardy);
        options = new String[2];
        tardyReason = (Spinner) findViewById(R.id.tardyReason);
        comments = (TextView) findViewById(R.id.comments);
        reasons = ArrayAdapter.createFromResource(this, R.array.reasons, R.layout.spinner_item_2);
        tardyReason.setAdapter(reasons);
        tardyReason.setOnItemSelectedListener(this);
        SELECT_TEXT = reasons.getItem(0).toString();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (!parent.getSelectedItem().toString().equalsIgnoreCase(SELECT_TEXT)) {
            options[0] = parent.getSelectedItem().toString();
            Log.d("OPTIONS", "onItemSelected: options"+options[0]);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        options = new String[2];
    }

    public void alertDialogCreator(String t, String m) {
        android.app.AlertDialog.Builder alertdialog = new android.app.AlertDialog.Builder(this);
        alertdialog
                .setTitle(t)
                .setMessage(m)
                .setCancelable(true);
        android.app.AlertDialog d = alertdialog.create();
        d.show();
    }

    public void sendTardyData(View v) {
        if (options[0] == null || options[0].isEmpty()) {
            alertDialogCreator("Please Enter Reason", "Please enter a tardy reason before sending to database.");
        } else {
            options[1] = comments.getText() != null ? comments.getText().toString().replace("\n", "") : null;
            Intent i = new Intent();
            i.putExtra("tardyInfo", options);
            setResult(RESULT_OK, i);
            finish();
        }
    }

}
