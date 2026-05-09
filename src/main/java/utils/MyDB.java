package utils;
import java.sql.*;
public class MyDB {
    private Connection conn;
    final private String URL = "jdbc:mysql://localhost:3306/momentum";
    final private String USER = "root";
    final private String PASS = "";
    private static MyDB instance;

    private MyDB() {
        try {
            conn = DriverManager.getConnection(URL,USER,PASS);
            System.out.println("Connected");
        } catch (SQLException s){
            System.out.println(s.getMessage());
        }
    }
    public static MyDB getInstance() {
        if(instance == null) {
            instance = new MyDB();
        }
        return instance;
    }

    public Connection getConn() {
        return conn;
    }
}
