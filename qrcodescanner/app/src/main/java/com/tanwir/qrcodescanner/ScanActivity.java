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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ScanActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_QR_SCAN = 101;
    private static final int REQUEST_TARDY_INFORMATION = 102;
    private String todayDate;
    private String schoolYear;
    private Person personScanned;
    private Calendar currentTime;
    private Calendar tardyTime;
    private HashMap<String, ArrayList<Person>> allPeople;
    private HashSet<Person> savedPeople = new HashSet<>();
    DatabaseReference dbRoot = FirebaseDatabase.getInstance().getReference();
    public ReConfigs.ReCurrentConfig currentConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        currentTime = Calendar.getInstance();
        schoolYear = formatSchoolYearFromDateObject();
        getCurrentConfig(getIntent());
        /* Check config object in RecSelectActivity.java */
        setupTardyTime(currentConfig.re_shift.split("/")[1]);
        dbRoot = dbRoot.child("REC/")
                .child(currentConfig.re_center)
                .child(currentConfig.re_class)
                .child("Shifts/")
                .child(currentConfig.re_shift);
        checkPermissions();
        if (connectedToInternet()) {
            if (!rosterExists()) {
                downloadRoster();
            }
            readRosterFromFile();
            readAndAddStudentsFromFile();
        } else {
            createAlertDialogWithTitleAndMessage("No Internet", "Unable to connect to Internet, cannot update or download attendance roster, please connect to internet to update attendance.");
        }
    }

    public void getCurrentConfig(Intent i) {
        if(i.hasExtra("center") && i.hasExtra("class") && i.hasExtra("shift")) {
            currentConfig = new ReConfigs.ReCurrentConfig(i);
        } else {
            Toast.makeText(this, "Something went wrong. Please re-select your configurations", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public String getRosterFileName() {
        String shift = currentConfig.re_shift.replace("/", "@");
        return "roster#" + currentConfig.re_center + "#"+currentConfig.re_class + "#" + shift + ".json";
    }



    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
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

    public void downloadRoster() {
        schoolYear = formatSchoolYearFromDateObject();
        DatabaseReference rosterRef = dbRoot.child("People").child(schoolYear);
        Query q = rosterRef.orderByKey();
        final HashMap<String, ArrayList<Person>> people = new HashMap<>();
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot role : dataSnapshot.getChildren()) {
                    for (DataSnapshot item : role.getChildren()) {
                        if(hasGrade(item.getKey())) {
                            DataSnapshot grade = item;
                            for (DataSnapshot name : grade.getChildren()) {
                                if (name.getValue() != null) {
                                    //Person with role grade and name
                                    Person p = new Person(role.getKey(), name.getValue().toString(), grade.getKey());
                                    addToMap(people, role.getKey(), p);
                                }
                            }
                        } else {
                            DataSnapshot name = item;
                            if (name.getValue() != null) {
                                //Person with role grade and name
                                Person p = new Person(role.getKey(), name.getValue().toString(), null);
                                addToMap(people, role.getKey(), p);
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
        if(s == null) return false;
        for(String grade : getResources().getStringArray(R.array.grades)) {
            if(grade.equals(s)) {
                return true;
            }
        }
        return false;
    }

    public void readRosterFromFile() {
        if (rosterExists()) {
            try {
                InputStream is = openFileInput(getRosterFileName());
                JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                allPeople = new HashMap<>();
                reader.beginObject();
                reader.nextName();
                SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");
                try {
                    Date date = formatter.parse(reader.nextString());
                    long diff= TimeUnit.DAYS.convert(date.getTime() - new Date().getTime(), TimeUnit.MILLISECONDS);
                    if(diff > 30) {
                        downloadRoster();
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
                        addToMap(allPeople, role, p);
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

    public void addToMap(HashMap<String, ArrayList<Person>> map, String key, Person value) {
        if(map.get(key) != null) {
            map.get(key).add(value);
        } else {
            map.put(key, new ArrayList<Person>());
            map.get(key).add(value);
        }
    }

    public void saveRoster(HashMap<String, ArrayList<Person>> people) {
        try {
            FileOutputStream os = openFileOutput(getRosterFileName(), Context.MODE_PRIVATE);
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
                    if (p.getGrade() != null)
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
                    "attendance will not be updated. Please go to menu and download the roster or give permissions.");
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

    public String formatSchoolYearFromDateObject() {
        currentTime.setTime(new Date());
        int month = currentTime.get(Calendar.MONTH);
        Log.d("month", "formatSchoolYearFromDateObject: "+month);
        int year = currentTime.get(Calendar.YEAR);
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
        File f = new File(getFilesDir() + "/" + getRosterFileName());
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
        DatabaseReference currentRef = dbRoot.child("Dates");
        if (personScanned != null) {
            if (connectedToInternet()) {
                if (sendingFromFile) {
                    schoolYear = formatSchoolYearFromString(personScanned.getDate());
                    todayDate = personScanned.getDate();
                }
                currentRef = currentRef.child(schoolYear).child(todayDate);
                //Getting Reference
                if(personScanned.hasGrade()) {
                    currentRef = currentRef.child(personScanned.getRole())
                                        .child(personScanned.getGrade()).child(personScanned.getName());
                } else {
                    currentRef = currentRef.child(personScanned.getRole()).child(personScanned.getName());
                }

                //Send to DB Now
                currentRef.child("Time").setValue(personScanned.getTime());
                currentRef.child("Status").setValue(personScanned.getStatus());
                if (personScanned.getReason() != null) {
                    currentRef.child("Reason").setValue(personScanned.getReason());
                }
                if (personScanned.getComments() != null)
                    currentRef.child("Comments").setValue(personScanned.getComments());
                return true;
            } else {
                savedPeople.add(personScanned);
                writeSavedStudentsToFile();
                Toast.makeText(this, "No Internet, Student saved locally, attendance not updated", Toast.LENGTH_SHORT).show();
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
                createAlertDialogWithTitleAndMessage("Error", "There was an error sending to database please try again.");
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
                        currentTime.setTime(new Date());
                        schoolYear = formatSchoolYearFromDateObject();
                        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(currentTime.getTime());
                        todayDate = DateFormat.getDateInstance().format(currentTime.getTime());
                        int index = sp.getSelectedItemPosition();
                        personScanned = personArrayList.get(index);
                        personScanned.setTime(time);
                        personScanned.setDate(todayDate);
                        if (isPersonTardy()) {
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

    public void setupTardyTime(String times) {
        Integer[] s2 = parseDate(times);
        try {
            tardyTime = Calendar.getInstance();
            tardyTime.setTime(new Date());
            tardyTime.set(Calendar.HOUR_OF_DAY, s2[0]);
            tardyTime.set(Calendar.MINUTE, s2[1]);
            tardyTime.add(Calendar.MINUTE, 10);
            tardyTime.set(Calendar.MILLISECOND, 0);
            tardyTime.set(Calendar.SECOND, 0);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPersonTardy() {
        currentTime.set(Calendar.SECOND, 0);
        currentTime.set(Calendar.MILLISECOND, 0);
        return !currentTime.getTime().before(tardyTime.getTime()) || currentTime.getTime().equals(tardyTime.getTime());
    }


    public static Integer[] parseDate(String s) {
        String[] s2 = s.split("-")[0].split("_");
        if(s2[1].equals("AM")) {
            return new Integer[]{Integer.parseInt(s2[0].split(":")[0]), (Integer.parseInt(s2[0].split(":")[1]))};
        } else {
            return new Integer[]{Integer.parseInt(s2[0].split(":")[0]) + 12, (Integer.parseInt(s2[0].split(":")[1]))};
        }
    }

}
