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

package software.amazon.awssdk.services.iam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.iam.model.LimitExceededException;
import software.amazon.awssdk.services.iam.model.ListSigningCertificatesRequest;
import software.amazon.awssdk.services.iam.model.ListSigningCertificatesResponse;
import software.amazon.awssdk.services.iam.model.MalformedCertificateException;
import software.amazon.awssdk.services.iam.model.SigningCertificate;
import software.amazon.awssdk.services.iam.model.StatusType;
import software.amazon.awssdk.services.iam.model.UpdateSigningCertificateRequest;
import software.amazon.awssdk.services.iam.model.UploadSigningCertificateRequest;
import software.amazon.awssdk.services.iam.model.UploadSigningCertificateResponse;

/**
 * Certs integ tests for IAM.
 */
public class CertsIntegrationTest extends IntegrationTestBase {

    // This cert should be good through 2039
    public static final String SAMPLE_CERT = "-----BEGIN CERTIFICATE-----\n"
                                             + "MIIBxzCCAXGgAwIBAgIQEkj5aN2xhrVIIrVD1oz7rjANBgkqhkiG9w0BAQQFADAW\n"
                                             + "MRQwEgYDVQQDEwtSb290IEFnZW5jeTAeFw0xMDA4MTgyMzQ2MjdaFw0zOTEyMzEy\n"
                                             + "MzU5NTlaMCIxIDAeBgNVBAMTF0pvZSdzLVNvZnR3YXJlLUVtcG9yaXVtMIGfMA0G\n"
                                             + "CSqGSIb3DQEBAQUAA4GNADCBiQKBgQDqe8B8wH9jasG4z2C3d5UQ1igvYuBWy5sG\n"
                                             + "FhBypu3w7k0E+AivOAGZ+/Cbjcz4ONR8jbKDutHiRDM/rSU+2lwnZszfEOeeKglg\n"
                                             + "6+pPN12Ysqvv8krBgUEx0sfke+3igYT0rFFnTGOH7Dwz4BSdnZf6XNhP5S8mUIgj\n"
                                             + "rfb+cKrVeQIDAQABo0swSTBHBgNVHQEEQDA+gBAS5AktBh0dTwCNYSHcFmRjoRgw\n"
                                             + "FjEUMBIGA1UEAxMLUm9vdCBBZ2VuY3mCEAY3bACqAGSKEc+41KpcNfQwDQYJKoZI\n"
                                             + "hvcNAQEEBQADQQB/Ssqf9vNxK+0H/9L/O/wFOpdh3XEDTTQEnhcdhjyxETxj64La\n"
                                             + "6RMhYRZQVa4+8YS6KeJSaSJvItx6piaCU1cv\n"
                                             + "-----END CERTIFICATE-----";

