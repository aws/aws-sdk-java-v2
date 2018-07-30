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
import org.junit.Test;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

/**
 * Tests for generated list member copiers.
 */
public class ListCopierTest {
    @Test
    public void nullParamsAreCopiedAsAutoConstructedList() {
        assertThat(ListOfStringsCopier.copy(null)).isInstanceOf(SdkAutoConstructList.class);
    }

    @Test
    public void preservesAutoConstructedListInput() {
        assertThat(ListOfStringsCopier.copy(DefaultSdkAutoConstructList.getInstance())).isInstanceOf(SdkAutoConstructList.class);
    }

    @Test
    public void explicitlyEmptyListsAreNotCopiedAsAutoConstructed() {
        assertThat(ListOfStringsCopier.copy(new ArrayList<>())).isNotInstanceOf(SdkAutoConstructList.class);
    }
}
