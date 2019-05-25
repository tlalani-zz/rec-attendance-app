package com.tanwir.qrcodescanner;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import androidx.appcompat.app.AppCompatActivity;
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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class ManualEntryActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Spinner roleDropdown;
    private Spinner gradeDropdown;
    private Spinner reasonDropdown;
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
    private Person p = new Person();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_entry);
        roleDropdown = findViewById(R.id.roleSelect);
        gradeDropdown = findViewById(R.id.gradeSelect);
        reasonDropdown = findViewById(R.id.reasonSelect);
        et = findViewById(R.id.name);
        time = findViewById(R.id.time);
        dates = findViewById(R.id.date);
        reason = findViewById(R.id.comments);
        ArrayAdapter<CharSequence> roles = ArrayAdapter.createFromResource(this, R.array.roles, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> grades = ArrayAdapter.createFromResource(this, R.array.grades, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> reasons = ArrayAdapter.createFromResource(this, R.array.reasons, android.R.layout.simple_spinner_dropdown_item);
        roleDropdown.setAdapter(roles);
        gradeDropdown.setAdapter(grades);
        reasonDropdown.setAdapter(reasons);
        roleDropdown.setOnItemSelectedListener(this);
        gradeDropdown.setOnItemSelectedListener(this);
        reasonDropdown.setOnItemSelectedListener(this);
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

    public void doEntry(View v) {
        v.setBackground(getDrawable(R.drawable.balaj));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(position != 0) {
            if (parent.getId() == reasonDropdown.getId()) {
                p.setReason(reasonDropdown.getItemAtPosition(position).toString());
            } else if (parent.getId() == gradeDropdown.getId()) {
                p.setGrade(gradeDropdown.getItemAtPosition(position).toString());
            } else if (parent.getId() == roleDropdown.getId()) {
                p.setRole(roleDropdown.getItemAtPosition(position).toString());
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
