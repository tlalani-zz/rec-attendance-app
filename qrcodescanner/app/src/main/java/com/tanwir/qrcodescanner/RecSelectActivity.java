package com.tanwir.qrcodescanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RecSelectActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private HashMap<String, ArrayList<String>> centers;
    private ArrayList<String> shifts;
    private Spinner center_select;
    private Spinner class_select;
    private Spinner shift_select;
    private String re_center;
    private String re_class;
    private String re_shift;
    public static final String TAG = "RecSelectActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rec_select);
        shift_select = findViewById(R.id.shift_spinner);
        shift_select.setEnabled(false);
        class_select = findViewById(R.id.class_spinner);
        class_select.setEnabled(false);
        center_select = findViewById(R.id.location_spinner);
        centers = new HashMap<>();
        shifts = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        getCenters();
    }

    public void getCenters() {
        DatabaseReference ref = mRootRef.child("users")
                .child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .child("permissions");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot center : dataSnapshot.getChildren()) {
                    centers.put(center.getKey(), new ArrayList<String>());
                    for(DataSnapshot re_class : center.getChildren()) {
                        centers.get(center.getKey()).add(re_class.getKey());
                    }
                }
                String[] a = new String[centers.size()];
                a = centers.keySet().toArray(a);
                String[] b = {"Select a Center"};
                a = ArrayUtils.concat(b, a);
                ArrayAdapter adapter = new ArrayAdapter<>(RecSelectActivity.this, android.R.layout.simple_spinner_item, a);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                center_select.setAdapter(adapter);
                center_select.setOnItemSelectedListener(RecSelectActivity.this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(RecSelectActivity.this, "There was an error or you do not have permission. Please retry.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void getClasses() {
        String[] a = createAdapter(centers.get(re_center), "Select a Class");
        if(a == null) {
            Toast.makeText(this, "Error getting Classes, Either restart, or re-login.", Toast.LENGTH_LONG).show();
            return;
        }
        ArrayAdapter adapter = new ArrayAdapter<>(RecSelectActivity.this, android.R.layout.simple_spinner_item, a);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        class_select.setAdapter(adapter);
        class_select.setOnItemSelectedListener(this);
        class_select.setEnabled(true);
    }


    public void getShifts() {
        DatabaseReference ref = mRootRef.child("REC").child(re_center).child(re_class).child("Shifts");
        shifts.clear();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot day : dataSnapshot.getChildren()) {
                    for(DataSnapshot time: day.getChildren()) {
                        shifts.add(day.getKey() + ", " + time.getKey());
                    }
                }
                String[] a = createAdapter(shifts, "Select a Shift");
                if(a == null) {
                    Toast.makeText(RecSelectActivity.this, "Error getting Shifts, Either restart, or re-login.", Toast.LENGTH_LONG).show();
                    return;
                }
                ArrayAdapter adapter = new ArrayAdapter<>(RecSelectActivity.this, android.R.layout.simple_spinner_item, a);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                shift_select.setAdapter(adapter);
                shift_select.setOnItemSelectedListener(RecSelectActivity.this);
                shift_select.setEnabled(true);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(RecSelectActivity.this, "There was an error or you do not have permission. Please retry.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.location_spinner:
                if(position != 0) {
                    re_center = parent.getItemAtPosition(position).toString();
                    getClasses();
                }
                break;
            case R.id.class_spinner:
                if(position != 0) {
                    re_class = parent.getItemAtPosition(position).toString();
                    getShifts();
                }
                break;
            case R.id.shift_spinner:
                if(position != 0) {
                    re_shift = parent.getItemAtPosition(position).toString();
                }
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void goToApp(View v) {
        if(re_center != null && re_class != null && re_shift != null) {
            Intent i = new Intent(RecSelectActivity.this, ScanActivity.class);
            i.putExtra("center", re_center).putExtra("class", re_class).putExtra("shift", re_shift);
            startActivity(i);
        } else {
            String displayMessage = "RE Center: "+re_center + "\nRE Class: "+re_class+"\nRE_SHIFT: "+re_shift;
            AlertDialog d = createAlertDialogWithTitleAndMessage("Unable to Start Scan", "");
            d.setMessage("You have not selected one of these\n"+displayMessage);
            d.show();
        }
    }

    public String[] createAdapter(ArrayList<String> list, String initString) {
        if(list.size() > 0) {
            String[] a = new String[list.size()];
            a = list.toArray(a);
            String[] b = {initString};
            String[] c = ArrayUtils.concat(b, a);
            return c;
        }
        return null;
    }


    public AlertDialog createAlertDialogWithTitleAndMessage(String title, String message) {
        android.app.AlertDialog.Builder alertdialog = new android.app.AlertDialog.Builder(this);
        alertdialog
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true);
        android.app.AlertDialog d = alertdialog.create();
        return d;

    }
}
