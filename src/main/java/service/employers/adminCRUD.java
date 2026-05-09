package service.employers;

import entities.employers.administrateur_systeme;
import utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class adminCRUD {
    private Connection conn;
    public adminCRUD() throws SQLException {
        conn= MyDB.getInstance().getConn();
    }
    public administrateur_systeme findbyemail(String email) throws SQLException
    {
        String sql="select * from administrateur_systeme where e_mail=? ";
        PreparedStatement ps=conn.prepareStatement(sql);
        ps.setString(1,email);
        ResultSet rs=ps.executeQuery();
        if(rs.next()){
            administrateur_systeme admin= new administrateur_systeme();
            admin.setId(rs.getInt("id"));
            admin.setE_mail(rs.getString("e_mail"));
            admin.setMot_de_passe(rs.getString("mot_de_passe"));
            return admin;
        }
        return null;
    }
}
