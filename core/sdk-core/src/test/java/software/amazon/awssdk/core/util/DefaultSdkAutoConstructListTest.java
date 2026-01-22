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

package software.amazon.awssdk.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import org.junit.jupiter.api.Test;

public class DefaultSdkAutoConstructListTest {
    private static final DefaultSdkAutoConstructList<String> INSTANCE = DefaultSdkAutoConstructList.getInstance();

    @Test
    public void equals_emptyList() {
        assertThat(INSTANCE.equals(new LinkedList<>())).isTrue();
    }

    @Test
    public void hashCode_sameAsEmptyList() {
        assertThat(INSTANCE.hashCode()).isEqualTo(new LinkedList<>().hashCode());

        // The formula for calculating the hashCode is specified by the List
        // interface. For an empty list, it should be 1.
        assertThat(INSTANCE.hashCode()).isEqualTo(1);

    }

    @Test
    public void toString_emptyList() {
        assertThat(INSTANCE.toString()).isEqualTo("[]");
    }

    @Test
    public void serialization_sameSingletonInstance() throws Exception {
        DefaultSdkAutoConstructList<?> originalInstance = DefaultSdkAutoConstructList.getInstance();

        // Serialize the object
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
        objectOut.writeObject(originalInstance);
        objectOut.close();

        // Deserialize the object
        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream objectIn = new ObjectInputStream(byteIn);
        DefaultSdkAutoConstructList<?> deserializedInstance = (DefaultSdkAutoConstructList<?>) objectIn.readObject();
        objectIn.close();

        // Assert that deserialization was successful
        assertNotNull(deserializedInstance);
        assertSame(originalInstance, deserializedInstance, "Deserialized instance should be the same singleton instance");
    }
}
