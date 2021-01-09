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

package software.amazon.awssdk.arns;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The Arns generated and recognized by this code are the Arns described here:
 *
 * https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
 *
 * <p>
 * The primary supported Arn format is:
 *
 * <code>
 * arn::&#60;partition&#62;::&#60;service&#62;::&#60;region&#62;::&#60;account&#62;::&#60;resource&#62;
 * </code>
 *
 * <p>
 * {@link #resourceAsString()} returns everything after the account section of the Arn
 * as a single string.
 *
 * <p>
 * However, the following Arn formats are supported where the values are present and well
 * formatted through {@link #resource()}:
 *
 * <pre>
 * arn:&#60;partition&#62;:&#60;service&#62;:&#60;region&#62;:&#60;account&#62;:&#60;resourcetype&#62;/resource
 * arn:&#60;partition&#62;:&#60;service&#62;:&#60;region&#62;:&#60;account&#62;:&#60;resourcetype&#62;/resource/qualifier
 * arn:&#60;partition&#62;:&#60;service&#62;:&#60;region&#62;:&#60;account&#62;:&#60;resourcetype&#62;/resource:qualifier
 * arn:&#60;partition&#62;:&#60;service&#62;:&#60;region&#62;:&#60;account&#62;:&#60;resourcetype&#62;:resource
 * arn:&#60;partition&#62;:&#60;service&#62;:&#60;region&#62;:&#60;account&#62;:&#60;resourcetype&#62;:resource:qualifier
 * </pre>
 *
 * {@link #resource()} returns a {@link ArnResource} which has access
 * to {@link ArnResource#resourceType()}, {@link ArnResource#resource()} and
 * {@link ArnResource#qualifier()}.
 *
 * <p>
 * To parse an Arn from a string use Arn.fromString(). To convert an Arn to it's
 * string representation use Arn.toString().
 *
 * <p>
 * For instance, for a string s, containing a well-formed Arn the
 * following should always be true:
 *
 * <pre>
 * Arn theArn = Arn.fromString(s);
 * s.equals(theArn.toString());
 * </pre>
 *
 * @see ArnResource
 */
@SdkPublicApi
public final class Arn implements ToCopyableBuilder<Arn.Builder, Arn> {

    private final String partition;
    private final String service;
    private final String region;
    private final String accountId;
    private final String resource;
    private final ArnResource arnResource;

    private Arn(DefaultBuilder builder) {
        this.partition = Validate.paramNotBlank(builder.partition, "partition");
        this.service = Validate.paramNotBlank(builder.service, "service");
        this.region = builder.region;
        this.accountId = builder.accountId;
        this.resource = Validate.paramNotBlank(builder.resource, "resource");
        this.arnResource = ArnResource.fromString(resource);
    }

    /**
     * @return The partition that the resource is in.
     */
    public String partition() {
        return partition;
    }

    /**
     * @return The service namespace that identifies the AWS product (for example, Amazon S3, IAM, or Amazon RDS).
     */
    public String service() {
        return service;
    }

    /**
     * @return The Region that the resource resides in.
     */
    public Optional<String> region() {
        return Optional.ofNullable(region);
    }

    /**
     * @return The ID of the AWS account that owns the resource, without the hyphens.
     */
    public Optional<String> accountId() {
        return Optional.ofNullable(accountId);
    }

    /**
     * @return {@link ArnResource}
     */
    public ArnResource resource() {
        return arnResource;
    }

    /**
     * @return the resource as string
     */
    public String resourceAsString() {
        return resource;
    }

    /**
     * @return a builder for {@link Arn}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Parses a given string into an {@link Arn}. The resource is accessible entirely as a
     * string through {@link #resourceAsString()}. Where correctly formatted, a parsed
     * resource containing resource type, resource and qualifier is available through
     * {@link #resource()}.
     *
     * @param arn - A string containing an Arn.
     * @return {@link Arn} - A modeled Arn.
     */
    public static Arn fromString(String arn) {
        int arnColonIndex = arn.indexOf(':');
        if (arnColonIndex < 0 || !"arn".equals(arn.substring(0, arnColonIndex))) {
            throw new IllegalArgumentException("Malformed ARN - doesn't start with 'arn:'");
        }

        int partitionColonIndex = arn.indexOf(':', arnColonIndex + 1);
        if (partitionColonIndex < 0) {
            throw new IllegalArgumentException("Malformed ARN - no AWS partition specified");
        }
        String partition = arn.substring(arnColonIndex + 1, partitionColonIndex);

        int serviceColonIndex = arn.indexOf(':', partitionColonIndex + 1);
        if (serviceColonIndex < 0) {
            throw new IllegalArgumentException("Malformed ARN - no service specified");
        }
        String service = arn.substring(partitionColonIndex + 1, serviceColonIndex);

        int regionColonIndex = arn.indexOf(':', serviceColonIndex + 1);
        if (regionColonIndex < 0) {
            throw new IllegalArgumentException("Malformed ARN - no AWS region partition specified");
        }
        String region = arn.substring(serviceColonIndex + 1, regionColonIndex);

        int accountColonIndex = arn.indexOf(':', regionColonIndex + 1);
        if (accountColonIndex < 0) {
            throw new IllegalArgumentException("Malformed ARN - no AWS account specified");
        }
        String accountId = arn.substring(regionColonIndex + 1, accountColonIndex);

        String resource = arn.substring(accountColonIndex + 1);
        if (resource.isEmpty()) {
            throw new IllegalArgumentException("Malformed ARN - no resource specified");
        }

        return Arn.builder()
                  .partition(partition)
                  .service(service)
                  .region(region)
                  .accountId(accountId)
                  .resource(resource)
                  .build();
    }

    @Override
    public String toString() {
        return "arn:"
               + this.partition
               + ":"
               + this.service
               + ":"
               + region
               + ":"
               + this.accountId
               + ":"
               + this.resource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Arn arn = (Arn) o;

        if (!Objects.equals(partition, arn.partition)) {
            return false;
        }
        if (!Objects.equals(service, arn.service)) {
            return false;
        }
        if (!Objects.equals(region, arn.region)) {
            return false;
        }
        if (!Objects.equals(accountId, arn.accountId)) {
            return false;
        }
        if (!Objects.equals(resource, arn.resource)) {
            return false;
        }
        return Objects.equals(arnResource, arn.arnResource);
    }

    @Override
    public int hashCode() {
        int result = partition.hashCode();
        result = 31 * result + service.hashCode();
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        result = 31 * result + resource.hashCode();
        return result;
    }

    @Override
    public Builder toBuilder() {
        return builder().accountId(accountId)
                        .partition(partition)
                        .region(region)
                        .resource(resource)
                        .service(service)
            ;
    }

    /**
     * A builder for a {@link Arn}. See {@link #builder()}.
     */
    public interface Builder extends CopyableBuilder<Builder, Arn> {

        /**
         * Define the partition that the resource is in.
         *
         * @param partition the partition that the resource is in
         * @return Returns a reference to this builder
         */
        Builder partition(String partition);

        /**
         * Define the service name that identifies the AWS product
         *
         * @param service The service name that identifies the AWS product
         * @return Returns a reference to this builder
         */
        Builder service(String service);

        /**
         * Define the Region that the resource resides in.
         *
         * @param region The Region that the resource resides in.
         * @return Returns a reference to this builder
         */
        Builder region(String region);

        /**
         * Define the ID of the AWS account that owns the resource, without the hyphens.
         *
         * @param accountId The ID of the AWS account that owns the resource, without the hyphens.
         * @return Returns a reference to this builder
         */
        Builder accountId(String accountId);

        /**
         * Define the resource identifier. A resource identifier can be the name or ID of the resource
         * or a resource path.
         *
         * @param resource resource identifier
         * @return Returns a reference to this builder
         */
        Builder resource(String resource);

        /**
         * @return an instance of {@link Arn} that is created from the builder
         */
        Arn build();
    }

    private static final class DefaultBuilder implements Builder {
        private String partition;
        private String service;
        private String region;
        private String accountId;
        private String resource;

        private DefaultBuilder() {
        }

        public void setPartition(String partition) {
            this.partition = partition;
        }

        @Override
        public Builder partition(String partition) {
            setPartition(partition);
            return this;
        }

        public void setService(String service) {
            this.service = service;
        }

        @Override
        public Builder service(String service) {
            setService(service);
            return this;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        @Override
        public Builder region(String region) {
            setRegion(region);
            return this;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        @Override
        public Builder accountId(String accountId) {
            setAccountId(accountId);
            return this;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        @Override
        public Builder resource(String resource) {
            setResource(resource);
            return this;
        }

        @Override
        public Arn build() {
            return new Arn(this);
        }
    }
}
