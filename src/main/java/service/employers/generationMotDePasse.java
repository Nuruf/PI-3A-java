package service.employers;

import java.security.SecureRandom;

public class generationMotDePasse {
    private static final String caractere="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int len_mot_de_passe=10;
    public static String generer(){
        SecureRandom random=new SecureRandom();
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<len_mot_de_passe;i++){
            int j=random.nextInt(caractere.length());
            sb.append(caractere.charAt(j));
        }
        return sb.toString();
    }
}
