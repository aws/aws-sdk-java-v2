/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.cloudfront.internal.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class Pem {

    private static final String BEGIN_MARKER = "-----BEGIN ";
    private static final Pattern BEGIN = Pattern.compile("BEGIN", Pattern.LITERAL);

    private Pem() {
    }

    /**
     * Returns the first private key that is found from the input stream of a
     * PEM file.
     *
     * @throws InvalidKeySpecException
     *             if failed to convert the DER bytes into a private key.
     * @throws IllegalArgumentException
     *             if no private key is found.
     */
    public static PrivateKey readPrivateKey(InputStream is) throws InvalidKeySpecException, IOException {
        List<PemObject> objects = readPemObjects(is);
        for (PemObject object : objects) {
            switch (object.getPemObjectType()) {
                case PRIVATE_KEY_PKCS1:
                    return Rsa.privateKeyFromPkcs1(object.getDerBytes());
                case PRIVATE_KEY_PKCS8:
                    return Rsa.privateKeyFromPkcs8(object.getDerBytes());
                default:
                    break;
            }
        }
        throw new IllegalArgumentException("Found no private key");
    }

    /**
     * Returns the first public key that is found from the input stream of a PEM
     * file.
     *
     * @throws InvalidKeySpecException
     *             if failed to convert the DER bytes into a public key.
     * @throws IllegalArgumentException
     *             if no public key is found.
     */
    public static PublicKey readPublicKey(InputStream is) throws InvalidKeySpecException, IOException {
        List<PemObject> objects = readPemObjects(is);
        for (PemObject object : objects) {
            if (object.getPemObjectType() == PemObjectType.PUBLIC_KEY_X509) {
                return Rsa.publicKeyFrom(object.getDerBytes());
            }
        }
        throw new IllegalArgumentException("Found no public key");
    }

    /**
     * A lower level API used to returns all PEM objects that can be read off
     * from the input stream of a PEM file.
     * <p>
     * This method can be useful if more than one PEM object of different types
     * are embedded in the same PEM file.
     */
    public static List<PemObject> readPemObjects(InputStream is) throws IOException {
        List<PemObject> pemContents = new ArrayList<>();
        /*
         * State of reading: set to true if reading content between a
         * begin-marker and end-marker; false otherwise.
         */
        boolean readingContent = false;
        String beginMarker = null;
        String endMarker = null;
        StringBuilder sb = null;
        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            while ((line = reader.readLine()) != null) {
                if (readingContent) {
                    if (line.contains(endMarker)) {
                        pemContents.add(new PemObject(beginMarker, Base64.getDecoder().decode(sb.toString())));
                        readingContent = false;
                    } else {
                        sb.append(line.trim());
                    }
                } else {
                    if (line.contains(BEGIN_MARKER)) {
                        readingContent = true;
                        beginMarker = line.trim();
                        endMarker = BEGIN.matcher(beginMarker).replaceAll("END");
                        sb = new StringBuilder();
                    }
                }
            }
            return pemContents;
        }
    }
}
