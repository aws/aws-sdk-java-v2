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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.Pair;

public class MpuRequestContextTest {

    private static final Pair<PutObjectRequest, AsyncRequestBody> REQUEST = Pair.of(PutObjectRequest.builder().build(), AsyncRequestBody.empty());
    private static final long CONTENT_LENGTH = 999;
    private static final long PART_SIZE = 111;
    private static final long NUM_PARTS_COMPLETED = 3;
    private static final String UPLOAD_ID = "55555";
    private static final Map<Integer, CompletedPart> EXISTING_PARTS = new ConcurrentHashMap<>();

    @Test
    public void mpuRequestContext_withValues_buildsCorrectly() {
        MpuRequestContext mpuRequestContext = MpuRequestContext.builder()
                                                               .request(REQUEST)
                                                               .contentLength(CONTENT_LENGTH)
                                                               .partSize(PART_SIZE)
                                                               .uploadId(UPLOAD_ID)
                                                               .existingParts(EXISTING_PARTS)
                                                               .numPartsCompleted(NUM_PARTS_COMPLETED)
                                                               .build();

        assertThat(mpuRequestContext.request()).isEqualTo(REQUEST);
        assertThat(mpuRequestContext.contentLength()).isEqualTo(CONTENT_LENGTH);
        assertThat(mpuRequestContext.partSize()).isEqualTo(PART_SIZE);
        assertThat(mpuRequestContext.uploadId()).isEqualTo(UPLOAD_ID);
        assertThat(mpuRequestContext.existingParts()).isEqualTo(EXISTING_PARTS);
        assertThat(mpuRequestContext.numPartsCompleted()).isEqualTo(NUM_PARTS_COMPLETED);
    }

    @Test
    public void mpuRequestContext_default_buildsCorrectly() {
        MpuRequestContext mpuRequestContext = MpuRequestContext.builder().build();

        assertThat(mpuRequestContext.request()).isNull();
        assertThat(mpuRequestContext.contentLength()).isNull();
        assertThat(mpuRequestContext.partSize()).isNull();
        assertThat(mpuRequestContext.uploadId()).isNull();
        assertThat(mpuRequestContext.existingParts()).isNull();
        assertThat(mpuRequestContext.numPartsCompleted()).isNull();
    }

    @Test
    void testEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(MpuRequestContext.class);
    }
}
