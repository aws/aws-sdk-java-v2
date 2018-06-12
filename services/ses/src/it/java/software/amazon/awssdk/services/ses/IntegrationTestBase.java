/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.ses.model.ListVerifiedEmailAddressesRequest;
import software.amazon.awssdk.services.ses.model.ListVerifiedEmailAddressesResponse;
import software.amazon.awssdk.services.ses.model.VerifyEmailAddressRequest;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Base class for AWS Email integration tests; responsible for loading AWS account credentials for
 * running the tests, instantiating clients, etc.
 */
public abstract class IntegrationTestBase extends AwsTestBase {

    public static final String HUDSON_EMAIL_LIST = "no-reply@amazon.com";
    protected static final String RAW_MESSAGE_FILE_PATH = "/software/amazon/awssdk/services/email/rawMimeMessage.txt";
    public static String DESTINATION;
    public static String SOURCE;
    protected static SesClient email;

    /**
     * Loads the AWS account info for the integration tests and creates client objects for tests to
     * use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();

        if (DESTINATION == null) {
            DESTINATION = System.getProperty("user.name").equals("webuser") ? HUDSON_EMAIL_LIST :
                    System.getProperty("user.name") + "@amazon.com";
            SOURCE = DESTINATION;
        }

        email = SesClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    protected static void sendVerificationEmail() {
        ListVerifiedEmailAddressesResponse verifiedEmails =
                email.listVerifiedEmailAddresses(ListVerifiedEmailAddressesRequest.builder().build());
        for (String email : verifiedEmails.verifiedEmailAddresses()) {
            if (email.equals(DESTINATION)) {
                return;
            }
        }

        email.verifyEmailAddress(VerifyEmailAddressRequest.builder().emailAddress(DESTINATION).build());
        fail("Please check your email and verify your email address.");
    }

    protected String loadRawMessage(String messagePath) throws Exception {
        String rawMessage = IOUtils.toString(getClass().getResourceAsStream(messagePath));
        rawMessage = rawMessage.replace("@DESTINATION@", DESTINATION);
        rawMessage = rawMessage.replace("@SOURCE@", SOURCE);
        return rawMessage;
    }

    protected InputStream loadRawMessageAsStream(String messagePath) throws Exception {
        return IOUtils.toInputStream(loadRawMessage(messagePath));
    }
}
