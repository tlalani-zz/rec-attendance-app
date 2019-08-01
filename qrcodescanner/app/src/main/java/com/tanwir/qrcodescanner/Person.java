package com.tanwir.qrcodescanner;

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

    public enum Status {
        Absent, Present, Tardy;
    }

    public Person(String date, String role, String grade, String name, String reason, String time, String comments, Status status) {
        this.date = date;
        this.role = role;
        this.grade = grade;
        this.name = name;
        this.reason = reason;
        this.time = time;
        this.comments = comments;
        this.status = status;
    }

    public Person() {
        this.role = null;
    }

    public Person(String role, String grade, String name, String date, String time, Status status) {
        this.role = role;
        this.grade = grade;
        this.name = name;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public Person(String role, String name, String date, String time, Status status) {
        this.role = role;
        this.grade = null;
        this.name = name;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public Person(String role, String name, String grade) {
        this.role = role;
        this.grade = grade;
        this.name = name;
    }


    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return this.status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isTardy() {
        return tardy;
    }

    public void setTardy(boolean tardy) {
        this.tardy = tardy;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isStudentOrIntern() {
        return (this.role == "Student" || this.role == "Intern");
    }


    public String toString() {
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
