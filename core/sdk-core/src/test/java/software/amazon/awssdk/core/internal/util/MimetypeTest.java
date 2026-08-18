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

package software.amazon.awssdk.core.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.IoUtils;

public class MimetypeTest {

    private static final String MIME_TYPES_RESOURCE = "software/amazon/awssdk/core/util/mime.types";
    private static final String NATIVE_IMAGE_RESOURCE_CONFIG =
        "META-INF/native-image/software.amazon.awssdk/sdk-core/resource-config.json";

    private static Mimetype mimetype;

    @BeforeAll
    public static void setup() {
        mimetype = Mimetype.getInstance();
    }

    @Test
    public void extensionsWithCaps() throws Exception {
        assertThat(mimetype.getMimetype("image.JPeG")).isEqualTo("image/jpeg");
    }

    @Test
    public void extensionsWithUvi() throws Exception {
        assertThat(mimetype.getMimetype("test.uvvi")).isEqualTo("image/vnd.dece.graphic");
    }

    @Test
    public void unknownExtensions_defaulttoBeStream() throws Exception {
        assertThat(mimetype.getMimetype("test.unknown")).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void noExtensions_defaulttoBeStream() throws Exception {
        assertThat(mimetype.getMimetype("test")).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void pathWithoutFileName_defaulttoBeStream() throws Exception {
        Path mockPath = mock(Path.class);
        when(mockPath.getFileName()).thenReturn(null);
        assertThat(mimetype.getMimetype(mockPath)).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void nativeImageResourceConfig_includesMimeTypes() throws IOException {
        try (InputStream resourceConfig = Mimetype.class.getClassLoader().getResourceAsStream(NATIVE_IMAGE_RESOURCE_CONFIG)) {
            assertThat(resourceConfig).isNotNull();

            String resourceConfigContents = IoUtils.toUtf8String(resourceConfig);
            assertThat(resourceConfigContents).contains(MIME_TYPES_RESOURCE);
        }
    }
}
