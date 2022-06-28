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

package software.amazon.awssdk.imds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


/**
 * Unit Tests to test the Ec2Metadata Client functionality
 */
@RunWith(MockitoJUnitRunner.class)
public class Ec2MetadataTest {

    @Mock
    private Ec2Metadata ec2Metadata;


    @Test
    public void when_dummy_string_is_returned(){

        when(ec2Metadata.get("/ami-id")).thenReturn("IMDS");
        assertThat(ec2Metadata.get("/ami-id")).isEqualTo("IMDS");

    }

    @Test
    public void verify_equals_hashcode(){

        EqualsVerifier.forClass(Ec2Metadata.class)
            .usingGetClass()
            .verify();
    }

}
