/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.acm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.acm.model.AcmException;
import software.amazon.awssdk.services.acm.model.GetCertificateRequest;
import software.amazon.awssdk.services.acm.model.ListCertificatesRequest;
import software.amazon.awssdk.services.acm.model.ListCertificatesResponse;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class AwsCertficateManagerAsyncIntegrationTest extends AwsIntegrationTestBase {
    private static AcmAsyncClient client;

    @BeforeClass
    public static void setUp() {
        client = AcmAsyncClient.builder().credentialsProvider(getCredentialsProvider()).build();
    }

    @Test
    public void list_certificates() {
        ListCertificatesResponse result = client.listCertificates(ListCertificatesRequest.builder().build())
                                                .join();
        Assert.assertTrue(result.certificateSummaryList().size() >= 0);
    }

    @Test
    public void get_certificate_fake_arn_throws_exception() {
        assertThatThrownBy(() -> client.getCertificate(getCertificateRequest()).join())
            .hasCauseInstanceOf(AcmException.class);
    }

    private GetCertificateRequest getCertificateRequest() {
        return GetCertificateRequest.builder()
                                    .certificateArn("arn:aws:acm:us-east-1:123456789:fakecert")
                                    .build();
    }
}
