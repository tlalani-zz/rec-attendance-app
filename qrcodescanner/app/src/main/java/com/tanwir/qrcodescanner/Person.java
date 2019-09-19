package com.tanwir.qrcodescanner;

import lombok.Data;
import lombok.NonNull;

@Data
public class Person {
    private String date;
    private String role;
    private String grade;
    private String name;
    private String reason;
    private boolean tardy;
    private String time;
    private String comments;
    private Status status;

    enum Status {
        A, P, T;
    }

    Person(String date, String role, String grade, String name, String reason, String time, String comments, Status status) {
        this.date = date;
        this.role = role;
        this.grade = grade;
        this.name = name;
        this.reason = reason;
        this.time = time;
        this.comments = comments;
        this.status = status;
    }

    Person() {
        this.role = null;
    }

    Person(String role, String name, String grade) {
        this.role = role;
        this.grade = grade;
        this.name = name;
    }

    boolean hasGrade() {
        return this.grade != null;
    }

    boolean isTardy() {
        return tardy;
    }

    boolean isStudentOrIntern() {
        return (this.role.equals("Student") || this.role.equals("Intern"));
    }


    String toFileString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.date);
        sb.append("#"+this.role);
        sb.append("#"+this.grade);
        sb.append("#"+this.name);
        sb.append("#"+this.time);
        sb.append("#"+this.reason);
        sb.append("#"+this.comments);
        sb.append("#"+this.status+"\n");
        return sb.toString();
    }
}
