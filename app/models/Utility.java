package models;


import java.math.BigInteger;
import java.security.MessageDigest;

public class Utility {
    public String getMD5Hash(String s) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");

        messageDigest.update(s.getBytes(), 0, s.length());

        String hash = new BigInteger(1, messageDigest.digest()).toString(16);

        return hash;
    }
}
