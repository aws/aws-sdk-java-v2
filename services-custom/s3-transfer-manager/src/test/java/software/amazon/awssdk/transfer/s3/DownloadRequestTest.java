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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
    public void usingFile_null_shouldThrowException() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("destination");
        File file = null;
        DownloadRequest.builder()
                       .getObjectRequest(b -> b.bucket("bucket").key("key"))
                       .destination(file)
                       .build();

    }

    @Test
    public void equals_hashcode() {
        EqualsVerifier.forClass(DownloadRequest.class)
                      .withNonnullFields("destination", "getObjectRequest")
                      .verify();
    }
}
