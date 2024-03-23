package main.java.Managers;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class SecurityManager {
    private KeyPair rsaKeyPair;

    public KeyPair getRsaKeyPair() {
        return rsaKeyPair;
    }

    public void generateRSAKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            rsaKeyPair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String encryptMessageWithAES(String message, String recipientPublicKey) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey aesKey = keyGen.generateKey();
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // Generate IV
            byte[] iv = new byte[aesCipher.getBlockSize()];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);

            byte[] byteCipherText = aesCipher.doFinal(message.getBytes());
            String encryptedMessage = Base64.getEncoder().encodeToString(byteCipherText);
            String ivString = Base64.getEncoder().encodeToString(iv);

            // Encrypt AES key met recipient's public key
            String encryptedAESKey = encryptAESKey(aesKey, recipientPublicKey);

            // Return encrypted IV, message en encrypted AES key
            return ivString + ":" + encryptedMessage + ":" + encryptedAESKey;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public String encryptAESKey(SecretKey aesKey, String publicKeyString) {
        try {
            byte[] key = aesKey.getEncoded();
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString));
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedKey = cipher.doFinal(key);
            return Base64.getEncoder().encodeToString(encryptedKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String decryptMessageWithAES(String encryptedData) {
        try {
            String[] parts = encryptedData.split(":", 3);
            if (parts.length < 3) {
                return "Invalid encrypted data format";
            }
            String ivString = parts[0];
            String encryptedMessage = parts[1];
            String encryptedAESKey = parts[2];

            // Decrypt  AES key
            SecretKey aesKey = decryptAESKey(encryptedAESKey);

            // decrypt met IV
            IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder().decode(ivString));

            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);

            byte[] decryptedMessageBytes = aesCipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
            return new String(decryptedMessageBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during decryption";
        }
    }


    public SecretKey decryptAESKey(String encryptedKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
            byte[] decryptedKey = cipher.doFinal(Base64.getDecoder().decode(encryptedKey));
            return new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
