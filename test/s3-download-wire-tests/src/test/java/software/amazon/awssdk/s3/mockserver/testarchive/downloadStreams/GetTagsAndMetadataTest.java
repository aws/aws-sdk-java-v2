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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.s3.mockserver.client.ConfiguredClient;
import software.amazon.awssdk.s3.mockserver.client.Payload;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.HttpClientFeature.CrtCreate;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.HttpClientFeature.NettyNio;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.MultipartFeature;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.RetryFeature;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.s3.mockserver.server.S3RequestFilters;
import software.amazon.awssdk.s3.mockserver.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.mockserver.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.mockserver.testarchive.ClientTest;
import software.amazon.awssdk.s3.mockserver.testarchive.Feature;
import software.amazon.awssdk.s3.mockserver.testarchive.S3TestBase;
import software.amazon.awssdk.s3.mockserver.testarchive.helpers.Downloads;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class GetTagsAndMetadataTest extends S3TestBase {

    private static final String KEY = "download-streams/tags-and-metadata";
    private static final int EIGHT_MIB = 8 * 1024 * 1024;

    private static final Map<String, String> TAGS = Immutable.mapOf("project", "wiretap", "stage", "four");
    private static final Map<String, String> METADATA = ordered("origin", "wiretap", "Mixed-Case", "kept");
    private static final Map<String, String> METADATA_AS_READ = ordered("origin", "wiretap", "mixed-case", "kept");

    private static final StoredObject OBJECT =
            StoredObject.multipartCrc32Composite(3 * EIGHT_MIB, EIGHT_MIB).withTags(TAGS).withMetadata(METADATA);

    @ClientTest
    @Feature(feature = HttpClientFeature.class, values = { NettyNio.class, CrtCreate.class })
    @Feature(feature = MultipartFeature.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    void toBytes(ConfiguredClient c) {
        GetMockResponders.register(wiretap(), KEY, OBJECT);
        ResponseBytes<GetObjectResponse> bytes = c.asyncClient().getObject(b -> b.bucket(bucket()).key(KEY),
                AsyncResponseTransformer.toBytes()).join();
        assertArrayEquals(OBJECT.payload().toByteArray(), bytes.asByteArray(), "the object's bytes");
        assertReported(c, bytes.response());
    }

    @ClientTest
    @Feature(feature = HttpClientFeature.class, values = { NettyNio.class, CrtCreate.class })
    @Feature(feature = MultipartFeature.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    void toFile(ConfiguredClient c) throws Exception {
        GetMockResponders.register(wiretap(), KEY, OBJECT);
        Path file = Downloads.freshFile("wiretap-get-tags-");
        try {
            GetObjectResponse response = c.asyncClient().getObject(b -> b.bucket(bucket()).key(KEY),
                    AsyncResponseTransformer.toFile(file)).join();
            assertArrayEquals(OBJECT.payload().sha256(), Payload.of(Files.readAllBytes(file)).sha256(),
                    "the object's bytes");
            assertReported(c, response);
        } finally {
            Payload.deleteQuietly(file);
        }
    }

    private static Map<String, String> ordered(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private void assertReported(ConfiguredClient c, GetObjectResponse response) {
        awaitWireQuiescence();
        int gets = wiretap().requestsMatching(S3RequestFilters.getObject()).size();
        boolean splits = isManagedCrt(c.asyncClient()) || c.value(MultipartFeature.class) != null;
        if (splits) {
            assertTrue(gets > 1, () -> "the premise: this client splits a 24 MiB read, saw " + gets + " GET(s)");
        } else {
            assertEquals(1, gets, "a plain client reads in one GET");
        }
        assertEquals(Integer.valueOf(TAGS.size()), response.tagCount(),
                "the response reports the object's tag count");
        assertEquals(METADATA_AS_READ, response.metadata(), "the response reports the object's user metadata");
        assertSettledRequestCount(wiretap().requestCount());
    }
}