    public static final String SAMPLE_CERT_2 = "-----BEGIN CERTIFICATE-----\n"
                                               + "MIIBxzCCAXGgAwIBAgIQ2FdZSWDq1rNEni+n6Z8cAzANBgkqhkiG9w0BAQQFADAW\n"
                                               + "MRQwEgYDVQQDEwtSb290IEFnZW5jeTAeFw0xMDA4MTkxNzQzMTlaFw0zOTAxMDEw\n"
                                               + "NzAwMDBaMCIxIDAeBgNVBAMTF0pvZSdzLVNvZnR3YXJlLUVtcG9yaXVtMIGfMA0G\n"
                                               + "CSqGSIb3DQEBAQUAA4GNADCBiQKBgQDqe8B8wH9jasG4z2C3d5UQ1igvYuBWy5sG\n"
                                               + "FhBypu3w7k0E+AivOAGZ+/Cbjcz4ONR8jbKDutHiRDM/rSU+2lwnZszfEOeeKglg\n"
                                               + "6+pPN12Ysqvv8krBgUEx0sfke+3igYT0rFFnTGOH7Dwz4BSdnZf6XNhP5S8mUIgj\n"
                                               + "rfb+cKrVeQIDAQABo0swSTBHBgNVHQEEQDA+gBAS5AktBh0dTwCNYSHcFmRjoRgw\n"
                                               + "FjEUMBIGA1UEAxMLUm9vdCBBZ2VuY3mCEAY3bACqAGSKEc+41KpcNfQwDQYJKoZI\n"
                                               + "hvcNAQEEBQADQQAT0EUwsPvVVfehxAlhrAlaKnmvN9n0lFl5UCBrL+MhOBbTj73n\n"
                                               + "yETygusot/kWtHcUyjrNPQCcsxOS6z7nw7ux\n"
                                               + "-----END CERTIFICATE-----";
    public static final String SAMPLE_CERT_3 = "-----BEGIN CERTIFICATE-----\n"
                                               + "MIIBxzCCAXGgAwIBAgIQIaRf4jjxEbpCNVLNJUWCSjANBgkqhkiG9w0BAQQFADAW\n"
                                               + "MRQwEgYDVQQDEwtSb290IEFnZW5jeTAeFw0xMDA4MTkxNzQzNTNaFw0zOTAxMDIw\n"
                                               + "NzAwMDBaMCIxIDAeBgNVBAMTF0pvZSdzLVNvZnR3YXJlLUVtcG9yaXVtMIGfMA0G\n"
                                               + "CSqGSIb3DQEBAQUAA4GNADCBiQKBgQDqe8B8wH9jasG4z2C3d5UQ1igvYuBWy5sG\n"
                                               + "FhBypu3w7k0E+AivOAGZ+/Cbjcz4ONR8jbKDutHiRDM/rSU+2lwnZszfEOeeKglg\n"
                                               + "6+pPN12Ysqvv8krBgUEx0sfke+3igYT0rFFnTGOH7Dwz4BSdnZf6XNhP5S8mUIgj\n"
                                               + "rfb+cKrVeQIDAQABo0swSTBHBgNVHQEEQDA+gBAS5AktBh0dTwCNYSHcFmRjoRgw\n"
                                               + "FjEUMBIGA1UEAxMLUm9vdCBBZ2VuY3mCEAY3bACqAGSKEc+41KpcNfQwDQYJKoZI\n"
                                               + "hvcNAQEEBQADQQARssaUFQT70XhJ0ZrffwaaOXVbjCKDWmcTnC528cI5mY/kHiEd\n"
                                               + "HXKBzpT1KrkFdIMiPOXpYJExVxWuxnHSBzZR\n"
                                               + "-----END CERTIFICATE-----";

    public static final String SAMPLE_CERT_4 = "-----BEGIN CERTIFICATE-----\n"
                                               + "MIIBxzCCAXGgAwIBAgIQHq0Uh7fqDpRCgRQAa6OgBTANBgkqhkiG9w0BAQQFADAW\n"
                                               + "MRQwEgYDVQQDEwtSb290IEFnZW5jeTAeFw0xMDA4MTkxNzQ0MzVaFw0zOTAxMDQw\n"
                                               + "NzAwMDBaMCIxIDAeBgNVBAMTF0pvZSdzLVNvZnR3YXJlLUVtcG9yaXVtMIGfMA0G\n"
                                               + "CSqGSIb3DQEBAQUAA4GNADCBiQKBgQDqe8B8wH9jasG4z2C3d5UQ1igvYuBWy5sG\n"
                                               + "FhBypu3w7k0E+AivOAGZ+/Cbjcz4ONR8jbKDutHiRDM/rSU+2lwnZszfEOeeKglg\n"
                                               + "6+pPN12Ysqvv8krBgUEx0sfke+3igYT0rFFnTGOH7Dwz4BSdnZf6XNhP5S8mUIgj\n"
                                               + "rfb+cKrVeQIDAQABo0swSTBHBgNVHQEEQDA+gBAS5AktBh0dTwCNYSHcFmRjoRgw\n"
                                               + "FjEUMBIGA1UEAxMLUm9vdCBBZ2VuY3mCEAY3bACqAGSKEc+41KpcNfQwDQYJKoZI\n"
                                               + "hvcNAQEEBQADQQAYIaxPDCcSvHVkPc5SROzd5IzbejBlzfFCaUACK8LSmq3Gfq5d\n"
                                               + "Db+F77OhEBBKqncCoqwTH8jUxRxKB8ybwxSp\n"
                                               + "-----END CERTIFICATE-----";

