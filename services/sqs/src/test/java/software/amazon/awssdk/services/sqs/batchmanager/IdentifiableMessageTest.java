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

package software.amazon.awssdk.services.sqs.batchmanager;



import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.internal.batchmanager.IdentifiableMessage;

public class IdentifiableMessageTest {

    @Test
    public void createIdentifiableMessage() {
        String id = "id";
        String request = "request";
        IdentifiableMessage<String> myRequest = new IdentifiableMessage<>(id, request);
        Assert.assertEquals(id, myRequest.id());
        Assert.assertEquals(request, myRequest.message());
    }

    @Test
    public void checkIdenticalIdentifiableMessagesAreEqual() {
        String id = "id";
        String request = "request";
        IdentifiableMessage<String> myRequest1 = new IdentifiableMessage<>(id, request);
        IdentifiableMessage<String> myRequest2 = new IdentifiableMessage<>(id, request);
        Assert.assertEquals(myRequest1, myRequest2);
        Assert.assertEquals(myRequest1.hashCode(), myRequest2.hashCode());
    }

    @Test
    public void checkIdenticalIdentifiableMessagesAreNotEqual() {
        IdentifiableMessage<String> myRequest1 = new IdentifiableMessage<>("id1", "request1");
        IdentifiableMessage<String> myRequest2 = new IdentifiableMessage<>("id2", "request2");
        Assert.assertNotEquals(myRequest1, myRequest2);
        Assert.assertNotEquals(myRequest1.hashCode(), myRequest2.hashCode());
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(IdentifiableMessage.class)
                      .withNonnullFields("id", "message")
                      .withPrefabValues(IdentifiableMessage.class,
                                        new IdentifiableMessage<>("id1", "message1"),
                                        new IdentifiableMessage<>("id2", "message2"))
                      .verify();
    }
}

