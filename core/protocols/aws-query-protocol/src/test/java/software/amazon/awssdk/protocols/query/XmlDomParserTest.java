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

package software.amazon.awssdk.protocols.query;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.xml.stream.XMLStreamException;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.query.unmarshall.XmlDomParser;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;
import software.amazon.awssdk.utils.StringInputStream;

public class XmlDomParserTest {

    @Test
    public void simpleXmlDocument_ParsedCorrectly() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                     + "<Struct>"
                     + " <stringMember>stringVal</stringMember>"
                     + " <integerMember>42</integerMember>"
                     + "</Struct>";
        XmlElement element = XmlDomParser.parse(new StringInputStream(xml));
        assertThat(element.elementName()).isEqualTo("Struct");
        assertThat(element.children()).hasSize(2);
        assertThat(element.getElementsByName("stringMember"))
            .hasSize(1);
        assertThat(element.getElementsByName("stringMember").get(0).textContent())
            .isEqualTo("stringVal");
        assertThat(element.getElementsByName("integerMember"))
            .hasSize(1);
        assertThat(element.getElementsByName("integerMember").get(0).textContent())
            .isEqualTo("42");
    }

    @Test
    public void xmlWithAttributes_ParsedCorrectly() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                     + "<Struct xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"foo\" xsi:nil=\"bar\">"
                     + " <stringMember>stringVal</stringMember>"
                     + "</Struct>";
        XmlElement element = XmlDomParser.parse(new StringInputStream(xml));
        assertThat(element.elementName()).isEqualTo("Struct");
        assertThat(element.children()).hasSize(1);
        assertThat(element.getElementsByName("stringMember"))
            .hasSize(1);
        assertThat(element.attributes()).hasSize(2);
        assertThat(element.getOptionalAttributeByName("xsi:type").get()).isEqualTo("foo");
        assertThat(element.getOptionalAttributeByName("xsi:nil").get()).isEqualTo("bar");
    }

    @Test
    public void multipleElementsWithSameName_ParsedCorrectly() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                     + "<Struct>"
                     + " <member>valOne</member>"
                     + " <member>valTwo</member>"
                     + "</Struct>";
        XmlElement element = XmlDomParser.parse(new StringInputStream(xml));
        assertThat(element.getElementsByName("member"))
            .hasSize(2);
        assertThat(element.getElementsByName("member").get(0).textContent())
            .isEqualTo("valOne");
        assertThat(element.getElementsByName("member").get(1).textContent())
            .isEqualTo("valTwo");
    }

    @Test
    public void invalidXml_ThrowsException() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                     + "<Struct>"
                     + " <member>valOne"
                     + " <member>valTwo</member>"
                     + "</Struct>";
        assertThatThrownBy(() -> XmlDomParser.parse(new StringInputStream(xml)))
            .isInstanceOf(SdkClientException.class)
            .hasCauseInstanceOf(XMLStreamException.class);
    }

}
