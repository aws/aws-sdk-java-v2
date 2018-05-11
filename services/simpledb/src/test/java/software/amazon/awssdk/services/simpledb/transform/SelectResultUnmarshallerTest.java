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

package software.amazon.awssdk.services.simpledb.transform;

import static org.junit.Assert.assertTrue;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import org.junit.Test;
import software.amazon.awssdk.awscore.protocol.xml.StaxUnmarshallerContext;
import software.amazon.awssdk.services.simpledb.model.Item;
import software.amazon.awssdk.services.simpledb.model.SelectResponse;
import software.amazon.awssdk.utils.XmlUtils;

public class SelectResultUnmarshallerTest {

    /**
     * Test method for SelectResponseUnmarshaller
     */
    @Test
    public final void testUnmarshall() throws Exception {
        XMLInputFactory xmlInputFactory = XmlUtils.xmlInputFactory();
        XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(DomainMetadataResultUnmarshallerTest.class
                                                                                  .getResourceAsStream("SelectResponse.xml"));
        StaxUnmarshallerContext unmarshallerContext = new StaxUnmarshallerContext(eventReader);
        SelectResponse result = new SelectResponseUnmarshaller().unmarshall(unmarshallerContext);

        assertTrue(!result.items().isEmpty());
        assertTrue(result.items().size() == 2);
        assertTrue(((Item) result.items().get(0)).name().equals("ItemOne"));
        assertTrue(((Item) result.items().get(1)).name().equals("ItemTwo"));
    }

}
