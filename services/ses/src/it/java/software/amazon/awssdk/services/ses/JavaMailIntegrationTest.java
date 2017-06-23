/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.ses;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.services.simpleemail.AwsJavaMailTransport;

/**
 * Integration test for JavaMail interface on top of AWS E-mail Service. Tests
 * by sending plain text email as well as multi-part MIME message with
 * attachments. Hard to test if the message actually was sent and received so
 * implicitly checks for a 200 response
 */
@Ignore("Requires manual verification of email and not suitable for pipeline")
public class JavaMailIntegrationTest extends IntegrationTestBase {

    private static final String ADDITIONAL_DESTINATION = DESTINATION; //HUDSON_EMAIL_LIST;
    private static final String CC = ADDITIONAL_DESTINATION;
    private static final String TEXT_EMAIL_BODY = "This is a test sending a text e-mail.";
    private static final String[] MULTI_DESTINATION = {DESTINATION,
                                                       DESTINATION, DESTINATION};
    protected static Session session;

    @BeforeClass
    public static void createSession() throws FileNotFoundException, IOException,
                                              NoSuchProviderException {
        sendVerificationEmail();

        // Get JavaMail Properties and Setup Session
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "aws");
        props.setProperty("mail.aws.user", credentials.accessKeyId());
        props.setProperty("mail.aws.password", credentials.secretAccessKey());
        props.setProperty("mail.debug", "true");
        session = Session.getInstance(props);
    }

    /**
     * Tests creating a new Transport object and sending two messages with given
     * credentials before closing
     */
    @Test
    public void testMultipleMessagesWithOneConnect() throws Exception {
        Transport t = new AwsJavaMailTransport(session, null);
        t.connect(credentials.accessKeyId(), credentials
                .secretAccessKey());
        Address[] a = {new InternetAddress(ADDITIONAL_DESTINATION)};
        t.sendMessage(getTestTextEmail(true), null);
        t.sendMessage(getTestMimeEmail(true), a);
        t.close();
    }

    /**
     * Tests sending a plain text e-mail with the defined SOURCE and DESTINATION
     * above.
     */
    public Message getTestTextEmail(boolean includeCC) throws Exception {
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(SOURCE));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(DESTINATION));

        if (includeCC) {
            msg.addRecipient(Message.RecipientType.CC, new InternetAddress(CC));
        }
        msg.setSubject("JavaMail Test");
        msg.setText(TEXT_EMAIL_BODY);
        msg.saveChanges();
        return msg;
    }

    /**
     * Tests sending a multi-part MIME message with the SOURCE and DESTINATION
     * fields being those set above.
     */
    public Message getTestMimeEmail(boolean includeCC) throws Exception {
        Message msg = new MimeMessage(session, loadRawMessageAsStream(
                RAW_MESSAGE_FILE_PATH));
        msg.setFrom(new InternetAddress(SOURCE));

        msg.setRecipient(RecipientType.TO, new InternetAddress(DESTINATION));
        if (includeCC) {
            msg.addRecipient(Message.RecipientType.CC, new InternetAddress(CC));
        }
        msg.saveChanges();
        return msg;
    }

    /**
     * Tests sending a multi-part MIME to multiple recipients with the SOURCE
     * and MULTI_DESTINATION fields specified above.
     */
    public Message testMimeMultiRecipientEmail(boolean includeCC)
            throws Exception {
        Message msg = new MimeMessage(session, loadRawMessageAsStream(
                RAW_MESSAGE_FILE_PATH));
        msg.setFrom(new InternetAddress(SOURCE));

        Address[] multiAddress = new Address[MULTI_DESTINATION.length];
        for (int i = 0; i < MULTI_DESTINATION.length; i++) {
            multiAddress[i] = new InternetAddress(MULTI_DESTINATION[i]);
        }
        msg.setRecipients(RecipientType.TO, multiAddress);
        if (includeCC) {
            msg.addRecipient(Message.RecipientType.CC, new InternetAddress(CC));
        }
        msg.saveChanges();
        return msg;
    }

}
