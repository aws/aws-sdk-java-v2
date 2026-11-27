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

package software.amazon.awssdk.s3.mockserver.testarchive.helpers;

import java.net.URL;
import java.time.Duration;
import java.util.function.Consumer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public final class PresignedUrls {

    private static final Duration VALIDITY = Duration.ofMinutes(15);

    private PresignedUrls() {
    }

    public static URL getObject(AwsCredentialsProvider credentials, Region region,
                                Consumer<GetObjectRequest.Builder> request) {
        try (S3Presigner presigner = S3Presigner.builder()
                .credentialsProvider(credentials)
                .region(region)
                .dualstackEnabled(false)
                .fipsEnabled(false)
                .build()) {
            return presigner.presignGetObject(p -> p.signatureDuration(VALIDITY).getObjectRequest(request)).url();
        }
    }
}
