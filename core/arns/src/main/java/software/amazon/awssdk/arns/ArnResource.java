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
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An additional model within {@link Arn} that provides the Resource Type, Resource, and
 * Resource Qualifier of an AWS Arn when those values are present and correctly formatted
 * within an Arn.
 *
 * <p>
 * If {@link #resourceType} is not present, {@link #resourceId} will return the entire resource
 * as a string the same as {@link Arn#resource()}.
 *
 * @see Arn
 */
@SdkPublicApi
public final class ArnResource implements ToCopyableBuilder<ArnResource.Builder, ArnResource> {

    private final String resourceType;
    private final String resourceId;
    private final String qualifier;
    private final ArnResourceFormat resourceFormat;

    /**
     * There are two valid ways to format a resource type paired with a resource ID:
     * with a colon or with a slash.
     */
    public enum ArnResourceFormat {
        RESOURCE_WITH_SLASH('/'),
        RESOURCE_WITH_COLON(':');

        private final char seperator;

        ArnResourceFormat(char seperator) {
            this.seperator = seperator;
        }

        char getSeperator() {
            return this.seperator;
        }
    }

    private ArnResource(DefaultBuilder builder) {
        this.resourceType = builder.resourceType;
        this.resourceId = Validate.paramNotBlank(builder.resourceId, "resource");
        this.qualifier = builder.qualifier;
        this.resourceFormat = builder.resourceFormat;
    }

    /**
     * @return the optional resource type
     */
    public Optional<String> resourceType() {
        return Optional.ofNullable(resourceType);
    }

    /**
     * @return the entire resource as a string
     */
    public String resource() {
        return this.resourceId;
    }

    /**
     * @return the resourceId
     */
    public String resourceId() {
        return resourceId;
    }

    /**
     * @return the optional resource qualifier
     */
    public Optional<String> qualifier() {
        return Optional.ofNullable(qualifier);
    }

    /**
     * @return a builder for {@link ArnResource}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Parses a string containing either a resource, resource type and resource or
     * resource type, resource and qualifier into an {@link ArnResource}.
     *
     * <p>
     * Supports fields separated by either ":" or "/".
     *
     * <p>
     * For legacy AWS Arns not following the resourceType:resource:qualifier pattern,
     * the qualifier field will contain everything after the first two sections separated
     * by either ":" or "/".
     *
     * @param resource - The resource string to parse.
     * @return {@link ArnResource}
     */
    public static ArnResource fromString(String resource) {
        Character splitter = StringUtils.findFirstOccurrence(resource, ':', '/');

        if (splitter == null) {
            return ArnResource.builder().resourceId(resource).build();
        }

        int resourceTypeColonIndex = resource.indexOf(splitter);

        ArnResource.Builder builder = ArnResource.builder().resourceType(resource.substring(0, resourceTypeColonIndex));
        int resourceColonIndex = resource.indexOf(splitter, resourceTypeColonIndex);
        int qualifierColonIndex = resource.indexOf(':', resourceColonIndex + 1);
        if (qualifierColonIndex < 0) {
            builder.resourceId(resource.substring(resourceTypeColonIndex + 1));
        } else {
            builder.resourceId(resource.substring(resourceTypeColonIndex + 1, qualifierColonIndex));
            builder.qualifier(resource.substring(qualifierColonIndex + 1));
        }
        if (splitter.equals('/')) {
            builder.resourceFormat(ArnResourceFormat.RESOURCE_WITH_SLASH);
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return this.resourceType
               + ":"
               + this.resourceId
               + ":"
               + this.qualifier;
    }

    public String toStringFormatted() {
        if (this.resourceType == null) {
            return this.resourceId;
        }
        if (this.qualifier == null) {
            return this.resourceType + this.resourceFormat.getSeperator() + this.resourceId;
        }
        return this.resourceType + this.resourceFormat.getSeperator() + this.resourceId + ':' + qualifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArnResource that = (ArnResource) o;

        if (!Objects.equals(resourceType, that.resourceType)) {
            return false;
        }
        if (!Objects.equals(resourceId, that.resourceId)) {
            return false;
        }
        return Objects.equals(qualifier, that.qualifier);
    }

    @Override
    public int hashCode() {
        int result = resourceType != null ? resourceType.hashCode() : 0;
        result = 31 * result + (resourceId != null ? resourceId.hashCode() : 0);
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        return result;
    }

    @Override
    public Builder toBuilder() {
        return builder()
            .resourceId(resourceId)
            .resourceType(resourceType)
            .qualifier(qualifier)
            .resourceFormat(resourceFormat);
    }


    public interface Builder extends CopyableBuilder<ArnResource.Builder, ArnResource> {

        /**
         * Define the type of the resource.
         *
         * @param resourceType the partition that the resource is in
         * @return Returns a reference to this builder
         */
        Builder resourceType(String resourceType);

        /**
         * Define the entire resource.
         *
         * @param resource the entire resource
         * @return Returns a reference to this builder
         */
        Builder resource(String resource);

        /**
         * Define the resource ID. This has the same affect as {@link #resource(String)}, but
         * renamed to better align with the ARN format spec. 
         *
         * @param resourceId the entire resource
         * @return Returns a reference to this builder
         */
        Builder resourceId(String resourceId);

        /**
         * Define the qualifier of the resource.
         *
         * @param qualifier the qualifier of the resource
         * @return Returns a reference to this builder
         */
        Builder qualifier(String qualifier);

        /**
         * For legacy AWS Arns not following the resourceType:resource:qualifier pattern,
         * the qualifier field will contain everything after the first two sections separated
         * by a slash character "/". The default is to use a colon.
         *
         * @param resourceFormat the resource format to use
         * @return Returns a reference to this builder
         */
        Builder resourceFormat(ArnResourceFormat resourceFormat);

        /**
         * Set the builder from an pre-existing {@link ArnResource}.
         * 
         * @param arnResource The {@link ArnResource} to copy.
         * @return Returns a reference to this builder
         */
        Builder arnResource(ArnResource arnResource);

        /**
         * @return an instance of {@link ArnResource} that is created from the builder
         */
        @Override
        ArnResource build();
    }

    public static final class DefaultBuilder implements Builder {
        private String resourceType;
        private String resourceId;
        private String qualifier;
        private ArnResourceFormat resourceFormat = ArnResourceFormat.RESOURCE_WITH_COLON;

        private DefaultBuilder() {
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        @Override
        public Builder resourceType(String resourceType) {
            setResourceType(resourceType);
            return this;
        }

        public void setResource(String resource) {
            this.resourceId = resource;
        }

        @Override
        public Builder resource(String resource) {
            setResource(resource);
            return this;
        }

        public void setQualifier(String qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public Builder qualifier(String qualifier) {
            setQualifier(qualifier);
            return this;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        @Override
        public Builder resourceId(String resourceId) {
            setResourceId(resourceId);
            return this;
        }

        public void setResourceFormat(ArnResourceFormat resourceFormat) {
            this.resourceFormat = resourceFormat;
        }

        @Override
        public Builder resourceFormat(ArnResourceFormat resourceFormat) {
            setResourceFormat(resourceFormat);
            return this;
        }

        public void setArnResource(ArnResource arnResource) {
            setResourceType(arnResource.resourceType);
            setResourceId(arnResource.resourceId);
            setQualifier(arnResource.qualifier);
            setResourceFormat(arnResource.resourceFormat);
        }

        @Override
        public Builder arnResource(ArnResource arnResource) {
            setArnResource(arnResource);
            return this;
        }

        @Override
        public ArnResource build() {
            return new ArnResource(this);
        }
    }
}