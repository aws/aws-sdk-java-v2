/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

public class MimetypeTest {

    private static Mimetype mimetype;

    @BeforeClass
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
}
