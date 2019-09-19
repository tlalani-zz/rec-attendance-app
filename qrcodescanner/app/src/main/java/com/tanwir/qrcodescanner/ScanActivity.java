package com.tanwir.qrcodescanner;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ScanActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_QR_SCAN = 101;
    private static final int REQUEST_TARDY_INFORMATION = 102;
    private static final String SCAN = "SCAN";
    private static final String SEND = "SEND";
    private Button scanButton;
    private String todayDate;
    private String schoolYear;
    private Person personScanned;
    private Calendar calendar;
    private HashMap<String, ArrayList<Person>> allPeople;
    private HashSet<Person> savedPeople = new HashSet<>();
    private boolean isDownloaded = false;

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
        setContentView(R.layout.activity_scan);
        calendar = GregorianCalendar.getInstance();
        calendar.setTime(new Date());

        checkPermissions();
        if (connectedToInternet()) {
            if (!rosterExists()) {
                downloadRoster(new Date());
            }
            readRosterFromFile();
            readAndAddStudentsFromFile();
        } else {
            createAlertDialogWithTitleAndMessage("No Internet", "Unable to connect to Internet, cannot update or download attendance roster, please connect to internet to update attendance.");
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.roster:
                downloadRoster(new Date());
                return true;
            case R.id.save:
                readAndAddStudentsFromFile();
                return true;
            case R.id.manual:
                startActivity(new Intent(this, ManualEntryActivity.class));
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
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
        Intent intent = new Intent(ScanActivity.this, QrCodeActivity.class);
        startActivityForResult(intent, REQUEST_CODE_QR_SCAN);
    }

    public boolean connectedToInternet() {
        ConnectivityManager a = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            NetworkInfo networkInfo = a.getActiveNetworkInfo();
            return networkInfo.isConnectedOrConnecting();
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

    public void downloadRoster(Date date) {
        schoolYear = formatSchoolYearFromDateObject(date);
        DatabaseReference rosterRef = mRootRef.child("People").child(schoolYear);
        Query q = rosterRef.orderByKey();
        final HashMap<String, ArrayList<Person>> people = initializeMap();
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot role : dataSnapshot.getChildren()) {
                    if (hasGrade(role.getKey())) {
                        for (DataSnapshot grade : role.getChildren()) {
                            for (DataSnapshot name : grade.getChildren()) {
                                if (name.getValue() != null) {
                                    //Person with role grade and name
                                    Person p = new Person(role.getKey(), name.getValue().toString(), grade.getKey());
                                    Objects.requireNonNull(people.get(role.getKey())).add(p);
                                }
                            }
                        }
                    } else {
                        for (DataSnapshot name : role.getChildren()) {
                            if (name.getValue() != null) {
                                //Person with only role and name.
                                Person p = new Person(role.getKey(), name.getValue().toString(), null);
                                Objects.requireNonNull(people.get(role.getKey())).add(p);
                            }
                        }
                    }
                }
                saveRoster(people);
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                createAlertDialogWithTitleAndMessage("Error", "Couldn't access Database");
            }
        });
    }

    public boolean hasGrade(String s) {
        return s != null && (s.equals("Student") || s.equals("Teacher"));
    }

    public HashMap<String, ArrayList<Person>> initializeMap() {
        HashMap<String, ArrayList<Person>> map = new HashMap<>();
        String[] roles = getString(R.string.roles).split(":");
        for (String s : roles) {
            map.put(s, new ArrayList<Person>());
        }
        return map;
    }

    public void readRosterFromFile() {
        if (rosterExists()) {
            try {
                InputStream is = openFileInput(getString(R.string.roster_file_name));
                JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                allPeople = initializeMap();
                reader.beginObject();
                reader.nextName();
                SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");
                try {
                    Date date = formatter.parse(reader.nextString());
                    long diff= TimeUnit.DAYS.convert(date.getTime() - new Date().getTime(), TimeUnit.MILLISECONDS);
                    if(diff > 30) {
                        downloadRoster(date);
                        return;
                    }
                } catch(ParseException pe) {
                    pe.printStackTrace();
                }
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
                        Objects.requireNonNull(allPeople.get(role)).add(p);
                    }
                    reader.endArray();
                }
                reader.endObject();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void saveRoster(HashMap<String, ArrayList<Person>> people) {
        try {
            FileOutputStream os = openFileOutput(getString(R.string.roster_file_name), Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            writer.setIndent(" ");
            writer.beginObject();
            writer.name("Date").value(DateFormat.getDateInstance().format(new Date()));
            for (Map.Entry<String, ArrayList<Person>> entry : people.entrySet()) {
                writer.name(entry.getKey());
                writer.beginArray();
                for (Person p : entry.getValue()) {
                    writer.beginObject();
                    writer.name("Name").value(p.getName());
                    if (p.getRole().equals("Student") || p.getRole().equals("Teacher"))
                        writer.name("Grade").value(p.getGrade());
                    writer.endObject();
                }
                writer.endArray();
            }
            writer.endObject();
            writer.close();
            readRosterFromFile();
        } catch (Exception e) {
            createAlertDialogWithTitleAndMessage("Could Not Save Roster", "If the roster is not saved, " +
                    "attendance will not be updated. Please go to menu and download the roster for attendance to update.");
            e.printStackTrace();
        }
    }


    public String formatSchoolYearFromString(String todayDate) {
        String[] date = todayDate.replace(",","").split(" "); //turns Aug 21, 2019 to ['Aug', '21', '2019'].
        String[] months = getString(R.string.months).split(":"); //String of short month names like Jan, Feb, Mar...
        int year = Integer.parseInt(date[2]);
        for (String month : months) {
            if (month.equals(date[0])) {
                return "" + year + "-" + (year + 1);//ex. year = 2018 and month is august --> 2018-2019
            }
        }
        return "" + (year - 1) + "-" + year;//ex. year = 2019 and month is february --> 2018-2019
    }

    public String formatSchoolYearFromDateObject(Date date) {
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        Log.d("month", "formatSchoolYearFromDateObject: "+month);
        int year = calendar.get(Calendar.YEAR);
        if (month >= 7) {
            return "" + year + "-" + (year + 1);
        } else {
            return "" + (year - 1) + "-" + year;
        }
    }

    public void writeStudentToFile(OutputStreamWriter osw, Person p) {
        try {
            osw.write(p.toString());
        } catch (Exception e) {
            createAlertDialogWithTitleAndMessage("Unexpected Error", "Could not save student for later retrieval. Please try again.");
        }
    }

    public boolean rosterExists() {
        File f = new File(getFilesDir() + "/" + getString(R.string.roster_file_name));
        return f.exists() && f.length() > 0;
    }

    public long peopleNotSaved() {
        File file = new File(getFilesDir() + "/" + getString(R.string.file_name));
        if (file.exists() && file.length() > 0) {
            return file.length();
        }
        return 0L;
    }

    public ArrayList<Person> searchForPersonInMap(String[] options) {
        ArrayList<Person> pList = new ArrayList<>();
        if (allPeople.get(options[0]) != null) {
            for (Person p : Objects.requireNonNull(allPeople.get(options[0]))) {
                if (p.getName().equals(options[1])) {
                    pList.add(p);
                }
            }
            Log.d("TAYAG", "searchForPersonInMap: "+pList);
            if(pList.size() > 0) {
                return pList;
//                return pList.toArray(Person[] pe);
            } else {
                createAlertDialogWithTitleAndMessage("Person Not Found", "" + options[1] + "Not found.\n Re-Download Roster and try again if person was recently added.");
                return null;
            }

        }
        createAlertDialogWithTitleAndMessage("Person Not Found", "" + options[1] + "Not found.\n Re-Download Roster and try again if person was recently added.");
        return null;
    }

    public boolean personIsTardy(int hour, int minute) {
        return (calendar.get(Calendar.HOUR_OF_DAY) > hour) || (calendar.get(Calendar.HOUR_OF_DAY) == hour && calendar.get(Calendar.MINUTE) > minute);

    }

    public void createAlertDialogWithTitleAndMessage(String title, String message) {
        android.app.AlertDialog.Builder alertdialog = new android.app.AlertDialog.Builder(this);
        alertdialog
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true);
        android.app.AlertDialog d = alertdialog.create();
        d.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            if(requestCode == REQUEST_CODE_QR_SCAN)
                createAlertDialogWithTitleAndMessage("Scan Error", "Unable to Scan QR Code");
            else if(requestCode == REQUEST_TARDY_INFORMATION) {
                createAlertDialogWithTitleAndMessage("Unable to add Tardy Information", "Tardy Information was unable to be added. Please try again.");
            }
        } else if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (data == null) return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            String[] options = result.split(":");
            for(int i = 0; i<options.length; i++) {
                options[i] = options[i].trim();
            }
            if (options.length < 2) {
                Toast.makeText(this, "Unable to parse QR Code. Try again or scan a different one", Toast.LENGTH_SHORT).show();
                return;
            }
            ArrayList<Person> personArrayList = searchForPersonInMap(options);
            if(personArrayList != null && !personArrayList.isEmpty()) {
                createScanInformationDialog(options, personArrayList);
            } else {
                createAlertDialogWithTitleAndMessage("Error", "We were unable to find this person, Please open" +
                        " the menu on the top right and select download roster if you have recently updated it. Otherwise, try again.");
            }
        } else if(requestCode == REQUEST_TARDY_INFORMATION) {
            if (data == null) return;
            String[] tardyInfo = data.getStringArrayExtra("tardyInfo");
            personScanned.setReason(tardyInfo[0]);
            if(tardyInfo[1] != null && !tardyInfo[1].isEmpty())
                personScanned.setComments(tardyInfo[1]);
            sendToDatabase(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //write to file here
    }

    @Override
    protected void onResume() {
        super.onResume();
        //write to db here if internet
    }

    public boolean sendToDatabase(boolean sendingFromFile) {
        if (personScanned != null) {
            if (personScanned.getReason() == null && personScanned.isTardy()) {
                Toast.makeText(this, "No Reason Selected, Please Select a Reason", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (connectedToInternet()) {
                if (sendingFromFile) {
                    schoolYear = formatSchoolYearFromString(personScanned.getDate());
                    todayDate = personScanned.getDate();
                    dateRef = mRootRef.child(schoolYear).child(todayDate);
                    setFirebaseRefs();
                }
                Log.d("ScanActivity", "sendToDatabase: "+personScanned);
                switch (personScanned.getRole()) {
                    case "Teacher":
                        gradeRef = teacherRef.child(personScanned.getGrade());
                        personRef = gradeRef.child(personScanned.getName());
                        break;

                    case "Student":
                        gradeRef = studentRef.child(personScanned.getGrade());
                        personRef = gradeRef.child(personScanned.getName());
                        break;

                    case "Management":
                        personRef = mgmtRef.child(personScanned.getName());
                        break;

                    case "Intern":
                    case "Interns":
                    case "Support":
                        personRef = supportRef.child(personScanned.getName());
                        break;

                    default:
                        createAlertDialogWithTitleAndMessage("Invalid Post", "Data was sent incorrectly, Please try Again");
                        return false;
                }
                personRef.child("Time").setValue(personScanned.getTime());
                personRef.child("Status").setValue(personScanned.getStatus());
                if (personScanned.getReason() != null) {
                    personRef.child("Reason").setValue(personScanned.getReason());
                }
                if (personScanned.getComments() != null)
                    personRef.child("Comments").setValue(personScanned.getComments());
                return true;
            } else {
                savedPeople.add(personScanned);
                writeSavedStudentsToFile();
                Toast.makeText(this, "No Internet, Student saved to file, attendance not updated", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        createAlertDialogWithTitleAndMessage("Error", "Something went wrong try again");
        return false;
    }

    public void readAndAddStudentsFromFile() {
        if (peopleNotSaved() > 0) {
            try {
                String line;
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
                    savedPeople.add(new Person(options[0], options[1], options[2], options[3], options[5], options[4], options[6], Person.Status.valueOf(options[7])));
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
                writeSavedStudentsToFile();
            } catch (Exception e) {
                createAlertDialogWithTitleAndMessage("IDK", "IDK WHATS GOING ON AAAAAAHHHHHH");
            }
        }
    }

    public boolean writeSavedStudentsToFile() {
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
                if (peopleNotSaved() == 0) {
                    return new File(getFilesDir() + "/" + getString(R.string.file_name)).delete();
                } else {
                    return false;
                }
            }
            osw.close();

        } catch (Exception e) {
            e.printStackTrace();
            createAlertDialogWithTitleAndMessage("Unexpected Error", "Could not save student for later retrieval. Please try again.");
        }
        return false;
    }


    public void createScanInformationDialog(String[] options, ArrayList<Person> pArrayList) {
        final ArrayList<Person> personArrayList = pArrayList;
        String displayMessage = "Role: " + options[0] + "\n" + "Name: " + options[1];
        displayMessage += options.length > 2 ? "\n" + "Grade" + options[2] : "";

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ScanActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.alert_dialog, null);
        final Spinner sp = mView.findViewById(R.id.spinner3);
        String[] s = new String[personArrayList.size()];
        if(options[0].equals("Management") || options[0].equals("Intern")) {
            s[0] = "";
        } else {
            for (int i = 0; i < personArrayList.size(); i++) {
                s[i] = personArrayList.get(i).getGrade();
            }
        }
        final ArrayAdapter<String> adp = new ArrayAdapter<>(ScanActivity.this,
                android.R.layout.simple_spinner_item, s);
        adp.setDropDownViewResource(R.layout.spinner_item);
        sp.setAdapter(adp);
        alertDialog.setView(mView);
        alertDialog.setTitle("Is This You?");
        alertDialog.setMessage(displayMessage);
        alertDialog.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Date date = new Date();
                        schoolYear = formatSchoolYearFromDateObject(date);
                        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
                        todayDate = DateFormat.getDateInstance().format(date);
                        int index = sp.getSelectedItemPosition();
                        personScanned = personArrayList.get(index);
                        personScanned.setTime(time);
                        personScanned.setDate(todayDate);
                        Log.d("THESETIMES", "personIsTardy: "+calendar.get(Calendar.HOUR_OF_DAY) + " " + calendar.get((Calendar.MINUTE)));
                        if ((personScanned.isStudentOrIntern() && (personIsTardy(10, 55))) || ((!personScanned.isStudentOrIntern()) && (personIsTardy(10, 10)))) {
                            personScanned.setTardy(true);
                            personScanned.setStatus(Person.Status.T);
                            startActivityForResult(new Intent(ScanActivity.this, TardyActivity.class), REQUEST_TARDY_INFORMATION);
                        } else {
                            personScanned.setStatus(Person.Status.P);
                            sendToDatabase(false);
                        }
                    }
                });
        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(ScanActivity.this, "Please Rescan your Code", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        alertDialog.create().show();
    }

}
