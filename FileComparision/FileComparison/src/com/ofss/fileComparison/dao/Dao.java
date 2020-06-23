package com.ofss.fileComparison.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class Dao {

    private Connection con = null;
    private String getPrimaryColumnsQuery = "SELECT \n" +
            "  TABLE_NAME,\n" +
            "  COLUMN_NAME\n" +
            "FROM \n" +
            "  SYS.USER_CONS_COLUMNS\n" +
            "WHERE \n" +
            "  TABLE_NAME = '%s'";

    public Dao() {
        con = DBHelper.getConnection();
    }


    public Set<String> getPrimaryColumns(String tableName) {
        Set<String> primaryColumns = new HashSet<>();
        try {
            PreparedStatement query = con.prepareStatement(String.format(getPrimaryColumnsQuery, tableName));
            ResultSet rs = query.executeQuery();
            while(rs.next()){
                primaryColumns.add(rs.getString("COLUMN_NAME"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return primaryColumns;
    }
}
