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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.junit.Test;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class SnsSignatureCheckerTest extends AwsTestBase {

    @Test
    public void validateMessageTest() throws URISyntaxException, IOException, CertificateException {
        final String jsonMessage = getResourceAsString(getClass(), SnsTestResources.SAMPLE_MESSAGE);
        SignatureChecker checker = new SignatureChecker();
        assertTrue(checker.verifyMessageSignature(jsonMessage, getPublicKey()));
    }

    private PublicKey getPublicKey() throws URISyntaxException, IOException, CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf
                .generateCertificate(getClass().getResourceAsStream(SnsTestResources.FIXED_PUBLIC_CERT));
        return cert.getPublicKey();
    }
}
