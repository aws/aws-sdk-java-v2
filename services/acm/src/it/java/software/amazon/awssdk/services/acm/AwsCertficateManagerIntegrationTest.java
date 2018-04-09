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

package software.amazon.awssdk.services.acm;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.acm.model.GetCertificateRequest;
import software.amazon.awssdk.services.acm.model.ListCertificatesRequest;
import software.amazon.awssdk.services.acm.model.ListCertificatesResponse;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class AwsCertficateManagerIntegrationTest extends AwsIntegrationTestBase {

    private static ACMClient client;

    @BeforeClass
    public static void setUp() {
        client = ACMClient.builder().credentialsProvider(StaticCredentialsProvider.create(getCredentials())).build();
    }

    @Test
    public void list_certificates() {
        ListCertificatesResponse result = client.listCertificates(ListCertificatesRequest.builder().build());
        Assert.assertTrue(result.certificateSummaryList().size() >= 0);
    }

    /**
     * Ideally the service must be throwing a Invalid Arn exception
     * instead of SdkServiceException. Have reported this to service to
     * fix it.
     *  TODO Change the expected when service fix this.
     */
    @Test(expected = SdkServiceException.class)
    public void get_certificate_fake_arn_throws_exception() {
        client.getCertificate(GetCertificateRequest.builder().certificateArn("arn:aws:acm:us-east-1:123456789:fakecert").build());
    }


}
