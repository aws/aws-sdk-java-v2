/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request to generate a URL representing an object in Amazon S3.
 *
 * If the object identified by the given bucket and key has public read permissions,
 * then this URL can be directly accessed to retrieve the object's data.
 */
@SdkPublicApi
public final class GetUrlRequest extends S3Request implements ToCopyableBuilder<GetUrlRequest.Builder, GetUrlRequest> {
    private static final SdkField<String> BUCKET_FIELD = SdkField
        .<String>builder(MarshallingType.STRING)
        .getter(getter(GetUrlRequest::bucket))
        .setter(setter(Builder::bucket))
        .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("Bucket")
                             .unmarshallLocationName("Bucket").build()).build();

    private static final SdkField<String> KEY_FIELD = SdkField
        .<String>builder(MarshallingType.STRING)
        .getter(getter(GetUrlRequest::key))
        .setter(setter(Builder::key))
        .traits(LocationTrait.builder().location(MarshallLocation.GREEDY_PATH).locationName("Key")
                             .unmarshallLocationName("Key").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(BUCKET_FIELD, KEY_FIELD));

    private final String bucket;

    private final String key;

    private GetUrlRequest(BuilderImpl builder) {
        super(builder);
        this.bucket = builder.bucket;
        this.key = builder.key;
    }

    /**
     * Returns the value of the Bucket property for this object.
     *
     * @return The value of the Bucket property for this object.
     */
    public String bucket() {
        return bucket;
    }

    /**
     * Returns the value of the Key property for this object.
     *
     * @return The value of the Key property for this object.
     */
    public String key() {
        return key;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(bucket());
        hashCode = 31 * hashCode + Objects.hashCode(key());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GetUrlRequest)) {
            return false;
        }
        GetUrlRequest other = (GetUrlRequest) obj;
        return Objects.equals(bucket(), other.bucket()) && Objects.equals(key(), other.key());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("GetUrlRequest").add("Bucket", bucket()).add("Key", key()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "Bucket":
                return Optional.ofNullable(clazz.cast(bucket()));
            case "Key":
                return Optional.ofNullable(clazz.cast(key()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<GetUrlRequest, T> g) {
        return obj -> g.apply((GetUrlRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends S3Request.Builder, SdkPojo, CopyableBuilder<Builder, GetUrlRequest> {
        /**
         * Sets the value of the Bucket property for this object.
         *
         * @param bucket
         *        The new value for the Bucket property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder bucket(String bucket);

        /**
         * Sets the value of the Key property for this object.
         *
         * @param key
         *        The new value for the Key property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder key(String key);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends S3Request.BuilderImpl implements Builder {
        private String bucket;

        private String key;

        private BuilderImpl() {
        }

        private BuilderImpl(GetUrlRequest model) {
            super(model);
            bucket(model.bucket);
            key(model.key);
        }

        public String getBucket() {
            return bucket;
        }

        @Override
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getKey() {
            return key;
        }

        @Override
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            super.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer) {
            super.overrideConfiguration(builderConsumer);
            return this;
        }

        @Override
        public GetUrlRequest build() {
            return new GetUrlRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
