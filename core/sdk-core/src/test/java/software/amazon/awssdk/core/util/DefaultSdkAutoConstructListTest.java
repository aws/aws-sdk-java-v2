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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("DefaultSdkAutoConstruct is Serializable, and is same instance.")
    public void serialization_sameSingletonInstance() throws Exception {
        // Create instance of DefaultSdkAutoConstructList
        List defaultSdkAutoConstructList = DefaultSdkAutoConstructList.getInstance();

        // Serialize instance into byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(baos);

        objectOutputStream.writeObject(defaultSdkAutoConstructList);
        objectOutputStream.flush();
        objectOutputStream.close();

        // Serialization result
        byte[] bytes = baos.toByteArray();

        // Deserialize bytes
        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        // Deserialized result
        DefaultSdkAutoConstructList resultList = (DefaultSdkAutoConstructList) objectInputStream.readObject();
        objectInputStream.close();

        // Compare using "==", to make sure it is the same reference.
        assertThat(resultList == defaultSdkAutoConstructList).isTrue();
    }
}
