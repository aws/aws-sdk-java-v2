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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.auth.signer.SdkClock;
import software.amazon.awssdk.services.s3.AwsS3V4Signer;

public class AwsS3V4SignerBuilderTest {

    @Test
    public void testS3Signer() {
        AwsS3V4Signer signer = AwsS3V4Signer.builder()
                                            .disableChunkedEncoding(true)
                                            .enablePayloadSigning(true)
                                            .doubleUrlEncode(false)
                                            .clock(SdkClock.STANDARD)
                                            .build();

        assertThat(signer.disableChunkedEncoding()).isEqualTo(true);
        assertThat(signer.enablePayloadSigning()).isEqualTo(true);
        assertThat(signer.doubleUrlEncode()).isEqualTo(false);
        assertThat(signer.clock()).isEqualTo(SdkClock.STANDARD);
    }
}
