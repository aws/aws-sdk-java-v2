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

package software.amazon.awssdk.bridge.smithyjava.error;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.bridge.smithyjava.retry.V2RetryClassification;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.smithy.java.core.error.ModeledException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.http.api.HttpResponse;

/**
 * A smithy-java {@link ModeledException} that wraps an AWS SDK v2 modeled exception.
 *
 * <h2>Why this exists</h2>
 *
 * <p>smithy-java's error deserialization path resolves error builders with
 * {@code typeRegistry.createBuilder(shapeId, ModeledException.class)}, and
 * {@code TypeRegistry.createBuilder} throws unless the registered class is assignable to the
 * requested type. {@link ModeledException} is an <em>abstract class</em>, and every v2 modeled
 * exception already extends {@code AwsServiceException}, so a v2 exception can never be a
 * {@code ModeledException} — Java has no multiple inheritance.
 *
 * <p>Rather than fork the protocol (AWS JSON's {@code HttpErrorDeserializer} is built internally by
 * a sealed protocol class and cannot be replaced), we register <em>this</em> class in the type
 * registry for every v2 error shape and delegate all deserialization to the v2 exception's generated
 * builder. One shim serves every error shape of every service; no per-error code is generated.
 *
 * <p>The v2 exception is unwrapped and rethrown at the client boundary, so callers still catch the
 * concrete v2 type. See {@code compatability_issues.md} section 1.1 for the residual differences.
 *
 * <h2>Retry classification and AWS error details</h2>
 *
 * <p>The generated schemas carry no Smithy traits, so smithy-java's {@code ApplyModelRetryInfoPlugin}
 * cannot classify anything. Instead {@link #enrich} is called once per attempt by
 * {@link V2ErrorEnricher} with the HTTP response, which lets us populate {@code awsErrorDetails},
 * the status code, and the request ID on the wrapped v2 exception, then derive smithy's
 * {@link software.amazon.smithy.java.retries.api.RetryInfo} from v2's own classification.
 */
@SdkProtectedApi
public final class V2ModeledError extends ModeledException {

    private final transient ShapeBuilder<? extends SerializableStruct> delegate;
    private AwsServiceException v2;

    private V2ModeledError(Schema schema, AwsServiceException v2, ShapeBuilder<? extends SerializableStruct> delegate) {
        super(schema, v2.getMessage(), null, Boolean.FALSE, true);
        this.v2 = v2;
        this.delegate = delegate;
        V2RetryClassification.applyTo(this, v2);
    }

    /**
     * The wrapped v2 exception. This is what the client boundary rethrows.
     *
     * @return the v2 exception.
     */
    public AwsServiceException v2Exception() {
        return v2;
    }

    /**
     * Fills in the parts of the v2 exception that only the transport layer knows about — status code,
     * request ID, and {@link AwsErrorDetails} — and re-derives retry classification from them.
     *
     * <p>v2 classifies throttling by AWS error code and clock skew by error code plus server date,
     * so without this the wrapped exception would be classified purely on modeled members and
     * throttling would never be retried.
     *
     * @param response    the raw HTTP response the error was deserialized from.
     * @param errorCode   the normalized error code smithy extracted from the payload, may be null.
     * @param serviceName the v2 service name to record in {@code awsErrorDetails}.
     */
    public void enrich(HttpResponse response, String errorCode, String serviceName) {
        enrich(response, errorCode, serviceName, null);
    }

    /**
     * As {@link #enrich(HttpResponse, String, String)}, also recording the raw error body, which stock v2
     * exposes as {@code awsErrorDetails().rawResponse()}.
     */
    public void enrich(HttpResponse response, String errorCode, String serviceName, byte[] rawBody) {
        if (!(delegate instanceof AwsServiceException.Builder builder)) {
            return;
        }
        SdkHttpResponse httpResponse = SdkHttpFullResponse.builder()
                                                          .statusCode(response.statusCode())
                                                          .headers(response.headers().map())
                                                          .build();
        String message = v2.getMessage();
        builder.awsErrorDetails(AwsErrorDetails.builder()
                                               .errorCode(errorCode)
                                               .errorMessage(message)
                                               .serviceName(serviceName)
                                               .sdkHttpResponse(httpResponse)
                                               .rawResponse(rawBody == null ? null
                                                                            : software.amazon.awssdk.core.SdkBytes
                                                                                  .fromByteArray(rawBody))
                                               .build())
               .statusCode(response.statusCode())
               .requestId(httpResponse.firstMatchingHeader("x-amzn-RequestId")
                                      .orElseGet(() -> httpResponse.firstMatchingHeader("x-amz-request-id")
                                                                   .orElse(null)))
               .extendedRequestId(httpResponse.firstMatchingHeader("x-amz-id-2").orElse(null));
        this.v2 = builder.build();
        V2RetryClassification.applyTo(this, this.v2);
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        ((SerializableStruct) v2).serializeMembers(serializer);
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        return ((SerializableStruct) v2).getMemberValue(member);
    }

    /**
     * Creates a builder factory to register in a {@code TypeRegistry} for one v2 error shape.
     *
     * <p>Generated code calls this once per error shape:
     * {@snippet :
     * TypeRegistry.builder()
     *     .putType(SomeException.$SCHEMA.id(), V2ModeledError.class,
     *              V2ModeledError.builderFactory(SomeException.$SCHEMA, SomeException::builder))
     *     .build();
     * }
     *
     * @param schema         the v2 error shape's schema.
     * @param builderFactory factory for the v2 exception's generated builder.
     * @return a supplier of shim builders.
     */
    public static Supplier<ShapeBuilder<V2ModeledError>> builderFactory(
            Schema schema,
            Supplier<? extends ShapeBuilder<? extends SerializableStruct>> builderFactory
    ) {
        return () -> new Builder(schema, builderFactory.get());
    }

    /**
     * Delegates every deserialization concern to the v2 exception's generated builder, then wraps the
     * result. The v2 builder is retained so {@link #enrich} can re-set transport-level fields.
     */
    private static final class Builder implements ShapeBuilder<V2ModeledError> {

        private final Schema schema;
        private final ShapeBuilder<? extends SerializableStruct> delegate;

        private Builder(Schema schema, ShapeBuilder<? extends SerializableStruct> delegate) {
            this.schema = schema;
            this.delegate = delegate;
        }

        @Override
        public V2ModeledError build() {
            SerializableStruct built = delegate.build();
            if (!(built instanceof AwsServiceException v2)) {
                throw new IllegalStateException(
                        "Expected " + schema.id() + " to build an AwsServiceException but got "
                        + built.getClass().getName());
            }
            return new V2ModeledError(schema, v2, delegate);
        }

        @Override
        public ShapeBuilder<V2ModeledError> deserialize(ShapeDeserializer decoder) {
            delegate.deserialize(decoder);
            return this;
        }

        @Override
        public ShapeBuilder<V2ModeledError> deserializeMember(ShapeDeserializer decoder, Schema member) {
            delegate.deserializeMember(decoder, member);
            return this;
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            delegate.setMemberValue(member, value);
        }

        @Override
        public ShapeBuilder<V2ModeledError> errorCorrection() {
            delegate.errorCorrection();
            return this;
        }

        @Override
        public Schema schema() {
            return schema;
        }
    }
}
