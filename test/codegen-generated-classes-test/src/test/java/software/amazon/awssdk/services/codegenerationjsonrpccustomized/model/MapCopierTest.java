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

package software.amazon.awssdk.services.codegenerationjsonrpccustomized.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import org.junit.Test;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

/**
 * Tests for generated map member copiers.
 */
public class MapCopierTest {
    @Test
    public void nullParamsAreCopiedAsAutoConstructedMap() {
        assertThat(MapOfStringToStringCopier.copy(null)).isInstanceOf(SdkAutoConstructMap.class);
    }

    @Test
    public void preservesAutoConstructedMapInput() {
        assertThat(MapOfStringToStringCopier.copy(DefaultSdkAutoConstructMap.getInstance())).isInstanceOf(SdkAutoConstructMap.class);
    }

    @Test
    public void explicitlyEmptyMapsAreNotCopiedAsAutoConstructed() {
        assertThat(MapOfStringToStringCopier.copy(new HashMap<>())).isNotInstanceOf(SdkAutoConstructMap.class);
    }
}
