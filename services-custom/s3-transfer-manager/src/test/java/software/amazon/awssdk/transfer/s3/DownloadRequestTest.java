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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class DownloadRequestTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void noGetObjectRequest_throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("getObjectRequest");

        DownloadRequest.builder()
                       .responseTransformer(AsyncResponseTransformer.toBytes())
                       .build();
    }

    @Test
    public void usingFile() {
        AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> responseTransformer =
            AsyncResponseTransformer.toBytes();
        
        DownloadRequest requestUsingFile = DownloadRequest.builder()
                                                          .getObjectRequest(b -> b.bucket("bucket").key("key"))
                                                          .responseTransformer(responseTransformer)
                                                          .build();

        assertThat(requestUsingFile.responseTransformer()).isEqualTo(responseTransformer);
    }


    @Test
    public void null_responseTransformer_shouldThrowException() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("responseTransformer");

        DownloadRequest.builder()
                       .getObjectRequest(b -> b.bucket("bucket").key("key"))
                       .responseTransformer(null)
                       .build();

    }

    @Test
    public void equals_hashcode() {
        EqualsVerifier.forClass(DownloadRequest.class)
                      .withNonnullFields("responseTransformer", "getObjectRequest")
                      .verify();
    }
}
