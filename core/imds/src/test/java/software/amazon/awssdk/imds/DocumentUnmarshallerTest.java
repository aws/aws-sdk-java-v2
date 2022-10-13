package software.amazon.awssdk.imds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.imds.internal.unmarshall.document.DocumentUnmarshaller;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.EmbeddedObjectJsonNode;


class DocumentUnmarshallerTest {
    @Test
    void testDocumentFromNumberNode() throws ParseException {
        JsonNode node = JsonNode.parser().parse("100");
        assertThat(Document.fromNumber(SdkNumber.fromInteger(100)).asNumber().intValue())
                .isEqualTo(node.visit(new DocumentUnmarshaller()).asNumber().intValue());
    }

    @Test
    void testDocumentFromBoolean() {
        JsonNode node = JsonNode.parser().parse("true");
        assertThat(Document.fromBoolean(true)).isEqualTo(node.visit(new DocumentUnmarshaller()));
    }

    @Test
    void testDocumentFromString() {
        JsonNode node = JsonNode.parser().parse("\"100.00\"");
        assertThat(Document.fromString("100.00")).isEqualTo(node.visit(new DocumentUnmarshaller()));
    }

    @Test
    void testDocumentFromNull() {
        JsonNode node = JsonNode.parser().parse("null");
        assertThat(Document.fromNull()).isEqualTo(node.visit(new DocumentUnmarshaller()));
    }

    @Test
    void testExceptionIsThrownFromEmbededObjectType() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> new EmbeddedObjectJsonNode(new Object()).visit(new DocumentUnmarshaller()));
    }

    @Test
    void testDocumentFromObjectNode(){
        JsonNode node = JsonNode.parser().parse("{\"firstKey\": \"firstValue\", \"secondKey\": \"secondValue\"}");

        Document documentMap = node.visit(new DocumentUnmarshaller());
        Map<String, Document> expectedMap = new LinkedHashMap<>();
        expectedMap.put("firstKey", Document.fromString("firstValue"));
        expectedMap.put("secondKey", Document.fromString("secondValue"));
        Document expectedDocumentMap = Document.fromMap(expectedMap);
        assertThat(documentMap).isEqualTo(expectedDocumentMap);
    }

    @Test
    void testDocumentFromArrayNode(){
        JsonNode node = JsonNode.parser().parse("[\"One\", 10, true, null]");
        List<Document> documentList = new ArrayList<>();
        documentList.add(Document.fromString("One"));
        documentList.add(Document.fromNumber(SdkNumber.fromBigDecimal(BigDecimal.TEN)));
        documentList.add(Document.fromBoolean(true));
        documentList.add(Document.fromNull());
        Document document = Document.fromList(documentList);
        Document actualDocument = node.visit(new DocumentUnmarshaller());
        assertThat(actualDocument).isEqualTo(document);
    }
}
