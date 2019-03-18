package com.tanwir.qrcodescanner;

public class Person {
    private String date;
    private String role;
    private String grade;
    private String name;
    private String reason;

    public Person(String date, String role, String grade, String name, String reason, String time, String comments) {
        this.date = date;
        this.role = role;
        this.grade = grade;
        this.name = name;
        this.reason = reason;
        this.time = time;
        this.comments = comments;
    }

    private String time;
    private String comments;

    public Person() {
        this.role = null;
    }

    public Person(String role, String grade, String name, String date, String time) {
        this.role = role;
        this.grade = grade;
        this.name = name;
        this.date = date;
        this.time = time;
    }

    public Person(String role, String name, String date, String time) {
        this.role = role;
        this.grade = null;
        this.name = name;
        this.date = date;
        this.time = time;
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.date);
        sb.append("#"+this.role);
        sb.append("#"+this.grade);
        sb.append("#"+this.name);
        sb.append("#"+this.time);
        sb.append("#"+this.reason);
        sb.append("#"+this.comments+"\n");
        return sb.toString();
    }
}
