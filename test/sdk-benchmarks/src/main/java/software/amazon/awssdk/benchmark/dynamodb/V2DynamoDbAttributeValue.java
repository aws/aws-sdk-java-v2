package software.amazon.awssdk.benchmark.dynamodb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BackupInUseException;
import software.amazon.awssdk.services.dynamodb.model.BackupNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ContinuousBackupsUnavailableException;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalTableAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.GlobalTableNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.IndexNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import software.amazon.awssdk.services.dynamodb.model.InvalidRestoreTimeException;
import software.amazon.awssdk.services.dynamodb.model.ItemCollectionSizeLimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.LimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.PointInTimeRecoveryUnavailableException;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReplicaAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.ReplicaNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.TableInUseException;
import software.amazon.awssdk.services.dynamodb.model.TableNotFoundException;
import software.amazon.awssdk.services.dynamodb.transform.PutItemRequestMarshaller;


public class V2DynamoDbAttributeValue {

    @Benchmark
    public Object putItem(PutItemState s) {
        return putItemRequestMarshaller().marshall(s.getReq());
    }

    @Benchmark
    public Object getItem(GetItemState s) throws Exception {
        SdkHttpFullResponse resp = fullResponse(s.testItem);
        return getItemResponseJsonResponseHandler().handle(resp, new ExecutionAttributes());
    }

    @State(Scope.Benchmark)
    public static class PutItemState {
        @Param( {"TINY", "SMALL", "HUGE"})
        private TestItem testItem;

        private PutItemRequest req;

        @Setup
        public void setup() {
            req = PutItemRequest.builder().item(testItem.getValue()).build();
        }

        public PutItemRequest getReq() {
            return req;
        }
    }

    @State(Scope.Benchmark)
    public static class GetItemState {
        @Param( {"TINY", "SMALL", "HUGE"})
        private TestItemUnmashalling testItem;
    }

    public enum TestItem {
        TINY,
        SMALL,
        HUGE;

        private static final AbstractItemFactory<AttributeValue> factory = new V2ItemFactory();

        private Map<String, AttributeValue> av;

        static {
            TINY.av = factory.tiny();
            SMALL.av = factory.small();
            HUGE.av = factory.huge();
        }

        public Map<String, AttributeValue> getValue() {
            return av;
        }
    }

    public enum TestItemUnmashalling {
        TINY,
        SMALL,
        HUGE;

        private byte[] utf8;

        static {
            TINY.utf8 = toUtf8ByteArray(TestItem.TINY.av);
            SMALL.utf8 = toUtf8ByteArray(TestItem.SMALL.av);
            HUGE.utf8 = toUtf8ByteArray(TestItem.HUGE.av);
        }

        public byte[] utf8() {
            return utf8;
        }
    }

    private SdkHttpFullResponse fullResponse(TestItemUnmashalling item) {
        AbortableInputStream abortableInputStream = AbortableInputStream.create(new ByteArrayInputStream(item.utf8()));
        return SdkHttpFullResponse.builder()
                                  .statusCode(200)
                                  .content(abortableInputStream)
                                  .build();
    }

    private static byte[] toUtf8ByteArray(Map<String, AttributeValue> item) {
        Request<?> marshalled = putItemRequestMarshaller().marshall(PutItemRequest.builder().item(item).build());
        InputStream content = marshalled.getContentStreamProvider().get().newStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buff[] = new byte[8192];
        int read;
        try {
            while ((read = content.read(buff)) != -1) {
                baos.write(buff, 0, read);
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return baos.toByteArray();
    }

    private static final AwsJsonProtocolFactory JSON_PROTOCOL_FACTORY = AwsJsonProtocolFactory
        .builder()
        .protocol(AwsJsonProtocol.AWS_JSON)
        .protocolVersion("1.0")
        .baseServiceExceptionClass(software.amazon.awssdk.services.dynamodb.model.DynamoDbException.class)
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("ResourceInUseException").withModeledClass(
                ResourceInUseException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("TableAlreadyExistsException").withModeledClass(
                TableAlreadyExistsException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("GlobalTableAlreadyExistsException").withModeledClass(
                GlobalTableAlreadyExistsException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("InvalidRestoreTimeException").withModeledClass(
                InvalidRestoreTimeException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("ReplicaAlreadyExistsException").withModeledClass(
                ReplicaAlreadyExistsException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("ConditionalCheckFailedException").withModeledClass(
                ConditionalCheckFailedException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("BackupNotFoundException").withModeledClass(
                BackupNotFoundException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("IndexNotFoundException").withModeledClass(
                IndexNotFoundException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("LimitExceededException").withModeledClass(
                LimitExceededException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("GlobalTableNotFoundException").withModeledClass(
                GlobalTableNotFoundException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("ItemCollectionSizeLimitExceededException").withModeledClass(
                ItemCollectionSizeLimitExceededException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("ReplicaNotFoundException").withModeledClass(
                ReplicaNotFoundException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("TableNotFoundException").withModeledClass(
                TableNotFoundException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("BackupInUseException").withModeledClass(
                BackupInUseException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("ResourceNotFoundException").withModeledClass(
                ResourceNotFoundException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("ContinuousBackupsUnavailableException").withModeledClass(
                ContinuousBackupsUnavailableException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("TableInUseException").withModeledClass(
                TableInUseException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("ProvisionedThroughputExceededException").withModeledClass(
                ProvisionedThroughputExceededException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("PointInTimeRecoveryUnavailableException").withModeledClass(
                PointInTimeRecoveryUnavailableException.class))
        .addErrorMetadata(
            new JsonErrorShapeMetadata().withErrorCode("InternalServerError").withModeledClass(
                InternalServerErrorException.class))
        .build();


    private static final PutItemRequestMarshaller PUT_ITEM_REQUEST_MARSHALLER = new PutItemRequestMarshaller(getJsonProtocolFactory());

    private static HttpResponseHandler<GetItemResponse> getItemResponseJsonResponseHandler() {
        return JSON_PROTOCOL_FACTORY.createResponseHandler(new JsonOperationMetadata()
                .withPayloadJson(true).withHasStreamingSuccessResponse(false), GetItemResponse::builder);
    }

    public static PutItemRequestMarshaller putItemRequestMarshaller() {
        return PUT_ITEM_REQUEST_MARSHALLER;
    }

    private static AwsJsonProtocolFactory getJsonProtocolFactory() {
        return JSON_PROTOCOL_FACTORY;
    }
}