    // This cert expired on 1/1/2000
    public static final String EXPIRED_CERT = "-----BEGIN CERTIFICATE-----\n"
                                              + "MIIBxzCCAXGgAwIBAgIQQF+iYmHGE6hPj6Tf3CHqaTANBgkqhkiG9w0BAQQFADAW\n"
                                              + "MRQwEgYDVQQDEwtSb290IEFnZW5jeTAeFw0xMDA4MTgyMzUzNDJaFw0wMDAxMDEw\n"
                                              + "NzAwMDBaMCIxIDAeBgNVBAMTF0pvZSdzLVNvZnR3YXJlLUVtcG9yaXVtMIGfMA0G\n"
                                              + "CSqGSIb3DQEBAQUAA4GNADCBiQKBgQDqe8B8wH9jasG4z2C3d5UQ1igvYuBWy5sG\n"
                                              + "FhBypu3w7k0E+AivOAGZ+/Cbjcz4ONR8jbKDutHiRDM/rSU+2lwnZszfEOeeKglg\n"
                                              + "6+pPN12Ysqvv8krBgUEx0sfke+3igYT0rFFnTGOH7Dwz4BSdnZf6XNhP5S8mUIgj\n"
                                              + "rfb+cKrVeQIDAQABo0swSTBHBgNVHQEEQDA+gBAS5AktBh0dTwCNYSHcFmRjoRgw\n"
                                              + "FjEUMBIGA1UEAxMLUm9vdCBBZ2VuY3mCEAY3bACqAGSKEc+41KpcNfQwDQYJKoZI\n"
                                              + "hvcNAQEEBQADQQBZjsXyVeff7mp1yhOGEa6Y+sBn6e8biiFKRTnno/R4L2pSAwWs\n"
                                              + "XL7/9vKazVTnQq0eRiKxHQSDdwgaIs096puA\n"
                                              + "-----END CERTIFICATE-----";

    @Before
    public void testSetup() {
        IAMUtil.deleteUsersAndGroupsInTestNameSpace();
    }

    @Test
    public void testUploadCertificate() {
        String username = IAMUtil.createTestUser();

        try {
            UploadSigningCertificateResponse response = iam
                    .uploadSigningCertificate(UploadSigningCertificateRequest.builder()
                                                                             .userName(username).certificateBody(
                                    SAMPLE_CERT).build());

            assertEquals(username, response.certificate().userName());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = MalformedCertificateException.class)
    public void testMalformedCertificateException() {
        String username = IAMUtil.createTestUser();

        try {
            iam.uploadSigningCertificate(UploadSigningCertificateRequest.builder()
                                                                        .userName(username).certificateBody(EXPIRED_CERT)
                                                                        .build());

        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void testListCerts() {
        String username = IAMUtil.createTestUser();
        String[] certs = {SAMPLE_CERT, SAMPLE_CERT_2};
        String[] certId = new String[2];

        try {
            for (int i = 0; i < 2; i++) {
                UploadSigningCertificateResponse response = iam
                        .uploadSigningCertificate(UploadSigningCertificateRequest.builder()
                                                                                 .userName(username).certificateBody(
                                        certs[i]).build());

                certId[i] = response.certificate().certificateId();
            }

            ListSigningCertificatesResponse listRes = iam
                    .listSigningCertificates(ListSigningCertificatesRequest.builder()
                                                                           .userName(username).build());

            assertEquals(2, listRes.certificates().size());
            assertFalse(listRes.isTruncated());

            int matches = 0;

            for (SigningCertificate cert : listRes.certificates()) {
                for (int i = 0; i < 2; i++) {
                    if (certId[i].equals(cert.certificateId())) {
                        matches |= 1 << i;
                    }
                }
            }

            assertEquals(3, matches);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = LimitExceededException.class)
    public void testLimitExceededException() {
        String username = IAMUtil.createTestUser();
        String[] certs = {SAMPLE_CERT, SAMPLE_CERT_2, SAMPLE_CERT_3};

        try {
            for (int i = 0; i < 3; i++) {
                iam.uploadSigningCertificate(UploadSigningCertificateRequest.builder()
                                                                            .userName(username).certificateBody(certs[i])
                                                                            .build());
            }
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void testUpdateCert() {
        String username = IAMUtil.createTestUser();

        try {
            UploadSigningCertificateResponse response = iam
                    .uploadSigningCertificate(UploadSigningCertificateRequest.builder()
                                                                             .userName(username).certificateBody(
                                    SAMPLE_CERT).build());

            String certId = response.certificate().certificateId();
            assertEquals(StatusType.ACTIVE, response.certificate().status());

            iam.updateSigningCertificate(UpdateSigningCertificateRequest.builder()
                                                                        .userName(username).certificateId(certId)
                                                                        .status(StatusType.INACTIVE).build());

            ListSigningCertificatesResponse listRes = iam
                    .listSigningCertificates(ListSigningCertificatesRequest.builder()
                                                                           .userName(username).build());

            assertEquals(StatusType.INACTIVE, listRes.certificates().iterator().next().status());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }
}
