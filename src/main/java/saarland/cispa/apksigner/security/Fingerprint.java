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

import org.spongycastle.util.encoders.HexTranslator;

import java.security.MessageDigest;

import saarland.cispa.apksigner.sign.Base64;
import saarland.cispa.artist.log.Logg;
import trikita.log.Log;

/**
 * User: ken
 * Date: 1/17/13
 */
public class Fingerprint {


    private static final String TAG = Logg.TAG;

    static byte[] calcDigest(String algorithm, byte[] encodedCert) {
        byte[] result = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.update(encodedCert);
            result = messageDigest.digest();
        } catch (Exception x) {
            Log.e(TAG, x.getMessage(), x);
        }
        return result;
    }

    public static String hexFingerprint( String algorithm, byte[] encodedCert) {
        try {
            byte[] digest = calcDigest(algorithm,encodedCert);
            if (digest == null) return null;
            HexTranslator hexTranslator = new HexTranslator();
            byte[] hex = new byte[digest.length * 2];
            hexTranslator.encode(digest, 0, digest.length, hex, 0);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < hex.length; i += 2) {
                builder.append((char)hex[i]);
                builder.append((char)hex[i+1]);
                if (i != (hex.length - 2)) builder.append(':');
            }
            return builder.toString().toUpperCase();
        } catch (Exception x) {
            Log.e(TAG, x.getMessage(),x);
        }
        return null;
    }

//    public static void main(String[] args) {
//        byte[] data = "The Silence of the Lambs is a really good movie.".getBytes();
//        System.out.println(hexFingerprint("MD5", data));
//        System.out.println(hexFingerprint("SHA1", data));
//        System.out.println(base64Fingerprint("SHA1", data));
//
//    }

    public static String base64Fingerprint( String algorithm, byte[] encodedCert) {
        String result = null;
        try {
            byte[] digest = calcDigest(algorithm,encodedCert);
            if (digest == null) return result;
            return Base64.encode(digest);
        } catch (Exception x) {
            Log.e(TAG, x.getMessage(),x);
        }
        return result;
    }
}
