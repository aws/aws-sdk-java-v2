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

package software.amazon.awssdk.transfer.s3.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.reactivex.Flowable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.internal.ListObjectsHelper;

public class S3ApiCallMockUtils {

    private S3ApiCallMockUtils() {
    }

    public static void stubSuccessfulListObjects(ListObjectsHelper helper, String... keys) {
        List<S3Object> s3Objects = Arrays.stream(keys).map(k -> S3Object.builder().key(k).build()).collect(Collectors.toList());
        when(helper.listS3ObjectsRecursively(any(ListObjectsV2Request.class))).thenReturn(SdkPublisher.adapt(Flowable.fromIterable(s3Objects)));
    }

}
