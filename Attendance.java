package io.itpl.model;

import java.sql.Time;
import java.util.Date;
import java.util.Objects;

public class Attendance
{
    private int id;
    private String enrollmentNumber;
    private Date date;
    private Time time;
    private String subject;
    private AttendanceStatus status;
    public enum AttendanceStatus
    {
        PRESENT, ABSENT
    }
    public Attendance()
    {
    }

    public Attendance(String enrollmentNumber, Date date, Time time, String subject, AttendanceStatus status) {
        this.enrollmentNumber = enrollmentNumber;
        this.date = date;
        this.time = time;
        this.subject = subject;
        this.status = status;
    }

    // Constructor with all fields
    public Attendance(int id, String enrollmentNumber, Date date, Time time, String subject, AttendanceStatus status) {
        this.id = id;
        this.enrollmentNumber = enrollmentNumber;
        this.date = date;
        this.time = time;
        this.subject = subject;
        this.status = status;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEnrollmentNumber() {
        return enrollmentNumber;
    }

    public void setEnrollmentNumber(String enrollmentNumber) {
        this.enrollmentNumber = enrollmentNumber;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Attendance{" +
                "id=" + id +
                ", enrollmentNumber='" + enrollmentNumber + '\'' +
                ", date=" + date +
                ", time=" + time +
                ", subject='" + subject + '\'' +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attendance that = (Attendance) o;
        return id == that.id &&
                Objects.equals(enrollmentNumber, that.enrollmentNumber) &&
                Objects.equals(date, that.date) &&
                Objects.equals(time, that.time) &&
                Objects.equals(subject, that.subject) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, enrollmentNumber, date, time, subject, status);
    }
}