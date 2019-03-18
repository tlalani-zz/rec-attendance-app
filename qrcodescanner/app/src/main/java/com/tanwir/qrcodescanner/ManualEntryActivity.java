package com.tanwir.qrcodescanner;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class ManualEntryActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Spinner sp1;
    private Spinner sp2;
    private Spinner sp4;
    private EditText et;
    private TextView time;
    private TextView dates;
    private EditText reason;
    private String[] roles = {"Select A Role", "Student", "Management", "Teacher", "Support"};
    private String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private String role;
    private String grade = null;
    private Date date;
    private String inputDate;
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference roleRef;
    DatabaseReference dateRef;
    DatabaseReference studentRef;
    DatabaseReference gradeRef;
    DatabaseReference mgmtRef;
    DatabaseReference supportRef;
    DatabaseReference teacherRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_entry);
        sp1 = findViewById(R.id.roleSelect);
        sp2 = findViewById(R.id.gradeSelect);
        sp4 = findViewById(R.id.spinner4);
        et = findViewById(R.id.name);
        time = findViewById(R.id.time);
        dates = findViewById(R.id.date);
        reason = findViewById(R.id.comments);
        ArrayAdapter<CharSequence> roles = ArrayAdapter.createFromResource(this, R.array.roles, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> grades = ArrayAdapter.createFromResource(this, R.array.grades, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> reasons = ArrayAdapter.createFromResource(this, R.array.reasons, android.R.layout.simple_spinner_dropdown_item);
        sp1.setAdapter(roles);
        sp2.setAdapter(grades);
        sp4.setAdapter(reasons);
        sp2.setVisibility(View.INVISIBLE);
        sp1.setOnItemSelectedListener(this);
        sp2.setOnItemSelectedListener(this);
        sp4.setOnItemSelectedListener(this);
        dates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //To show current date in the datepicker
                Calendar mcurrentDate = Calendar.getInstance();
                int mYear = mcurrentDate.get(Calendar.YEAR);
                int mMonth = mcurrentDate.get(Calendar.MONTH);
                int mDay = mcurrentDate.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog mDatePicker =
                        new DatePickerDialog(ManualEntryActivity.this, new DatePickerDialog.OnDateSetListener() {
                            public void onDateSet(DatePicker datepicker, int selectedyear, int selectedmonth, int selectedday) {
                                dates.setText(months[selectedmonth] + " "+ selectedday+", "+selectedyear);
                            }
                        }, mYear, mMonth, mDay);
                mDatePicker.setTitle("Select Date");
                mDatePicker.show();
            }
        });
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(ManualEntryActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        String AMPM = "";
                        if(selectedHour > 12) {
                            selectedHour = selectedHour - 12;
                            AMPM = "PM";
                        } else if(selectedHour == 0) {
                            selectedHour = 12;
                            AMPM = "AM";
                        } else {
                            AMPM = "AM";
                        }
                        time.setText(selectedHour + ":" + selectedMinute + " "+AMPM);
                    }
                }, hour, minute, false);//Yes 24 hour time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();

            }
        });
    }

    public void doEntry(View v) throws Exception {
        String currentTime;
        date = new Date();
        inputDate = DateFormat.getDateInstance(DateFormat.LONG).format(date);
        if(dates.getText() != null) {
            if(!dates.getText().toString().isEmpty()) {
                inputDate = dates.getText().toString();
            }
        }
        if (time.getText() != null) {
            if (!time.getText().toString().isEmpty()) {
                currentTime = time.getText().toString();
            } else
                currentTime = DateFormat.getTimeInstance().format(date);
        } else
            currentTime = DateFormat.getTimeInstance().format(date);
        if (role != null) {
            if (et.getText() != null) {
                if (!et.getText().toString().isEmpty()) {
                    if (role.equals("Student")) {
                        if (grade != null) {
                            studentRef = mRootRef.child(inputDate).child(role).child(grade).child(et.getText().toString());
                            studentRef.child("Time").setValue(currentTime);
                            if (reason.getText() != null) {
                                if (!sp4.getSelectedItem().toString().equalsIgnoreCase("Select a Reason")) {
                                    studentRef.child("Reason").setValue(sp4.getSelectedItem().toString());
                                }
                                if(reason.getText() != null && !reason.getText().toString().isEmpty())
                                    studentRef.child("Comments").setValue(reason.getText().toString());
                            }
                            studentRef = null;
                            et.setText("");
                            time.setText("");
                            reason.setText("");
                            Toast.makeText(this, "Sent Successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Please Select A Valid Grade", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        mgmtRef = mRootRef.child(inputDate).child(role).child(et.getText().toString());
                        mgmtRef.child("Time").setValue(currentTime);
                        if (reason.getText() != null) {
                            if (!reason.getText().toString().isEmpty()) {
                                mgmtRef.child("Reason").setValue(reason.getText().toString());
                            }
                        }
                        mgmtRef = null;
                        et.setText("");
                        time.setText("");
                        reason.setText("");
                        Toast.makeText(this, "Sent Successfully", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Please Enter a Name", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please Enter a Name", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please Select A Valid Role", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        for (String s : roles) {
            if (!parent.getItemAtPosition(position).toString().equals(roles[0])) {
                if (parent.getItemAtPosition(position).toString().equals(s)) {
                    grade = null;
                    role = s;
                    if (s.equals("Student")) {
                        sp2.setVisibility(View.VISIBLE);
                        return;
                    }
                    sp2.setVisibility(View.INVISIBLE);
                    return;
                }
            } else {
                sp2.setVisibility(View.INVISIBLE);
                Toast.makeText(this, "Please Select A Valid Role", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (sp2.getVisibility() == View.VISIBLE && parent.getItemAtPosition(position).toString().equals("Select A Grade")) {
            Toast.makeText(this, "Please Select a Valid Grade", Toast.LENGTH_SHORT).show();
        } else {
            grade = parent.getItemAtPosition(position).toString();
        }
        if (sp4.getSelectedItem().toString().equalsIgnoreCase("Select a Reason")) {
            Toast.makeText(this, "Please Select a Valid Reason", Toast.LENGTH_LONG).show();
        } else {
            reason.setText(parent.getItemAtPosition(position).toString());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
