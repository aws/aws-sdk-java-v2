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

package software.amazon.awssdk.benchmark.apicall.protocol;

import static java.util.Arrays.asList;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromB;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromBool;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromBs;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromL;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromM;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromNs;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromNul;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromSs;

import java.net.URI;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import software.amazon.awssdk.services.dynamodb.model.BackupInUseException;
import software.amazon.awssdk.services.dynamodb.model.BackupNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ContinuousBackupsUnavailableException;
import software.amazon.awssdk.services.dynamodb.model.DuplicateItemException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ExportConflictException;
import software.amazon.awssdk.services.dynamodb.model.ExportNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalTableAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.GlobalTableNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.IdempotentParameterMismatchException;
import software.amazon.awssdk.services.dynamodb.model.ImportConflictException;
import software.amazon.awssdk.services.dynamodb.model.ImportNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.IndexNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import software.amazon.awssdk.services.dynamodb.model.InvalidExportTimeException;
import software.amazon.awssdk.services.dynamodb.model.InvalidRestoreTimeException;
import software.amazon.awssdk.services.dynamodb.model.ItemCollectionSizeLimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.LimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.PointInTimeRecoveryUnavailableException;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReplicaAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.ReplicaNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.TableInUseException;
import software.amazon.awssdk.services.dynamodb.model.TableNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.TransactionConflictException;
import software.amazon.awssdk.services.dynamodb.model.TransactionInProgressException;
import software.amazon.awssdk.services.dynamodb.transform.GetItemRequestMarshaller;
import software.amazon.awssdk.services.dynamodb.transform.PutItemRequestMarshaller;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Benchmarking for running with different protocols.
 */
