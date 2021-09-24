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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.transfer.s3.FailedUpload;

public class FailedUploadTest {

    @Test
    public void equalsHashcode() {
        SdkClientException exception = SdkClientException.create("test");
        Path path = Paths.get(".");
        FailedUpload failedUpload1 = DefaultFailedUpload.builder()
                                                        .exception(exception)
                                                        .path(path)
                                                        .build();

        FailedUpload failedUpload2 = DefaultFailedUpload.builder()
                                                               .exception(exception)
                                                               .path(path)
                                                               .build();

        FailedUpload failedUpload3 = DefaultFailedUpload.builder()
                                                               .exception(SdkClientException.create("blah"))
                                                               .path(Paths.get(".."))
                                                               .build();

        assertThat(failedUpload1).isEqualTo(failedUpload2);
        assertThat(failedUpload1.hashCode()).isEqualTo(failedUpload2.hashCode());
        assertThat(failedUpload1).isNotEqualTo(failedUpload3);
        assertThat(failedUpload1.hashCode()).isNotEqualTo(failedUpload3);
    }
}
