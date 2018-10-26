package software.amazon.awssdk.protocols.query;


import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.stream.XMLStreamException;
import org.junit.Test;
import software.amazon.awssdk.protocols.query.XmlDomParser;
import software.amazon.awssdk.protocols.query.XmlElement;
import software.amazon.awssdk.utils.StringInputStream;

public class XmlDomParserTest {

    @Test
    public void simpleXmlDocument_ParsedCorrectly() throws XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                     + "<Struct>"
                     + " <stringMember>stringVal</stringMember>"
                     + " <integerMember>42</integerMember>"
                     + "</Struct>";
        XmlElement element = XmlDomParser.parse(new StringInputStream(xml));
        assertThat(element.attributes()).isEmpty();
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
    public void elementWithAttributes_ParsedCorrectly() throws XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                     + "<Struct attrOne=\"valOne\" attrTwo=\"valTwo\">"
                     + " <stringMember>stringVal</stringMember>"
                     + " <integerMember>42</integerMember>"
                     + "</Struct>";
        XmlElement element = XmlDomParser.parse(new StringInputStream(xml));
        assertThat(element.attributes()).hasSize(2);
        assertThat(element.attribute("attrOne")).isEqualTo("valOne");
        assertThat(element.attribute("attrTwo")).isEqualTo("valTwo");
    }

    @Test
    public void multipleElementsWithSameName_ParsedCorrectly() throws XMLStreamException {
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

    @Test(expected = XMLStreamException.class)
    public void invalidXml_ThrowsException() throws XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                     + "<Struct>"
                     + " <member>valOne"
                     + " <member>valTwo</member>"
                     + "</Struct>";
        XmlDomParser.parse(new StringInputStream(xml));
    }

}