@State(Scope.Benchmark)
// @Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
// @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2) // To reduce difference between each run
@BenchmarkMode(Mode.All)
public class MarshallingBenchmark {
    private static final AwsJsonProtocolFactory PROTOCOL_FACTORY =
        AwsJsonProtocolFactory.builder()
                              .clientConfiguration(SdkClientConfiguration.builder()
                                                                         .option(SdkClientOption.ENDPOINT, URI.create("https://localhost"))
                                                                         .build())
                              .defaultServiceExceptionSupplier(DynamoDbException::builder)
                              .protocol(AwsJsonProtocol.AWS_JSON)
                              .protocolVersion("1.0")
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("RequestLimitExceeded")
                                                   .exceptionBuilderSupplier(RequestLimitExceededException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("GlobalTableAlreadyExistsException")
                                                   .exceptionBuilderSupplier(GlobalTableAlreadyExistsException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ImportConflictException")
                                                   .exceptionBuilderSupplier(ImportConflictException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ConditionalCheckFailedException")
                                                   .exceptionBuilderSupplier(ConditionalCheckFailedException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("LimitExceededException")
                                                   .exceptionBuilderSupplier(LimitExceededException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("GlobalTableNotFoundException")
                                                   .exceptionBuilderSupplier(GlobalTableNotFoundException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ItemCollectionSizeLimitExceededException")
                                                   .exceptionBuilderSupplier(ItemCollectionSizeLimitExceededException::builder).httpStatusCode(400)
                                                   .build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ReplicaNotFoundException")
                                                   .exceptionBuilderSupplier(ReplicaNotFoundException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("BackupInUseException")
                                                   .exceptionBuilderSupplier(BackupInUseException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ResourceNotFoundException")
                                                   .exceptionBuilderSupplier(ResourceNotFoundException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ContinuousBackupsUnavailableException")
                                                   .exceptionBuilderSupplier(ContinuousBackupsUnavailableException::builder).httpStatusCode(400)
                                                   .build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("IdempotentParameterMismatchException")
                                                   .exceptionBuilderSupplier(IdempotentParameterMismatchException::builder).httpStatusCode(400)
                                                   .build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ExportNotFoundException")
                                                   .exceptionBuilderSupplier(ExportNotFoundException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("TransactionInProgressException")
                                                   .exceptionBuilderSupplier(TransactionInProgressException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("TableInUseException")
                                                   .exceptionBuilderSupplier(TableInUseException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ProvisionedThroughputExceededException")
                                                   .exceptionBuilderSupplier(ProvisionedThroughputExceededException::builder).httpStatusCode(400)
                                                   .build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("PointInTimeRecoveryUnavailableException")
                                                   .exceptionBuilderSupplier(PointInTimeRecoveryUnavailableException::builder).httpStatusCode(400)
                                                   .build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ResourceInUseException")
                                                   .exceptionBuilderSupplier(ResourceInUseException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("TableAlreadyExistsException")
                                                   .exceptionBuilderSupplier(TableAlreadyExistsException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ExportConflictException")
                                                   .exceptionBuilderSupplier(ExportConflictException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("TransactionConflictException")
                                                   .exceptionBuilderSupplier(TransactionConflictException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("InvalidRestoreTimeException")
                                                   .exceptionBuilderSupplier(InvalidRestoreTimeException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ReplicaAlreadyExistsException")
                                                   .exceptionBuilderSupplier(ReplicaAlreadyExistsException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("BackupNotFoundException")
                                                   .exceptionBuilderSupplier(BackupNotFoundException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("IndexNotFoundException")
                                                   .exceptionBuilderSupplier(IndexNotFoundException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("TableNotFoundException")
                                                   .exceptionBuilderSupplier(TableNotFoundException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("DuplicateItemException")
                                                   .exceptionBuilderSupplier(DuplicateItemException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ImportNotFoundException")
                                                   .exceptionBuilderSupplier(ImportNotFoundException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("TransactionCanceledException")
                                                   .exceptionBuilderSupplier(TransactionCanceledException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("InvalidExportTimeException")
                                                   .exceptionBuilderSupplier(InvalidExportTimeException::builder).httpStatusCode(400).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("InternalServerError")
                                                   .exceptionBuilderSupplier(InternalServerErrorException::builder).httpStatusCode(500).build())
                              .build();
    private static final GetItemRequestMarshaller GET_MARSHALLER = new GetItemRequestMarshaller(PROTOCOL_FACTORY);
    private static final PutItemRequestMarshaller PUT_MARSHALLER = new PutItemRequestMarshaller(PROTOCOL_FACTORY);

    private static final GetItemRequest SMALL_REQUEST =
        GetItemRequest.builder()
                      .tableName("table")
                      .key(ImmutableMap.of("key", fromS("foo")))
                      .build();

    private static final PutItemRequest MEDIUM_REQUEST =
        PutItemRequest.builder()
                      .tableName("table")
                      .item(ImmutableMap.<String, AttributeValue>builder()
                                        .put("id", fromS("foo"))
                                        .put("sort", fromN("0"))
                                        .put("bytes", fromB(SdkBytes.fromUtf8String("lfkn3\0lkfn3fh039fhiil")))
                                        .put("bool", fromBool(false))
                                        .put("list", fromL(asList(fromS("start"),
                                                                  fromN("2"))))
                                        .put("byteSet", fromBs(asList(SdkBytes.fromUtf8String("lfkn3\0lkfn3fh039fhiil"),
                                                                 SdkBytes.fromUtf8String("lfkn3\0lkfn3fh039fhiil"),
                                                                 SdkBytes.fromUtf8String("lfkn3\0lkfn3fh039fhiil"))))
                                        .put("map", fromM(ImmutableMap.<String, AttributeValue>builder()
                                                                    .put("1", fromS("2"))
                                                                    .build()))
                                        .put("numberSet", fromNs(asList("1", "2", "3", "4", "5")))
                                        .put("null", fromNul(true))
                                        .put("stringSet", fromSs(asList("a", "b", "c")))
                                        .build())
                      .build();

    @Benchmark
    public void oldWaySmallObject(Blackhole blackhole) {
        blackhole.consume(GET_MARSHALLER.marshall(SMALL_REQUEST));
    }

    @Benchmark
    public void newWaySmallObject(Blackhole blackhole) {
        blackhole.consume(GET_MARSHALLER.fastMarshall(SMALL_REQUEST));
    }

    @Benchmark
    public void oldWayLargeObject(Blackhole blackhole) {
        blackhole.consume(PUT_MARSHALLER.marshall(MEDIUM_REQUEST));
    }

    @Benchmark
    public void newWayLargeObject(Blackhole blackhole) {
        blackhole.consume(PUT_MARSHALLER.fastMarshall(MEDIUM_REQUEST));
    }

    public static void main(String... args) throws Exception {
        System.out.println(IoUtils.toUtf8String(GET_MARSHALLER.marshall(SMALL_REQUEST).contentStreamProvider().get().newStream()));
        System.out.println(IoUtils.toUtf8String(GET_MARSHALLER.fastMarshall(SMALL_REQUEST).contentStreamProvider().get().newStream()));
        System.out.println(IoUtils.toUtf8String(PUT_MARSHALLER.marshall(MEDIUM_REQUEST).contentStreamProvider().get().newStream()));
        System.out.println(IoUtils.toUtf8String(PUT_MARSHALLER.fastMarshall(MEDIUM_REQUEST).contentStreamProvider().get().newStream()));
        Options opt = new OptionsBuilder()
            .include(MarshallingBenchmark.class.getSimpleName())
            .build();
        new Runner(opt).run();
    }
}
