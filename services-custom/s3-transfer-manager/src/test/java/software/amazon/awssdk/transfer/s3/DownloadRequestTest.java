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

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class DownloadRequestTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void noGetObjectRequest_throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("getObjectRequest");

        DownloadRequest.builder()
                       .destination(Paths.get("."))
                       .build();
    }

    @Test
    public void pathMissing_throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("destination");

        DownloadRequest.builder()
                       .getObjectRequest(b -> b.bucket("bucket").key("key"))
                       .build();
    }

    @Test
    public void usingFile() {
        Path path = Paths.get(".");
        DownloadRequest requestUsingFile = DownloadRequest.builder()
                                                          .getObjectRequest(b -> b.bucket("bucket").key("key"))
                                                          .destination(path.toFile())
                                                          .build();

        assertThat(requestUsingFile.destination()).isEqualTo(path);
    }

    @Test
    public void equals_hashcode() {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket("bucket")
                                                            .key("key")
                                                            .build();

        DownloadRequest request1 = DownloadRequest.builder()
                                                 .getObjectRequest(b -> b.bucket("bucket").key("key"))
                                                 .destination(Paths.get("."))
                                                 .build();

        DownloadRequest request2 = DownloadRequest.builder()
                                                  .getObjectRequest(getObjectRequest)
                                                  .destination(Paths.get("."))
                                                  .build();

        DownloadRequest request3 = DownloadRequest.builder()
                                                  .getObjectRequest(b -> b.bucket("bucket1").key("key1"))
                                                  .destination(Paths.get("."))
                                                  .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());

        assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode());
        assertThat(request1).isNotEqualTo(request3);
    }
}
