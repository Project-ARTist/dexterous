/*
 * Copyright (C) 2010 Ken Ellinwood
 *
 * Changes Copyright (C) 2017 CISPA (https://cispa.saarland), Saarland University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package saarland.cispa.apksigner.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import saarland.cispa.apksigner.sign.Base64;
import saarland.cispa.artist.log.Logg;
import trikita.log.Log;

public class PasswordObfuscator {

    private static final String TAG = Logg.TAG;

    private static PasswordObfuscator instance = null;
    
    static final String x = "harold-and-maude";   

    SecretKeySpec skeySpec;
    
    private PasswordObfuscator() {
        skeySpec = new SecretKeySpec(x.getBytes(), "AES");
    }

    public static PasswordObfuscator getInstance() {
        if (instance == null) instance = new PasswordObfuscator();
        return instance;
    }

    public String encodeKeystorePassword( String keystorePath, String password) {
        return encode( keystorePath, password);
    }

    public String encodeKeystorePassword( String keystorePath, char[] password) {
        return encode( keystorePath, password);
    }

    public String encodeAliasPassword( String keystorePath, String aliasName, String password) {
        return encode( keystorePath+aliasName, password);
    }

    public String encodeAliasPassword( String keystorePath, String aliasName, char[] password) {
        return encode( keystorePath+aliasName, password);
    }

    public char[] decodeKeystorePassword( String keystorePath, String password) {
        return decode(keystorePath,password);
    }

    public char[] decodeAliasPassword( String keystorePath, String aliasName, String password) {
        return decode(keystorePath+aliasName,password);
    }

    public String encode( String junk, String password) {
        if (password == null) return null;
        char[] c = password.toCharArray();
        String result = encode( junk, c);
        flush(c);
        return result;
    }

    public String encode( String junk, char[] password) {
        if (password == null) return null;
        try {
            // Instantiate the cipher
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer w = new OutputStreamWriter(baos);
            w.write(junk);
            w.write(password);
            w.flush();
            byte[] encoded = cipher.doFinal( baos.toByteArray());
            return Base64.encode( encoded);
        } catch (Exception x) {
            Log.e(TAG, "Failed to obfuscate password", x);
        }
        return null;
    }
    
    public char[] decode( String junk, String password) {
        if (password == null) return null;
        try {
            // Instantiate the cipher  
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec skeySpec = new SecretKeySpec(x.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] bytes = cipher.doFinal( Base64.decode(password.getBytes()));
            BufferedReader r = new BufferedReader( new InputStreamReader( new ByteArrayInputStream( bytes)));
            char[] cb = new char[128];
            int length = 0;
            int numRead;
            while ((numRead = r.read(cb, length, 128-length)) != -1) {
                length += numRead;
            }
            
            if (length <= junk.length()) return null;
            
            char[] result = new char[ length - junk.length()];
            int j = 0;
            for (int i = junk.length(); i < length; i++) {
                result[j] = cb[i];
                j += 1;
            }
            flush(cb);
            return result;
            
        } catch (Exception x) {
            Log.e(TAG, "Failed to decode password", x);
        }
        return null;
    }
    
    public static void flush( char[] charArray) {
        if (charArray == null) return;
        for (int i = 0; i < charArray.length; i++) {
            charArray[i] = '\0';
        }
    }

    public static void flush( byte[] charArray) {
        if (charArray == null) return;
        for (int i = 0; i < charArray.length; i++) {
            charArray[i] = 0;
        }
    }
}
