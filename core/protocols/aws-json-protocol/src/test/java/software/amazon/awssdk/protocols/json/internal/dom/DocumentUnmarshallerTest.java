package software.amazon.awssdk.protocols.json.internal.dom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


import org.junit.Test;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.protocols.json.internal.unmarshall.document.DocumentUnmarshaller;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;


public class DocumentUnmarshallerTest {

    @Test
    public void testDocumentFromNumberNode() throws ParseException {

        SdkJsonNode sdkJsonNode = SdkScalarNode.createNumber(100);
        assertThat( Document.fromNumber(SdkNumber.fromString("100")))
                .isEqualTo(new DocumentUnmarshaller().visit(sdkJsonNode));

        SdkJsonNode sdkJsonNodeInt = SdkScalarNode.createNumber(100);
        assertThat( Document.fromNumber(SdkNumber.fromInteger(100)).asNumber().intValue())
                .isEqualTo(new DocumentUnmarshaller().visit(sdkJsonNodeInt).asNumber().intValue());

    }


    @Test
    public void testDocumentFromBoolean() {

        SdkJsonNode sdkScalarNode = SdkScalarNode.createBoolean(true);

        assertThat( Document.fromBoolean(true))
                .isEqualTo(new DocumentUnmarshaller().visit(sdkScalarNode));

    }

    @Test
    public void testDocumentFromString() {
        SdkJsonNode sdkScalarNode = SdkScalarNode.create("100.00");
        assertThat( Document.fromString("100.00"))
                .isEqualTo(new DocumentUnmarshaller().visit(sdkScalarNode));
    }

    @Test
    public void testDocumentFromNull() {
        assertThat( Document.fromNull())
                .isEqualTo(new DocumentUnmarshaller().visit(SdkNullNode.instance()));
    }


    @Test
    public void testExceptionIsThrownFromEmbededObjectType() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new DocumentUnmarshaller().visit(SdkEmbeddedObject.create(new HashMap<>())) );
    }


    @Test
    public void testDocumentFromObjectNode(){
        final SdkJsonNode sdkObjectNode = SdkObjectNode.builder().putField("firstKey", SdkScalarNode.create("firstValue"))
                .putField("secondKey", SdkScalarNode.create("secondValue")).build();
        final Document  documentMap = new DocumentUnmarshaller().visit(sdkObjectNode);
        Map<String, Document> expectedMap = new LinkedHashMap<>();
        expectedMap.put("firstKey", Document.fromString("firstValue"));
        expectedMap.put("secondKey", Document.fromString("secondValue"));
        final Document expectedDocumentMap = Document.fromMap(expectedMap);
        assertThat(documentMap).isEqualTo(expectedDocumentMap);
    }


    @Test
    public void testDocumentFromArrayNode(){
        final SdkArrayNode sdkArrayNode = SdkArrayNode.builder().addItem(SdkScalarNode.create("One")).addItem(SdkScalarNode.createNumber(10))
                .addItem(SdkScalarNode.createBoolean(true)).addItem(SdkNullNode.instance()).build();
        List<Document> documentList = new ArrayList<>();
        documentList.add(Document.fromString("One"));
        documentList.add(Document.fromNumber(SdkNumber.fromBigDecimal(BigDecimal.TEN)));
        documentList.add(Document.fromBoolean(true));
        documentList.add(Document.fromNull());
        final Document document = Document.fromList(documentList);
        final Document actualDocument = new DocumentUnmarshaller().visit(sdkArrayNode);
        assertThat(actualDocument).isEqualTo(document);

    }

}
