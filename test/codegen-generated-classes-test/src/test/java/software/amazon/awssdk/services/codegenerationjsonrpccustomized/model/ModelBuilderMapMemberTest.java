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

package software.amazon.awssdk.services.codegenerationjsonrpccustomized.model;

import org.junit.Test;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for verifying map member behavior for model builders.
 */
public class ModelBuilderMapMemberTest {
    @Test
    public void defaultConstructedModelsHaveInitialValue() {
        AllTypesRequest request = AllTypesRequest.builder().build();

        assertThat(request.mapOfStringToString()).isInstanceOf(SdkAutoConstructMap.class);
    }

    @Test
    public void nullSetterCreatesSdkAutoConstructedMap() {
        AllTypesRequest request = AllTypesRequest.builder()
                .mapOfStringToString(null)
                .build();

        assertThat(request.mapOfStringToString()).isInstanceOf(SdkAutoConstructMap.class);
    }

    @Test
    public void modelToBuilderRoundTripPreservesAutoConstructedMaps() {
        AllTypesRequest request = AllTypesRequest.builder().build();
        AllTypesRequest roundTrip = request.toBuilder().build();

        assertThat(roundTrip.mapOfStringToString()).isInstanceOf(SdkAutoConstructMap.class);
    }

    @Test
    public void modelToBuilderRoundTripPreservesExplicitEmptyMaps() {
        AllTypesRequest request = AllTypesRequest.builder()
                .mapOfStringToString(new HashMap<>())
                .build();
        AllTypesRequest roundTrip = request.toBuilder().build();

        assertThat(roundTrip.mapOfStringToString()).isNotInstanceOf(SdkAutoConstructMap.class);
    }
}
