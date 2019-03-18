package com.tanwir.qrcodescanner;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class ScanActivity extends AppCompatActivity  implements AdapterView.OnItemSelectedListener {
    private static final int REQUEST_CODE_QR_SCAN = 101;
    private static final String SCAN = "SCAN";
    private static final String SEND = "SEND";
    private TextView tardyText;
    private EditText tardyComments;
    private Button scanButton;
    private String todayDate;
    private String schoolYear;
    private Person personScanned;
    private Calendar calendar;
    private Spinner tardyReason;
    private ArrayAdapter<CharSequence> reasons;
    private boolean userIsInteracting;
    private HashMap<String, ArrayList<Person>> allPeople;
    private HashSet<Person> savedPeople = new HashSet<>();

    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference personRef;
    DatabaseReference dateRef;
    DatabaseReference studentRef;
    DatabaseReference gradeRef;
    DatabaseReference mgmtRef;
    DatabaseReference supportRef;
    DatabaseReference teacherRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        calendar = GregorianCalendar.getInstance();
        calendar.setTime(new Date());
        userIsInteracting = false;
        tardyText = findViewById(R.id.tardyText);
        tardyComments = findViewById(R.id.tardyComments);
        scanButton = findViewById(R.id.btn_scan);
        tardyText.setVisibility(View.INVISIBLE);
        tardyComments.setVisibility(View.INVISIBLE);
        tardyReason = findViewById(R.id.tardyReason);
        reasons = ArrayAdapter.createFromResource(this, R.array.reasons, android.R.layout.simple_spinner_dropdown_item);
        tardyReason.setAdapter(reasons);
        tardyReason.setVisibility(View.INVISIBLE);
        tardyReason.setOnItemSelectedListener(this);

        checkPermissions();
        if (checkInternet()) {
            if (!rosterExists()) {
                downloadRoster();
            }
            readAndUpdateRoster();
            readAndAddStudentsFromFile();
        } else {
            alertDialogCreator("No Internet", "Unable to connect to Internet, cannot update or download roster, please turn internet on to update attendance.");
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_scan, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.roster:
                downloadRoster();
                return true;
            case R.id.save:
                readAndAddStudentsFromFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 14);

        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 13);

        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 12);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == 12) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Thank You", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 13) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Thank You", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 14) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Thank You", Toast.LENGTH_SHORT).show();
            }
        }
        // permissions this app might request.
    }

    public void doScan(View v) {
        if (scanButton.getText().toString().equals(SCAN)) {
            Intent intent = new Intent(ScanActivity.this, QrCodeActivity.class);
            startActivityForResult(intent, REQUEST_CODE_QR_SCAN);
        } else {
            if (sendToDatabase(false)) {
                Toast.makeText(this, "Successfully sent to Database", Toast.LENGTH_SHORT).show();
                scanButton.setText(SCAN);
            }
        }
    }

    public boolean sendToDatabase(boolean fromFile) {
        if (personScanned != null) {
            if (tardyReason.getVisibility() == View.VISIBLE && tardyReason.getSelectedItem().equals(reasons.getItem(0))) {
                Toast.makeText(this, "No Reason Selected, Please Select a Reason", Toast.LENGTH_SHORT).show();
                return false;
            } else if (tardyComments.getVisibility() == View.VISIBLE) {
                personScanned.setReason(tardyReason.getSelectedItem().toString());
                if (!tardyComments.getText().toString().isEmpty())
                    personScanned.setComments(tardyComments.getText().toString());
            }
            if (checkInternet()) {
                if (fromFile) {
                    schoolYear = setSchoolYearSaved(personScanned.getDate());
                    todayDate = personScanned.getDate();
                    dateRef = mRootRef.child(schoolYear).child(todayDate);
                    setFirebaseRefs();
                }
                switch (personScanned.getRole()) {
                    case "Student":
                        gradeRef = studentRef.child(personScanned.getGrade());
                        personRef = gradeRef.child(personScanned.getName());
                        personRef.child("Time").setValue(personScanned.getTime());
                        if (personScanned.getReason() != null)
                            personRef.child("Reason").setValue(personScanned.getReason());
                        if (personScanned.getComments() != null)
                            personRef.child("Comments").setValue(personScanned.getComments());
                        returnToScanPage();
                        break;

                    case "Management":
                        personRef = mgmtRef.child(personScanned.getName());
                        personRef.child("Time").setValue(personScanned.getTime());
                        if (personScanned.getReason() != null)
                            personRef.child("Reason").setValue(personScanned.getReason());
                        if (personScanned.getComments() != null)
                            personRef.child("Comments").setValue(personScanned.getComments());
                        returnToScanPage();
                        break;

                    case "Intern":
                    case "Interns":
                    case "Support":
                        personRef = supportRef.child(personScanned.getName());
                        personRef.child("Time").setValue(personScanned.getTime());
                        if (personScanned.getReason() != null)
                            personRef.child("Reason").setValue(personScanned.getReason());
                        if (personScanned.getComments() != null)
                            personRef.child("Comments").setValue(personScanned.getComments());
                        returnToScanPage();
                        break;

                    case "Teacher":
                        personRef = teacherRef.child(personScanned.getName());
                        personRef.child("Time").setValue(personScanned.getTime());
                        if (personScanned.getReason() != null)
                            personRef.child("Reason").setValue(personScanned.getReason());
                        if (personScanned.getComments() != null)
                            personRef.child("Comments").setValue(personScanned.getComments());
                        returnToScanPage();
                        break;

                    default:
                        alertDialogCreator("Invalid Post", "Data was sent incorrectly, Please try Again");
                        returnToScanPage();
                        return false;
                }
                return true;
            } else {
                savedPeople.add(personScanned);
                writeSavedStudentsToFile();
                Toast.makeText(this, "No Internet, Student saved to file, attendance not updated", Toast.LENGTH_SHORT).show();
                returnToScanPage();
                return false;
            }
        }
        alertDialogCreator("Error", "Something went wrong try again");
        returnToScanPage();
        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            if (data == null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if (result != null) {
                AlertDialog alertDialog = new AlertDialog.Builder(ScanActivity.this).create();
                alertDialog.setTitle("Scan Error");
                alertDialog.setMessage("Error in Scanned value: " + result);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            alertDialogCreator("Scan Error", "Unable to Scan QR Code");
        } else if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (data == null) return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            final String[] options = result.split(":");
            for(int i = 0; i<options.length; i++) {
                options[i] = options[i].trim();
            }
            if (options.length < 2) {
                Toast.makeText(this, "Unable to parse QR Code. Try again or scan a different one", Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder displayMessage = new StringBuilder();
            displayMessage.append("Role: " + options[0] + "\n" + "Name: " + options[1]);
            AlertDialog alertDialog = new AlertDialog.Builder(ScanActivity.this).create();
            alertDialog.setTitle("Is This You?");
            alertDialog.setMessage(displayMessage.toString());
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Date date = new Date();
                            schoolYear = setSchoolYear(date);
                            String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
                            todayDate = DateFormat.getDateInstance().format(date);
                            dateRef = mRootRef.child(schoolYear).child(todayDate);
                            setFirebaseRefs();
                            personScanned = searchForStudentInMap(options);
                            personScanned.setTime(time);
                            personScanned.setDate(todayDate);
                            if (personScanned.getRole().equals("Student") && ((calendar.get(Calendar.HOUR_OF_DAY) > 10) || (calendar.get(Calendar.HOUR_OF_DAY) == 10 && calendar.get(Calendar.MINUTE) > 40))) {
                                showTardy();
                            } else if((calendar.get(Calendar.HOUR_OF_DAY) > 10) || (calendar.get(Calendar.HOUR_OF_DAY) == 10 && calendar.get(Calendar.MINUTE) > 0)) {
                                showTardy();
                            } else {
                                if (sendToDatabase(false))
                                    Toast.makeText(ScanActivity.this, "Successfully sent to Database", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(ScanActivity.this, "Please Rescan your Code", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
            alertDialog.show();
        }
    }

    public String setSchoolYearSaved(String day) {
        String[] date = day.split(" ");
        String[] months = getString(R.string.months).split(":");
        int year = Integer.parseInt(date[2]);
        for (String s : months) {
            if (s.equals(date[0])) {
                return "" + year + "-" + (year + 1);//ex. year = 2018 and month is august --> 2018-2019
            }
        }
        return "" + (year - 1) + "-" + year;//ex. year = 2019 and month is february --> 2018-2019
    }

    public String setSchoolYear(Date date) {
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        if (month >= 8) {
            return "" + year + "-" + (year + 1);
        } else {
            return "" + (year - 1) + "-" + year;
        }
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

    public boolean checkInternet() {
        ConnectivityManager a = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            NetworkInfo networkInfo = a.getActiveNetworkInfo();
            if (!networkInfo.isConnectedOrConnecting()) {

                return false;
            } else {
                return true;
            }
        } catch (NullPointerException e) {
            return false;
        }
    }

    public void setFirebaseRefs() {
        studentRef = dateRef.child("Student");
        mgmtRef = dateRef.child("Management");
        supportRef = dateRef.child("Intern");
        teacherRef = dateRef.child("Teacher");
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        userIsInteracting = true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selectItemText = reasons.getItem(0).toString();
        Log.d("SOMEDUDE", "onItemSelected: " + selectItemText);
        if (!parent.getSelectedItem().toString().equalsIgnoreCase(selectItemText) && userIsInteracting) {
            Log.d("HELLO", "onItemSelected: ITEM HAS BEEN SELECTED!!!!");
            tardyComments.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void showTardy() {
        tardyText.setVisibility(View.VISIBLE);
        tardyReason.setVisibility(View.VISIBLE);
        scanButton.setText(SEND);
    }

    public void downloadRoster() {
        DatabaseReference rosterRef = mRootRef.child("People");
        Query q = rosterRef.orderByKey();
        final HashMap<String, ArrayList<Person>> people = initializeMap();
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("TAG", "onDataChange: Downloading Roster");
                for (DataSnapshot role : dataSnapshot.getChildren()) {
                    if (!("Student").equals(role.getKey())) {
                        for (DataSnapshot name : role.getChildren()) {
                            if (name.getValue() != null) {
                                Person p = new Person(role.getKey(), name.getValue().toString(), null, null);
                                people.get(role.getKey()).add(p);
                            }
                        }

                    } else {
                        for (DataSnapshot grade : role.getChildren()) {
                            for (DataSnapshot name : grade.getChildren()) {
                                if (name.getValue() != null) {
                                    Person p = new Person(role.getKey(), grade.getKey(), name.getValue().toString(), null, null);
                                    people.get(role.getKey()).add(p);
                                }
                            }
                        }
                    }
                }
                saveRoster(people);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public HashMap<String, ArrayList<Person>> initializeMap() {
        HashMap<String, ArrayList<Person>> map = new HashMap<>();
        String[] roles = getString(R.string.roles).split(":");
        for (String s : roles) {
            map.put(s, new ArrayList<Person>());
        }
        return map;
    }

    public void readAndUpdateRoster() {
        if (rosterExists()) {
            try {
                InputStream is = openFileInput(getString(R.string.roster_file_name));
                JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
                allPeople = initializeMap();
                reader.beginObject();
                while (reader.hasNext()) {
                    String role = reader.nextName();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        Person p = new Person();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            p.setRole(role);
                            String name = reader.nextName();
                            switch (name) {
                                case "Name":
                                    p.setName(reader.nextString());
                                    break;
                                case "Grade":
                                    p.setGrade(reader.nextString());
                                    break;
                            }
                        }
                        reader.endObject();
                        allPeople.get(role).add(p);
                    }
                    reader.endArray();
                }
                reader.endObject();
                reader.close();
            } catch (IOException e) {
                Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void saveRoster(HashMap<String, ArrayList<Person>> people) {
        try {
            FileOutputStream os = openFileOutput(getString(R.string.roster_file_name), Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.setIndent(" ");
            writer.beginObject();
            for (Map.Entry<String, ArrayList<Person>> entry : people.entrySet()) {
                writer.name(entry.getKey());
                writer.beginArray();
                for (Person p : entry.getValue()) {
                    writer.beginObject();
                    writer.name("Name").value(p.getName());
                    if (p.getRole().equals("Student"))
                        writer.name("Grade").value(p.getGrade());
                    writer.endObject();
                }
                writer.endArray();
            }
            writer.endObject();
            writer.close();
            readAndUpdateRoster();
        } catch (Exception e) {
            alertDialogCreator("Could Not Save Roster", "If the roster is not saved, attendance will not be updated. Please go to menu and download the roster for attendance to update.");
            e.printStackTrace();
        }
    }

    public void readAndAddStudentsFromFile() {
        if (studentsNotSaved()) {
            try {
                String line = "";
                InputStream is = openFileInput(getString(R.string.file_name));
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader reader = new BufferedReader(isr);

                while ((line = reader.readLine()) != null) {
                    String[] options = line.split("#");
                    for (int i = 0; i < options.length; i++) {
                        if (options[i].equals("null")) {
                            options[i] = null;
                        }
                    }
                    savedPeople.add(new Person(options[0], options[1], options[2], options[3], options[5], options[4], options[6]));
                }
                if (savedPeople.size() > 0) {
                    Iterator it = savedPeople.iterator();
                    while (it.hasNext()) {
                        personScanned = (Person) it.next();
                        if (sendToDatabase(true)) {
                            it.remove();
                        }
                    }
                }
                //in case some didn't go thru.
                writeSavedStudentsToFile();
            } catch (Exception e) {
                alertDialogCreator("IDK", "IDK WHATS GOING ON AAAAAAHHHHHH");
            }
        }
    }

    public void writeSavedStudentsToFile() {
        try {
            FileOutputStream os = openFileOutput(getString(R.string.file_name), Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(os);
            if (savedPeople.size() > 0) {
                Iterator itr = savedPeople.iterator();
                Person p;
                while(itr.hasNext()) {
                    if((p = (Person)itr.next()) != null) {
                        writeStudentToFile(osw, p);
                    }
                }
            } else {
                if (!studentsNotSaved()) {
                    new File(getFilesDir() + "/" + getString(R.string.file_name)).delete();
                }
            }
            osw.close();

        } catch (Exception e) {
            e.printStackTrace();
            alertDialogCreator("Unexpected Error", "Could not save student for later retrieval. Please try again.");
        }
    }

    public void writeStudentToFile(OutputStreamWriter osw, Person p) {
        try {
            osw.write(p.toString());
        } catch (Exception e) {
            alertDialogCreator("Unexpected Error", "Could not save student for later retrieval. Please try again.");
        }
    }

    public boolean rosterExists() {
        return new File(getFilesDir() + "/" + getString(R.string.roster_file_name)).exists();
    }

    public boolean studentsNotSaved() {
        File file = new File(getFilesDir() + "/" + getString(R.string.file_name));
        return (file.exists() && file.length() > 0);
    }

    public Person searchForStudentInMap(String[] options) {
        if (allPeople.get(options[0]) != null) {
            for (Person p : allPeople.get(options[0])) {
                if (p.getName().equals(options[1])) {
                    return p;
                }
            }
        }
        alertDialogCreator("Person Not Found", "" + options[1] + "Not found.\n Re-Download Roster and try again if person was recently added.");
        return null;
    }

    public void returnToScanPage() {
        tardyReason.setVisibility(View.INVISIBLE);
        tardyComments.setVisibility(View.INVISIBLE);
        tardyComments.setText("");
        tardyText.setVisibility(View.INVISIBLE);
        scanButton.setText(SCAN);
    }
}
