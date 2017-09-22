package com.appdynamics.extensions.sql;


import java.sql.*;

public class JDBCConnectionAdapter {

    private final String connUrl;

    private JDBCConnectionAdapter(String connStr){
        this.connUrl = connStr;

    }

    static JDBCConnectionAdapter create(String connUrl){
        return new JDBCConnectionAdapter(connUrl);
    }

    Connection open() throws SQLException {
        Connection connection;
        connection = DriverManager.getConnection(connUrl);
        return connection;
    }

    ResultSet queryDatabase(Connection connection, String query) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(query);
    }

    void closeStatement(Statement statement) throws Exception{
        statement.close();
    }

    void closeConnection(Connection connection) throws Exception {
        connection.close();
    }
}
