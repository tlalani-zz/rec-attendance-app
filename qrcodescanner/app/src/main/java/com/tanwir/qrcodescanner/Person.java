package com.tanwir.qrcodescanner;

import java.io.Serializable;

import lombok.Data;
import lombok.NonNull;

@Data
public class Person implements Serializable {
    private String date;
    private String role;
    private String grade;
    private String name;
    private String reason;
    private String time;
    private String comments;
    private Status status;
    private String DIVIDER = "#";

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

    boolean isStudentOrIntern() {
        return (this.role.equals("Student") || this.role.equals("Intern"));
    }


    @Override
    public String toString() {
        return this.date + DIVIDER +
                this.role + DIVIDER +
                this.grade + DIVIDER +
                this.name + DIVIDER +
                this.reason + DIVIDER +
                this.time + DIVIDER +
                this.comments + DIVIDER +
                this.status+"\n";
    }

}
