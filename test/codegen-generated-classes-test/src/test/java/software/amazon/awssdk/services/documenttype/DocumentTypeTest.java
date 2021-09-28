package software.amazon.awssdk.services.documenttype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.documenttypejson.DocumentTypeJsonClient;
import software.amazon.awssdk.services.documenttypejson.model.AcceptHeader;
import software.amazon.awssdk.services.documenttypejson.model.AllTypesResponse;
import software.amazon.awssdk.services.documenttypejson.model.AllTypesWithPayloadResponse;
import software.amazon.awssdk.services.documenttypejson.model.ExplicitRecursivePayloadResponse;
import software.amazon.awssdk.services.documenttypejson.model.ImplicitNestedDocumentPayloadResponse;
import software.amazon.awssdk.services.documenttypejson.model.ImplicitOnlyDocumentPayloadResponse;
import software.amazon.awssdk.services.documenttypejson.model.ImplicitRecursivePayloadResponse;
import software.amazon.awssdk.services.documenttypejson.model.NestedDocumentPayload;
import software.amazon.awssdk.services.documenttypejson.model.RecursiveStructType;
import software.amazon.awssdk.services.documenttypejson.model.StringPayload;
import software.amazon.awssdk.services.documenttypejson.model.WithExplicitDocumentPayloadResponse;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class DocumentTypeTest {

    public static final Document STRING_DOCUMENT = Document.fromString("docsString");
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private DocumentTypeJsonClient jsonClient;

    private SdkHttpClient httpClient;

    @Before
    public void setup() throws IOException {
        httpClient = Mockito.mock(SdkHttpClient.class);
        jsonClient = initializeSync(DocumentTypeJsonClient.builder()).build();
    }

    private void setUpStub(String contentBody) {
        InputStream content = new StringInputStream(contentBody);
        SdkHttpFullResponse successfulHttpResponse = SdkHttpResponse.builder()
                                                                    .statusCode(200)
                                                                    .putHeader("accept", AcceptHeader.IMAGE_JPEG.toString())
                                                                    .putHeader("Content-Length",
                                                                               String.valueOf(contentBody.length()))
                                                                    .build();
        ExecutableHttpRequest request = Mockito.mock(ExecutableHttpRequest.class);
        try {
            Mockito.when(request.call()).thenReturn(HttpExecuteResponse.builder()
                                                                       .responseBody(AbortableInputStream.create(content))
                                                                       .response(successfulHttpResponse)
                                                                       .build());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Mockito.when(httpClient.prepareRequest(any())).thenReturn(request);
    }

    @Test
    public void implicitPayloadEmptyRequestMarshallingAndUnMarshalling() {
        setUpStub("{}");
        AllTypesResponse allTypesResponse = jsonClient.allTypes(SdkBuilder::build);
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo("{}");
        assertThat(allTypesResponse.myDocument()).isNull();
    }

    @Test
    public void implicitPayloadNonEmptyRequestMarshallingAndUnMarshalling() {
        setUpStub("{\"StringMember\":\"stringMember\",\"IntegerMember\":3,\"MyDocument\":\"stringDocument\"}");
        AllTypesResponse allTypesResponse = jsonClient.allTypes(
            c -> c.stringMember("stringMember").integerMember(3));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo("{\"StringMember\":\"stringMember\",\"IntegerMember\":3}");
        assertThat(allTypesResponse.stringMember()).isEqualTo("stringMember");
        assertThat(allTypesResponse.integerMember()).isEqualTo(3);
        assertThat(allTypesResponse.myDocument()).isEqualTo(Document.fromString("stringDocument"));
    }

    @Test
    public void explicitPayloadEmptyRequestMarshallingAndUnMarshalling() {
        setUpStub("{}");
        AllTypesWithPayloadResponse allTypesWithPayloadResponse =
            jsonClient.allTypesWithPayload(c -> c.stringPayload(StringPayload.builder().build()));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo("{}");
        assertThat(allTypesWithPayloadResponse.accept()).isEqualTo(AcceptHeader.IMAGE_JPEG);
        assertThat(allTypesWithPayloadResponse.stringPayload().stringMember()).isNull();
    }

    @Test
    public void explicitPayloadRequestMarshallingAndUnMarshalling() {
        String jsonFormat = "{\"StringMember\":\"payloadMember\"}";
        setUpStub(jsonFormat);
        AllTypesWithPayloadResponse payloadMember = jsonClient.allTypesWithPayload(
            c -> c.stringPayload(StringPayload.builder().stringMember("payloadMember").build()).build());
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        assertThat(payloadMember.accept()).isEqualTo(AcceptHeader.IMAGE_JPEG);
        assertThat(payloadMember.stringPayload().stringMember()).isEqualTo("payloadMember");
    }

    @Test
    public void implicitNestedPayloadWithSimpleDocument() {
        String jsonFormat = "{\"NestedDocumentPayload\":{\"StringMember\":\"stringMember\","
                            + "\"MyDocument\":\"stringDoc\"}}";
        setUpStub(jsonFormat);
        ImplicitNestedDocumentPayloadResponse implicitNestedDocumentPayload =
            jsonClient.implicitNestedDocumentPayload(
                c -> c.nestedDocumentPayload(
                    NestedDocumentPayload.builder()
                                         .myDocument(Document.fromString("stringDoc"))
                                         .stringMember("stringMember")
                                         .build()).build());
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        assertThat(implicitNestedDocumentPayload.nestedDocumentPayload().myDocument()).isEqualTo(Document.fromString("stringDoc"
        ));
        assertThat(implicitNestedDocumentPayload.nestedDocumentPayload().stringMember()).isEqualTo("stringMember");
        assertThat(implicitNestedDocumentPayload.accept()).isEqualTo(AcceptHeader.IMAGE_JPEG);
    }

    @Test
    public void implicitNestedPayloadWithDocumentMap() {
        String jsonFormat = "{\""
                            + "NestedDocumentPayload\":"
                            + "{\"StringMember\":\"stringMember\","
                            + "\"MyDocument\":"
                            + "{\"number\":2,\"stringValue\":\"string\"}}"
                            + "}";
        setUpStub(jsonFormat);
        Document document = Document.mapBuilder()
                                    .putNumber("number", SdkNumber.fromString("2"))
                                    .putString("stringValue", "string").build();
        ImplicitNestedDocumentPayloadResponse response = jsonClient.implicitNestedDocumentPayload(
            c -> c.nestedDocumentPayload(
                      NestedDocumentPayload.builder()
                                           .myDocument(document)
                                           .stringMember("stringMember").build())
                  .build());
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        assertThat(response.nestedDocumentPayload().stringMember()).isEqualTo("stringMember");
        assertThat(response.nestedDocumentPayload().myDocument()).isEqualTo(document);
        assertThat(response.accept()).isEqualTo(AcceptHeader.IMAGE_JPEG);
    }

    @Test
    public void explicitDocumentOnlyStringPayload() {
        String jsonFormat = "{\"MyDocument\":\"docsString\"}";
        setUpStub(jsonFormat);
        WithExplicitDocumentPayloadResponse response =
            jsonClient.withExplicitDocumentPayload(c -> c.myDocument(STRING_DOCUMENT).accept(AcceptHeader.IMAGE_JPEG));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(sdkHttpRequest.firstMatchingHeader("accept").get()).contains(AcceptHeader.IMAGE_JPEG.toString());
        assertThat(response.myDocument()).isEqualTo(STRING_DOCUMENT);
        assertThat(response.myDocument().isString()).isTrue();
    }

    @Test
    public void explicitDocumentOnlyListPayload() {
        String jsonFormat = "{\"MyDocument\":[{\"key\":\"value\"},null,\"string\",3,false]}";
        setUpStub(jsonFormat);
        Document document = Document.listBuilder()
                                    .addDocument(Document.mapBuilder().putString("key", "value").build())
                                    .addNull().addString("string").addNumber(3).addBoolean(false).build();
        WithExplicitDocumentPayloadResponse response =
            jsonClient.withExplicitDocumentPayload(c -> c.myDocument(document).accept(AcceptHeader.IMAGE_JPEG));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(sdkHttpRequest.firstMatchingHeader("accept").get()).contains(AcceptHeader.IMAGE_JPEG.toString());
        assertThat(response.myDocument()).isEqualTo(document);
        assertThat(response.myDocument().isList()).isTrue();
    }

    @Test
    public void explicitDocumentOnlyNullDocumentPayload() {
        String jsonFormat = "{\"MyDocument\":null}";
        setUpStub(jsonFormat);
        WithExplicitDocumentPayloadResponse response =
            jsonClient.withExplicitDocumentPayload(c -> c.myDocument(Document.fromNull()).accept(AcceptHeader.IMAGE_JPEG));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(sdkHttpRequest.firstMatchingHeader("accept").get()).contains(AcceptHeader.IMAGE_JPEG.toString());
        assertThat(response.myDocument()).isNotNull();
        assertThat(response.myDocument().isNull()).isTrue();
    }

    @Test
    public void explicitDocumentOnlyNullPayload() {
        String jsonFormat = "null";
        setUpStub(jsonFormat);
        WithExplicitDocumentPayloadResponse response =
            jsonClient.withExplicitDocumentPayload(c -> c.myDocument(null).accept(AcceptHeader.IMAGE_JPEG));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEmpty();
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(sdkHttpRequest.firstMatchingHeader("accept").get()).contains(AcceptHeader.IMAGE_JPEG.toString());
        assertThat(response.myDocument()).isNull();
    }

    @Test
    public void explicitDocumentOnlyEmptyPayload() {
        String jsonFormat = "";
        setUpStub(jsonFormat);
        WithExplicitDocumentPayloadResponse response =
            jsonClient.withExplicitDocumentPayload(c -> c.myDocument(null).accept(AcceptHeader.IMAGE_JPEG));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEmpty();
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(sdkHttpRequest.firstMatchingHeader("accept").get()).contains(AcceptHeader.IMAGE_JPEG.toString());
        assertThat(response.myDocument()).isNull();
    }


    @Test
    public void explicitDocumentOnlyPayloadWithMemberNameAsKeyNames() {
        String jsonFormat = "{\"MyDocument\":{\"MyDocument\":\"docsString\"}}";
        setUpStub(jsonFormat);
        Document document = Document.mapBuilder()
                                    .putDocument("MyDocument", STRING_DOCUMENT).build();
        WithExplicitDocumentPayloadResponse response =
            jsonClient.withExplicitDocumentPayload(c -> c.myDocument(document).accept(AcceptHeader.IMAGE_JPEG));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo("{\"MyDocument\":{\"MyDocument\":\"docsString\"}}");
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(sdkHttpRequest.firstMatchingHeader("accept").get()).contains(AcceptHeader.IMAGE_JPEG.toString());
        assertThat(response.myDocument()).isEqualTo(document);
        assertThat(response.myDocument().isMap()).isTrue();

    }


    @Test
    public void explicitPayloadWithRecursiveMember() {
        String jsonFormat = "{\"NoRecurse\":\"noRecursive1\",\"MyDocument\":\"level1\","
                            + "\"RecursiveStruct\":{\"NoRecurse\":\"noRecursive2\",\"MyDocument\":\"leve2\"}}";
        setUpStub(jsonFormat);

        ExplicitRecursivePayloadResponse response =
            jsonClient.explicitRecursivePayload(c -> c.recursiveStructType(
                                                          RecursiveStructType.builder()
                                                                             .myDocument(Document.fromString("level1"))
                                                                             .noRecurse("noRecursive1")

                                                                             .recursiveStruct(RecursiveStructType.builder().myDocument(Document.fromString("leve2")).noRecurse(
                                                                                 "noRecursive2").build())
                                                                             .build())
                                                      .registryName("registryName").accept(AcceptHeader.IMAGE_JPEG));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(sdkHttpRequest.firstMatchingHeader("accept").get()).contains(AcceptHeader.IMAGE_JPEG.toString());
        assertThat(response.registryName()).isNull();
        assertThat(response.accept()).isEqualTo(AcceptHeader.IMAGE_JPEG);
        assertThat(response.recursiveStructType().noRecurse()).isEqualTo("noRecursive1");
        assertThat(response.recursiveStructType().myDocument()).isEqualTo(Document.fromString("level1"));
        assertThat(response.recursiveStructType().recursiveStruct())
            .isEqualTo(RecursiveStructType.builder().noRecurse("noRecursive2").myDocument(Document.fromString("leve2")).build());
    }

    @Test
    public void explicitPayloadWithRecursiveMemberDocumentMap() {
        String jsonFormat = "{"
                            + "\"NoRecurse\":\"noRecursive1\","
                            + "\"MyDocument\":"
                            + "{\"docsL1\":\"docsStringL1\"},"
                            + "\"RecursiveStruct\":"
                            + "{\"NoRecurse\":\"noRecursive2\","
                            + "\"MyDocument\":"
                            + "{\"docsL2\":\"docsStringL2\"}"
                            + "}"
                            + "}";
        setUpStub(jsonFormat);
        Document documentOuter = Document.mapBuilder().putDocument("docsL1", Document.fromString("docsStringL1")).build();
        Document documentInner = Document.mapBuilder().putDocument("docsL2", Document.fromString("docsStringL2")).build();

        ExplicitRecursivePayloadResponse response =
            jsonClient.explicitRecursivePayload(
                c -> c.recursiveStructType(
                          RecursiveStructType.builder()
                                             .myDocument(documentOuter)
                                             .noRecurse("noRecursive1")
                                             .recursiveStruct(
                                                 RecursiveStructType.builder()
                                                                    .myDocument(documentInner)
                                                                    .noRecurse("noRecursive2")
                                                                    .build())
                                             .build())
                      .registryName("registryName")
                      .accept(AcceptHeader.IMAGE_JPEG)
            );
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(sdkHttpRequest.firstMatchingHeader("accept").get()).contains(AcceptHeader.IMAGE_JPEG.toString());
        assertThat(response.registryName()).isNull();
        assertThat(response.accept()).isEqualTo(AcceptHeader.IMAGE_JPEG);
        assertThat(response.recursiveStructType().noRecurse()).isEqualTo("noRecursive1");
        assertThat(response.recursiveStructType().myDocument()).isEqualTo(documentOuter);
        assertThat(response.recursiveStructType().recursiveStruct())
            .isEqualTo(RecursiveStructType.builder().noRecurse("noRecursive2").myDocument(documentInner).build());
    }

    @Test
    public void implicitPayloadWithRecursiveMemberDocumentMap() {
        String jsonFormat = "{\"MyDocument\":[1,null,\"end\"],\"MapOfStringToString\":{\"key1\":\"value1\","
                            + "\"key2\":\"value2\"},\"RecursiveStructType\":{\"NoRecurse\":\"noRecursive1\","
                            + "\"MyDocument\":{\"docsL1\":\"docsStringL1\"},"
                            + "\"RecursiveStruct\":{\"NoRecurse\":\"noRecursive2\","
                            + "\"MyDocument\":{\"docsL2\":\"docsStringL2\"}}}}";
        setUpStub(jsonFormat);
        Document documentOuter = Document.mapBuilder().putDocument("docsL1", Document.fromString("docsStringL1")).build();
        Document documentInner = Document.mapBuilder().putDocument("docsL2", Document.fromString("docsStringL2")).build();
        Document listDocument = Document.listBuilder().addNumber(1).addNull().addString("end").build();

        Map<String, String> stringStringMap = new HashMap<>();
        stringStringMap.put("key1", "value1");
        stringStringMap.put("key2", "value2");

        ImplicitRecursivePayloadResponse response = jsonClient.implicitRecursivePayload(
            c -> c.recursiveStructType(
                      RecursiveStructType.builder()
                                         .myDocument(documentOuter)
                                         .noRecurse("noRecursive1")
                                         .recursiveStruct(
                                             RecursiveStructType.builder()
                                                                .myDocument(documentInner)
                                                                .noRecurse("noRecursive2")
                                                                .build())
                                         .build())
                  .registryName("registryName")
                  .myDocument(listDocument)
                  .mapOfStringToString(stringStringMap)
                  .accept(AcceptHeader.IMAGE_JPEG)
        );
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(sdkHttpRequest.firstMatchingHeader("accept").get()).contains(AcceptHeader.IMAGE_JPEG.toString());
        assertThat(response.registryName()).isNull();
        assertThat(response.accept()).isEqualTo(AcceptHeader.IMAGE_JPEG);
        assertThat(response.recursiveStructType().noRecurse()).isEqualTo("noRecursive1");
        assertThat(response.recursiveStructType().myDocument()).isEqualTo(documentOuter);
        assertThat(response.recursiveStructType().recursiveStruct())
            .isEqualTo(RecursiveStructType.builder().noRecurse("noRecursive2").myDocument(documentInner).build());
        assertThat(response.myDocument()).isEqualTo(listDocument);

    }

    @Test
    public void implicitDocumentOnlyPayloadWithStringMember() {
        String jsonFormat = "{\"MyDocument\":\"stringDocument\"}";
        setUpStub(jsonFormat);
        Document document = Document.fromString("stringDocument");

        ImplicitOnlyDocumentPayloadResponse response =
            jsonClient.implicitOnlyDocumentPayload(c -> c.myDocument(document));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo("{\"MyDocument\":\"stringDocument\"}");
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(response.myDocument()).isEqualTo(document);
    }

    @Test
    public void implicitDocumentOnlyPayloadWithNullDocumentMember() {
        String jsonFormat = "{\"MyDocument\":null}";
        setUpStub(jsonFormat);
        Document document = Document.fromNull();

        ImplicitOnlyDocumentPayloadResponse response =
            jsonClient.implicitOnlyDocumentPayload(c -> c.myDocument(document));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(response.myDocument()).isNotNull();
        assertThat(response.myDocument()).isEqualTo(document);
    }

    @Test
    public void implicitDocumentOnlyPayloadWithNull() {
        String jsonFormat = "{}";
        setUpStub(jsonFormat);
        ImplicitOnlyDocumentPayloadResponse response =
            jsonClient.implicitOnlyDocumentPayload(c -> c.myDocument(null));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo("{}");
        SdkHttpRequest sdkHttpRequest = getSyncRequest();
        assertThat(response.myDocument()).isNull();
    }

    @Test
    public void implicitDocumentOnlyPayloadWithMapKeyAsMemberNames() {
        String jsonFormat = "{\"MyDocument\":{\"MyDocument\":\"stringDocument\"}}";
        setUpStub(jsonFormat);
        Document document = Document.mapBuilder()
                                    .putDocument("MyDocument", Document.fromString("stringDocument"))
                                    .build();

        ImplicitOnlyDocumentPayloadResponse response =
            jsonClient.implicitOnlyDocumentPayload(c -> c.myDocument(document));
        String syncRequest = getSyncRequestBody();
        assertThat(syncRequest).isEqualTo(jsonFormat);
        assertThat(response.myDocument()).isEqualTo(document);
    }

    private <T extends AwsSyncClientBuilder<T, ?> & AwsClientBuilder<T, ?>> T initializeSync(T syncClientBuilder) {
        return initialize(syncClientBuilder.httpClient(httpClient));
    }

    private <T extends AwsClientBuilder<T, ?>> T initialize(T clientBuilder) {
        return clientBuilder.credentialsProvider(AnonymousCredentialsProvider.create())
                            .region(Region.US_WEST_2);
    }

    private SdkHttpRequest getSyncRequest() {
        ArgumentCaptor<HttpExecuteRequest> captor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
        Mockito.verify(httpClient).prepareRequest(captor.capture());
        return captor.getValue().httpRequest();
    }

    private String getSyncRequestBody() {
        ArgumentCaptor<HttpExecuteRequest> captor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
        Mockito.verify(httpClient).prepareRequest(captor.capture());
        InputStream inputStream = captor.getValue().contentStreamProvider().get().newStream();
        return new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
    }
}