package io.itpl.model;

import java.util.Arrays;
import java.util.Objects;

public class User {
    private int id;
    private String enrollmentNumber;
    private String name;

    // Default constructor
    public User() { }

    // Constructor with enrollmentNumber, name, and faceData
    public User(String enrollmentNumber, String name, byte[] faceData) {
        this.enrollmentNumber = enrollmentNumber;
        this.name = name;
    }

    // Constructor with all fields
    public User(int id, String enrollmentNumber, String name, byte[] faceData) {
        this.id = id;
        this.enrollmentNumber = enrollmentNumber;
        this.name = name;
    }

    // Getter and setter for id
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    // Getter and setter for enrollmentNumber
    public String getEnrollmentNumber() {
        return enrollmentNumber;
    }
    public void setEnrollmentNumber(String enrollmentNumber) {
        this.enrollmentNumber = enrollmentNumber;
    }

    // Getter and setter for name
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", enrollmentNumber='" + enrollmentNumber + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id &&
                enrollmentNumber.equals(user.enrollmentNumber) &&
                name.equals(user.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, enrollmentNumber, name);
        result = 31 * result;
        return result;
    }
}
