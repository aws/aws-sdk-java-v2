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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request to generate a URL representing an object in Amazon S3.
 *
 * If the object identified by the given bucket and key has public read permissions,
 * then this URL can be directly accessed to retrieve the object's data.
 */
@SdkPublicApi
public final class GetUrlRequest implements SdkPojo, ToCopyableBuilder<GetUrlRequest.Builder, GetUrlRequest> {
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

    private final Region region;

    private final URI endpoint;

    private GetUrlRequest(BuilderImpl builder) {
        this.bucket = Validate.paramNotBlank(builder.bucket, "Bucket");
        this.key = Validate.paramNotBlank(builder.key, "Key");
        this.region = builder.region;
        this.endpoint = builder.endpoint;
    }

    /**
     * @return The name of the bucket for the object
     */
    public String bucket() {
        return bucket;
    }

    /**
     * @return The key value for this object.
     */
    public String key() {
        return key;
    }

    /**
     * @return The region value to use for constructing the URL
     */
    public Region region() {
        return region;
    }

    /**
     * @return The endpoint value to use for constructing the URL
     */
    public URI endpoint() {
        return endpoint;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
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

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, GetUrlRequest> {
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

        /**
         * Sets the region to use for constructing the URL.
         *
         * @param region
         *        The region to use for constructing the URL.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder region(Region region);

        /**
         * Sets the endpoint to use for constructing the URL.
         *
         * @param endpoint
         *        The endpoint to use for constructing the URL.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder endpoint(URI endpoint);

    }

    private static final class BuilderImpl implements Builder {
        private String bucket;

        private String key;

        private Region region;

        private URI endpoint;

        private BuilderImpl() {
        }

        private BuilderImpl(GetUrlRequest getUrlRequest) {
            bucket(getUrlRequest.bucket);
            key(getUrlRequest.key);
            region(getUrlRequest.region);
            endpoint(getUrlRequest.endpoint);
        }

        @Override
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        @Override
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
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
