package software.amazon.awssdk.services.batchmanagertest.batchmanager.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.batchmanager.BatchAndSend;
import software.amazon.awssdk.core.batchmanager.BatchKeyMapper;
import software.amazon.awssdk.core.batchmanager.BatchResponseMapper;
import software.amazon.awssdk.core.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.services.batchmanagertest.BatchManagerTestAsyncClient;
import software.amazon.awssdk.services.batchmanagertest.BatchManagerTestClient;
import software.amazon.awssdk.services.batchmanagertest.model.BatchManagerTestException;
import software.amazon.awssdk.services.batchmanagertest.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchRequestEntry;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchResultEntry;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchRequestEntry;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchResultEntry;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestResponse;
import software.amazon.awssdk.utils.Either;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class BatchManagerTestBatchFunctions {
    private BatchManagerTestBatchFunctions() {
    }

    public static BatchAndSend<SendRequestRequest, SendRequestBatchResponse> sendRequestBatchFunction(
        BatchManagerTestClient client, Executor executor) {
        return (identifiedRequests, batchKey) -> {
            SendRequestBatchRequest batchRequest = createSendRequestBatchRequest(identifiedRequests, batchKey);
            return CompletableFuture.supplyAsync(() -> client.sendRequestBatch(batchRequest), executor);
        };
    }

    public static BatchAndSend<SendRequestRequest, SendRequestBatchResponse> sendRequestBatchAsyncFunction(
        BatchManagerTestAsyncClient client) {
        return (identifiedRequests, batchKey) -> {
            SendRequestBatchRequest batchRequest = createSendRequestBatchRequest(identifiedRequests, batchKey);
            return client.sendRequestBatch(batchRequest);
        };
    }

    private static SendRequestBatchRequest createSendRequestBatchRequest(
        List<IdentifiableMessage<SendRequestRequest>> identifiedRequests, String batchKey) {
        List<SendRequestBatchRequestEntry> entries = identifiedRequests
            .stream()
            .map(identifiedRequest -> createSendRequestBatchRequestEntry(identifiedRequest.id(), identifiedRequest.message()))
            .collect(Collectors.toList());
        // Since requests are batched together according to a combination of their destination and
        // overrideConfiguration, all requests must have the same overrideConfiguration so it is sufficient to retrieve
        // it from the first request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0).message()
                                                                                            .overrideConfiguration();
        return overrideConfiguration.map(
            overrideConfig -> SendRequestBatchRequest.builder().destination(batchKey).overrideConfiguration(overrideConfig)
                                                     .entries(entries).build()).orElse(
            SendRequestBatchRequest.builder().destination(batchKey).entries(entries).build());
    }

    private static SendRequestBatchRequestEntry createSendRequestBatchRequestEntry(String id, SendRequestRequest request) {
        return SendRequestBatchRequestEntry.builder().id(id).messageBody(request.messageBody()).delaySeconds(request.delaySeconds())
                                           .messageDeduplicationId(request.messageDeduplicationId()).messageGroupId(request.messageGroupId()).build();
    }

    public static BatchResponseMapper<SendRequestBatchResponse, SendRequestResponse> sendRequestResponseMapper() {
        return batchResponse -> {
            List<Either<IdentifiableMessage<SendRequestResponse>, IdentifiableMessage<Throwable>>> mappedResponses = new ArrayList<>();
            batchResponse.successful().forEach(batchResponseEntry -> {
                IdentifiableMessage<SendRequestResponse> response = createSendRequestResponse(batchResponseEntry, batchResponse);
                mappedResponses.add(Either.left(response));
            });
            batchResponse.failed().forEach(batchResponseEntry -> {
                IdentifiableMessage<Throwable> response = sendRequestCreateThrowable(batchResponseEntry);
                mappedResponses.add(Either.right(response));
            });
            return mappedResponses;
        };
    }

    private static IdentifiableMessage<SendRequestResponse> createSendRequestResponse(
        SendRequestBatchResultEntry successfulEntry, SendRequestBatchResponse batchResponse) {
        String key = successfulEntry.id();
        SendRequestResponse.Builder builder = SendRequestResponse.builder().md5OfMessageBody(successfulEntry.md5OfMessageBody())
                                                                 .md5OfMessageAttributes(successfulEntry.md5OfMessageAttributes())
                                                                 .md5OfMessageSystemAttributes(successfulEntry.md5OfMessageSystemAttributes())
                                                                 .messageId(successfulEntry.messageId()).sequenceNumber(successfulEntry.sequenceNumber());
        if (batchResponse.responseMetadata() != null) {
            builder.responseMetadata(batchResponse.responseMetadata());
        }
        if (batchResponse.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(batchResponse.sdkHttpResponse());
        }
        SendRequestResponse response = builder.build();
        return new IdentifiableMessage<SendRequestResponse>(key, response);
    }

    private static IdentifiableMessage<Throwable> sendRequestCreateThrowable(BatchResultErrorEntry failedEntry) {
        String key = failedEntry.id();
        AwsErrorDetails errorDetailsBuilder = AwsErrorDetails.builder().errorCode(failedEntry.errorCode())
                                                                     .errorMessage(failedEntry.errorMessage()).build();
        Throwable response = BatchManagerTestException.builder().awsErrorDetails(errorDetailsBuilder).build();
        return new IdentifiableMessage<Throwable>(key, response);
    }

    public static BatchKeyMapper<SendRequestRequest> sendRequestBatchKeyMapper() {
        return request -> request.overrideConfiguration()
                                 .map(overrideConfig -> request.destination() + overrideConfig.hashCode()).orElse(request.destination());
    }

    public static BatchAndSend<DeleteRequestRequest, DeleteRequestBatchResponse> deleteRequestBatchFunction(
        BatchManagerTestClient client, Executor executor) {
        return (identifiedRequests, batchKey) -> {
            DeleteRequestBatchRequest batchRequest = createDeleteRequestBatchRequest(identifiedRequests, batchKey);
            return CompletableFuture.supplyAsync(() -> client.deleteRequestBatch(batchRequest), executor);
        };
    }

    public static BatchAndSend<DeleteRequestRequest, DeleteRequestBatchResponse> deleteRequestBatchAsyncFunction(
        BatchManagerTestAsyncClient client) {
        return (identifiedRequests, batchKey) -> {
            DeleteRequestBatchRequest batchRequest = createDeleteRequestBatchRequest(identifiedRequests, batchKey);
            return client.deleteRequestBatch(batchRequest);
        };
    }

    private static DeleteRequestBatchRequest createDeleteRequestBatchRequest(
        List<IdentifiableMessage<DeleteRequestRequest>> identifiedRequests, String batchKey) {
        List<DeleteRequestBatchRequestEntry> entries = identifiedRequests
            .stream()
            .map(identifiedRequest -> createDeleteRequestBatchRequestEntry(identifiedRequest.id(),
                                                                           identifiedRequest.message())).collect(Collectors.toList());
        // Since requests are batched together according to a combination of their destination and
        // overrideConfiguration, all requests must have the same overrideConfiguration so it is sufficient to retrieve
        // it from the first request.
        Optional<AwsRequestOverrideConfiguration> overrideConfiguration = identifiedRequests.get(0).message()
                                                                                            .overrideConfiguration();
        return overrideConfiguration.map(
            overrideConfig -> DeleteRequestBatchRequest.builder().destination(batchKey).overrideConfiguration(overrideConfig)
                                                       .entries(entries).build()).orElse(
            DeleteRequestBatchRequest.builder().destination(batchKey).entries(entries).build());
    }

    private static DeleteRequestBatchRequestEntry createDeleteRequestBatchRequestEntry(String id, DeleteRequestRequest request) {
        return DeleteRequestBatchRequestEntry.builder().id(id).receiptHandle(request.receiptHandle()).build();
    }

    public static BatchResponseMapper<DeleteRequestBatchResponse, DeleteRequestResponse> deleteRequestResponseMapper() {
        return batchResponse -> {
            List<Either<IdentifiableMessage<DeleteRequestResponse>, IdentifiableMessage<Throwable>>> mappedResponses = new ArrayList<>();
            batchResponse.successful().forEach(
                batchResponseEntry -> {
                    IdentifiableMessage<DeleteRequestResponse> response = createDeleteRequestResponse(batchResponseEntry,
                                                                                                      batchResponse);
                    mappedResponses.add(Either.left(response));
                });
            batchResponse.failed().forEach(batchResponseEntry -> {
                IdentifiableMessage<Throwable> response = deleteRequestCreateThrowable(batchResponseEntry);
                mappedResponses.add(Either.right(response));
            });
            return mappedResponses;
        };
    }

    private static IdentifiableMessage<DeleteRequestResponse> createDeleteRequestResponse(
        DeleteRequestBatchResultEntry successfulEntry, DeleteRequestBatchResponse batchResponse) {
        String key = successfulEntry.id();
        DeleteRequestResponse.Builder builder = DeleteRequestResponse.builder();
        if (batchResponse.responseMetadata() != null) {
            builder.responseMetadata(batchResponse.responseMetadata());
        }
        if (batchResponse.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(batchResponse.sdkHttpResponse());
        }
        DeleteRequestResponse response = builder.build();
        return new IdentifiableMessage<DeleteRequestResponse>(key, response);
    }

    private static IdentifiableMessage<Throwable> deleteRequestCreateThrowable(BatchResultErrorEntry failedEntry) {
        String key = failedEntry.id();
        AwsErrorDetails errorDetailsBuilder = AwsErrorDetails.builder().errorCode(failedEntry.errorCode())
                                                                     .errorMessage(failedEntry.errorMessage()).build();
        Throwable response = BatchManagerTestException.builder().awsErrorDetails(errorDetailsBuilder).build();
        return new IdentifiableMessage<Throwable>(key, response);
    }

    public static BatchKeyMapper<DeleteRequestRequest> deleteRequestBatchKeyMapper() {
        return request -> request.overrideConfiguration()
                                 .map(overrideConfig -> request.destination() + overrideConfig.hashCode()).orElse(request.destination());
    }
}
