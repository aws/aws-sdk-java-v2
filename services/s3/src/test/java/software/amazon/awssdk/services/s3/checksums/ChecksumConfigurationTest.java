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

package software.amazon.awssdk.services.s3.checksums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

public class ChecksumConfigurationTest {

    @Test
    public void s3Client_requestChecksumCalculationAndChecksumValidationEnabledBothSet_throwsError() {
        S3ClientBuilder clientBuilder = S3Client.builder()
                                                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                                                .serviceConfiguration(S3Configuration.builder().checksumValidationEnabled(false).build());

        Exception exception = assertThrows(IllegalStateException.class, clientBuilder::build);
        assertThat(exception.getMessage())
            .contains("Checksum behavior has been configured on both S3Configuration and the client/global level");
    }
    @Test
    public void s3Client_responseChecksumValidationAndChecksumValidationEnabledBothSet_throwsError() {
        S3ClientBuilder clientBuilder = S3Client.builder()
                                                .responseChecksumValidation(ResponseChecksumValidation.WHEN_SUPPORTED)
                                                .serviceConfiguration(S3Configuration.builder().checksumValidationEnabled(true).build());

        Exception exception = assertThrows(IllegalStateException.class, clientBuilder::build);
        assertThat(exception.getMessage())
            .contains("Checksum behavior has been configured on both S3Configuration and the client/global level");
    }
}
