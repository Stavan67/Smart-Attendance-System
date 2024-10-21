package io.itpl.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection
{
    private static final String URL = "jdbc:mysql://localhost:3306/sgp";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static
    {
        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("MySQL JDBC Driver not found");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException
    {
        try
        {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }
        catch (SQLException e)
        {
            System.err.println("Failed to connect to the database");
            e.printStackTrace();
            throw e;
        }
    }
}