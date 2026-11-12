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

package software.amazon.awssdk.s3.mockserver.testarchive.downloadStreams;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.s3.mockserver.client.ConfiguredClient;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.HttpClientFeature.AllNonCrt;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.RetryFeature;
import software.amazon.awssdk.s3.mockserver.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.mockserver.testarchive.ChecksumModeOption;
import software.amazon.awssdk.s3.mockserver.testarchive.Feature;
import software.amazon.awssdk.s3.mockserver.testarchive.StoredObjectTest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.IoUtils;

public class EveryTransportGetTest extends AbstractGetBodyTest<ConfiguredClient> {

    private static final Reader<ConfiguredClient> READER = (c, request) -> {
        if (c.asyncClient() == null) {
            try (ResponseInputStream<GetObjectResponse> in = c.client().getObject(request)) {
                return IoUtils.toByteArray(in);
            }
        }
        return c.asyncClient().getObject(request, AsyncResponseTransformer.toBytes()).join().asByteArray();
    };

    @StoredObjectTest(shapes = { StoredObject.Shape.SINGLE_PUT_CRC32, StoredObject.Shape.MPU_CRC32_COMPOSITE })
    @Feature(feature = HttpClientFeature.class, group = AllNonCrt.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    void wholeObjectOnEveryTransport(ConfiguredClient c, StoredObject object, ChecksumModeOption checksumMode) {
        wholeObject(c, object, checksumMode, READER);
    }
}
