package utils;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * 
 * @author Claus - clih@itu.dk
 * @author Michael - msoa@itu.dk
 *
 */
public class TokenService {
  public static byte[] getToken(byte[] credentials) {
    try {
      String credentialsStr;
      credentialsStr = Encrypter.decryptByteArray(credentials);
      String[] split = credentialsStr.split(",");
      String user = split[0];
      String pass = split[1];
      long ts = 0;
      ts = ItuAuthentication.authenticate(user, pass);
      Token tk = new Token(user, ts);
      return Encrypter.encryptString(new String(tk.getToken()));
    } catch (JSchException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
      return null;
    }
  }
  private static final Map<String, String> roleMap;
  static
  {
      roleMap = new HashMap<>();
      roleMap.put("Rao", "TEACHER");
      roleMap.put("Thomas", "TEACHER");
      roleMap.put("TA-01", "TA");
  }

  private static class ItuAuthentication {
    public static long authenticate(String user, String password) throws JSchException {
      String host = "ssh.itu.dk";
      JSch jsch = new JSch();
      String fs = File.separator;
      jsch.setKnownHosts(System.getProperty("user.home")+fs+".ssh"+fs+"known_hosts");
      Session session;
      session = jsch.getSession(user, host, 22);
      session.setPassword(password);
      return System.currentTimeMillis();
    }
  }
  
  private static class Token {
    private String user;
    private long timeStamp;
    
    public Token(String user, long timeStamp){
      this.user = user;
      this.timeStamp = timeStamp;
    }
    
    public byte[] getToken(){
      try {
        return Encrypter.encryptString(getRole()+","+timeStamp, Encrypter.generateKeyFromString("TokenServerKey"));
      } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
        return null;
      }
    }
    
    private String getRole() {
      if (roleMap.containsKey(user)) return roleMap.get(user);
      else return "STUDENT";
    }
  }
  private static class Encrypter {
    private static final byte[] symKeyData = DatatypeConverter.parseHexBinary("ClientTokenKey");
    private static final SecretKeySpec desKey = new SecretKeySpec(symKeyData, "AES");
    
    private static SecretKey generateKeyFromString(String str) {
      final byte[] symKeyData = DatatypeConverter.parseHexBinary(str);
      final SecretKeySpec desKey = new SecretKeySpec(symKeyData, "AES");
      return desKey;
    }
    private static byte[] encryptString(String str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
      return encryptString(str, desKey);
    }
    private static byte[] encryptString(String str, SecretKey desKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
      Cipher desCipher;

      // Create the cipher 
      desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
      
      // Initialize the cipher for encryption
      desCipher.init(Cipher.ENCRYPT_MODE, desKey);

      // Our cleartext
      byte[] cleartext = str.getBytes();

      // Encrypt the cleartext
      byte[] ciphertext = desCipher.doFinal(cleartext);
      
      return ciphertext;
    }
    
    private static String decryptByteArray(byte[] arr) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
      Cipher desCipher;
      desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
      
      // Initialize the same cipher for decryption
      desCipher.init(Cipher.DECRYPT_MODE, desKey);

      // Decrypt the ciphertext
      return new String(desCipher.doFinal(arr));
    }
  }
}
