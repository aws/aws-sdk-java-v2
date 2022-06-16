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

package software.amazon.awssdk.protocols.imds;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.internal.bytebuddy.matcher.ElementMatchers.is;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test Class which is used to test various methods of the Ec2Metadata Class.
 */
@ExtendWith(MockitoExtension.class)
public class Ec2MetadataTest {

    @Mock
    public Ec2Metadata client;

    /**
     * Test to check the functionality of get Method .
     */
    @Test
    public void getTest(){

        when(client.get("/ami-id")).thenReturn("IMDS");
        assertThat(client.get("/ami-id")).isEqualTo("IMDS");
    }

}
