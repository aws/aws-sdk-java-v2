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

import java.util.ArrayList;
import java.util.Collection;
import org.junit.Test;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

/**
 * Test for verifying list member behavior for model builders.
 */
public class ModelBuilderListMemberTest {
    @Test
    public void defaultConstructedModelsHaveInitialValue() {
        AllTypesRequest request = AllTypesRequest.builder().build();

        assertThat(request.listOfEnumsAsStrings()).isInstanceOf(SdkAutoConstructList.class);
    }

    @Test
    public void nullSetterCreatesSdkAutoConstructedList() {
        AllTypesRequest request = AllTypesRequest.builder()
                .listOfEnumsWithStrings((Collection<String>) null)
                .build();

        assertThat(request.listOfEnumsAsStrings()).isInstanceOf(SdkAutoConstructList.class);
    }

    @Test
    public void modelToBuilderRoundTripPreservesAutoConstructedLists() {
        AllTypesRequest request = AllTypesRequest.builder().build();
        AllTypesRequest roundTrip = request.toBuilder().build();

        assertThat(roundTrip.listOfEnumsAsStrings()).isInstanceOf(SdkAutoConstructList.class);
    }

    @Test
    public void modelToBuilderRoundTripPreservesExplicitEmptyLists() {
        AllTypesRequest request = AllTypesRequest.builder()
                .listOfEnums(new ArrayList<>())
                .build();
        AllTypesRequest roundTrip = request.toBuilder().build();

        assertThat(roundTrip.listOfEnumsAsStrings()).isNotInstanceOf(SdkAutoConstructList.class);
    }
}
