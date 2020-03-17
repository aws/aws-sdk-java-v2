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

package software.amazon.awssdk.services.sns;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Utility for validating signatures on a Simple Notification Service JSON message.
 */
public class SignatureChecker {

    private static final String NOTIFICATION_TYPE = "Notification";
    private static final String SUBSCRIBE_TYPE = "SubscriptionConfirmation";
    private static final String UNSUBSCRIBE_TYPE = "UnsubscribeConfirmation";
    private static final String TYPE = "Type";
    private static final String SUBSCRIBE_URL = "SubscribeURL";
    private static final String MESSAGE = "Message";
    private static final String TIMESTAMP = "Timestamp";
    private static final String SIGNATURE_VERSION = "SignatureVersion";
    private static final String SIGNATURE = "Signature";
    private static final String MESSAGE_ID = "MessageId";
    private static final String SUBJECT = "Subject";
    private static final String TOPIC = "TopicArn";
    private static final String TOKEN = "Token";
    private static final Set<String> INTERESTING_FIELDS = new HashSet<>(Arrays.asList(TYPE, SUBSCRIBE_URL, MESSAGE, TIMESTAMP,
                                                                                      SIGNATURE, SIGNATURE_VERSION, MESSAGE_ID,
                                                                                      SUBJECT, TOPIC, TOKEN));
    private Signature sigChecker;

    /**
     * Validates the signature on a Simple Notification Service message. No
     * Amazon-specific dependencies, just plain Java crypto and Jackson for
     * parsing
     *
     * @param message
     *            A JSON-encoded Simple Notification Service message. Note: the
     *            JSON may be only one level deep.
     * @param publicKey
     *            The Simple Notification Service public key, exactly as you'd
     *            see it when retrieved from the cert.
     *
     * @return True if the message was correctly validated, otherwise false.
     */
    public boolean verifyMessageSignature(String message, PublicKey publicKey) {

        // extract the type and signature parameters
        Map<String, String> parsed = parseJson(message);

        return verifySignature(parsed, publicKey);
    }

    /**
     * Validates the signature on a Simple Notification Service message. No
     * Amazon-specific dependencies, just plain Java crypto
     *
     * @param parsedMessage
     *            A map of Simple Notification Service message.
     * @param publicKey
     *            The Simple Notification Service public key, exactly as you'd
     *            see it when retrieved from the cert.
     *
     * @return True if the message was correctly validated, otherwise false.
     */
    public boolean verifySignature(Map<String, String> parsedMessage, PublicKey publicKey) {
        boolean valid = false;
        String version = parsedMessage.get(SIGNATURE_VERSION);
        if (version.equals("1")) {
            // construct the canonical signed string
            String type = parsedMessage.get(TYPE);
            String signature = parsedMessage.get(SIGNATURE);
            String signed = "";
            if (type.equals(NOTIFICATION_TYPE)) {
                signed = stringToSign(publishMessageValues(parsedMessage));
            } else if (type.equals(SUBSCRIBE_TYPE)) {
                signed = stringToSign(subscribeMessageValues(parsedMessage));
            } else if (type.equals(UNSUBSCRIBE_TYPE)) {
                signed = stringToSign(subscribeMessageValues(parsedMessage)); // no difference, for now
            } else {
                throw new RuntimeException("Cannot process message of type " + type);
            }
            valid = verifySignature(signed, signature, publicKey);
        }
        return valid;
    }

    /**
     * Does the actual Java cryptographic verification of the signature. This
     * method does no handling of the many rare exceptions it is required to
     * catch.
     *
     * This can also be used to verify the signature from the x-amz-sns-signature http header
     *
     * @param message
     *            Exact string that was signed.  In the case of the x-amz-sns-signature header the
     *            signing string is the entire post body
     * @param signature
     *            Base64-encoded signature of the message
     */
    public boolean verifySignature(String message, String signature, PublicKey publicKey) {
        boolean result = false;
        byte[] sigbytes = null;
        try {
            sigbytes = BinaryUtils.fromBase64Bytes(signature.getBytes());
            sigChecker = Signature.getInstance("SHA1withRSA"); //check the signature
            sigChecker.initVerify(publicKey);
            sigChecker.update(message.getBytes());
            result = sigChecker.verify(sigbytes);
        } catch (NoSuchAlgorithmException e) {
            // Rare exception: JVM does not support SHA1 with RSA
        } catch (InvalidKeyException e) {
            // Rare exception: The private key was incorrectly formatted
        } catch (SignatureException e) {
            // Rare exception: Catch-all exception for the signature checker
        }
        return result;
    }

    protected String stringToSign(SortedMap<String, String> signables) {
        // each key and value is followed by a newline
        StringBuilder sb = new StringBuilder();
        for (String k : signables.keySet()) {
            sb.append(k).append("\n");
            sb.append(signables.get(k)).append("\n");
        }
        String result = sb.toString();
        return result;
    }

    private Map<String, String> parseJson(String jsonmessage) {
        Map<String, String> parsed = new HashMap<String, String>();
        JsonFactory jf = new JsonFactory();
        try {
            JsonParser parser = jf.createParser(jsonmessage);
            parser.nextToken(); //shift past the START_OBJECT that begins the JSON
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = parser.getCurrentName();
                if (!INTERESTING_FIELDS.contains(fieldname)) {
                    parser.skipChildren();
                    continue;
                }
                parser.nextToken(); // move to value, or START_OBJECT/START_ARRAY
                String value;
                if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
                    value = "";
                    boolean first = true;
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (!first) {
                            value += ",";
                        }
                        first = false;
                        value += parser.getText();
                    }
                } else {
                    value = parser.getText();
                }
                parsed.put(fieldname, value);
            }
        } catch (JsonParseException e) {
            // JSON could not be parsed
            e.printStackTrace();
        } catch (IOException e) {
            // Rare exception
        }
        return parsed;
    }

    private TreeMap<String, String> publishMessageValues(Map<String, String> parsedMessage) {
        TreeMap<String, String> signables = new TreeMap<String, String>();
        String[] keys = {MESSAGE, MESSAGE_ID, SUBJECT, TYPE, TIMESTAMP, TOPIC};
        for (String key : keys) {
            if (parsedMessage.containsKey(key)) {
                signables.put(key, parsedMessage.get(key));
            }
        }
        return signables;
    }

    private TreeMap<String, String> subscribeMessageValues(Map<String, String> parsedMessage) {
        TreeMap<String, String> signables = new TreeMap<String, String>();
        String[] keys = {SUBSCRIBE_URL, MESSAGE, MESSAGE_ID, TYPE, TIMESTAMP, TOKEN, TOPIC};
        for (String key : keys) {
            if (parsedMessage.containsKey(key)) {
                signables.put(key, parsedMessage.get(key));
            }
        }
        return signables;
    }
}
