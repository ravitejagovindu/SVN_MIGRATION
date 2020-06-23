package com.ofss.fileComparison.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class DBHelper {

    private static Connection con = null;
    private static Properties properties = new Properties();

    static {
        try {
            properties.load(DBHelper.class.getClassLoader().getResourceAsStream("db.properties"));
//            properties.load(new FileInputStream(new File("db.properties")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DBHelper() {
    }

    public static Connection getConnection() {
        if (Objects.isNull(con)) {
            try {
                Class.forName(properties.getProperty("driverName"));
                con = DriverManager.getConnection(properties.getProperty("url"),
                        properties.getProperty("userName"),
                        properties.getProperty("password"));
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
        return con;
    }

}
