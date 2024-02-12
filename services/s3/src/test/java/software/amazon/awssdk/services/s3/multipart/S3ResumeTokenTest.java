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

package software.amazon.awssdk.services.s3.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class S3ResumeTokenTest {

    private static final String UPLOAD_ID = "uploadId";
    private static final long PART_SIZE = 99;
    private static final long TOTAL_NUM_PARTS = 20;
    private static final long NUM_PARTS_COMPLETED = 2;

    @Test
    public void s3ResumeToken_withValues_buildsCorrectly() {
        S3ResumeToken token = S3ResumeToken.builder()
                                           .uploadId(UPLOAD_ID)
                                           .partSize(PART_SIZE)
                                           .totalNumParts(TOTAL_NUM_PARTS)
                                           .numPartsCompleted(NUM_PARTS_COMPLETED)
                                           .build();

        assertThat(token.uploadId()).isEqualTo(UPLOAD_ID);
        assertThat(token.partSize()).isEqualTo(PART_SIZE);
        assertThat(token.totalNumParts()).isEqualTo(TOTAL_NUM_PARTS);
        assertThat(token.numPartsCompleted()).isEqualTo(NUM_PARTS_COMPLETED);
    }

    @Test
    public void s3ResumeToken_default_buildsCorrectly() {
        S3ResumeToken token = S3ResumeToken.builder().build();

        assertThat(token.uploadId()).isNull();
        assertThat(token.partSize()).isZero();
        assertThat(token.totalNumParts()).isZero();
        assertThat(token.numPartsCompleted()).isZero();
    }

    @Test
    void testEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(S3ResumeToken.class);
    }
}
