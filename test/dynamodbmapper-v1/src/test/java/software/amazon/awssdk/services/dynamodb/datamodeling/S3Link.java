/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.io.OutputStream;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.util.json.JacksonUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AccessControlPolicy;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * An S3 Link that works with {@link DynamoDbMapper}.
 * An S3 link is persisted as a JSON string in DynamoDB.
 * This link object can be used directly to upload/download files to S3.
 * Alternatively, the underlying
 * {@link S3Client} can be retrieved to
 * provide full access API to S3.
 * <p>
 * For example:
 * <pre class="brush: java">
 * AWSCredentialsProvider s3CredentialProvider = ...;
 * DynamoDBMapper mapper = new DynamoDBMapper(..., s3CredentialProvider);
 * String username = "jamestkirk";
 *
 * User user = new User();
 * user.setUsername(username);
 *
 * // S3 region can be specified, but is optional
 * S3Link s3link = mapper.createS3Link("my-company-user-avatars", username + ".jpg");
 * user.setAvatar(s3link);
 *
 * // All meta information of the S3 resource is persisted in DynamoDB, including
 * // region, bucket, and key
 * mapper.save(user);
 *
 * // Upload file to S3 with the link saved in DynamoDB
 * s3link.uploadFrom(new File("/path/to/all/those/user/avatars/" + username + ".jpg"));
 * // Download file from S3 via an S3Link
 * s3link.downloadTo(new File("/path/to/downloads/" + username + ".jpg"));
 *
 * // Full S3 API is available via the canonical AmazonS3Client and TransferManager API.
 * // For example:
 * AmazonS3Client s3 = s3link.getAmazonS3Client();
 * TransferManager s3m = s3link.getTransferManager();
 * // etc.
 * </pre>The User pojo class used above:<pre class="brush: java">
 * &commat;DynamoDBTable(tableName = "user-table")
 * public class User {
 *     private String username;
 *     private S3Link avatar;
 *
 *     &commat;DynamoDBHashKey
 *     public String getUsername() {
 *         return username;
 *     }
 *
 *     public void setUsername(String username) {
 *         this.username = username;
 *     }
 *
 *     public S3Link getAvatar() {
 *         return avatar;
 *     }
 *
 *     public void setAvatar(S3Link avatar) {
 *         this.avatar = avatar;
 *     }
 * }
 * </pre>
 */
public class S3Link {
    private final S3ClientCache s3cc;
    private final Id id;

    S3Link(S3ClientCache s3cc, String bucketName, String key) {
        this(s3cc, new Id(bucketName, key));
    }

    S3Link(S3ClientCache s3cc, String region, String bucketName, String key) {
        this(s3cc, new Id(region, bucketName, key));
    }

    private S3Link(S3ClientCache s3cc, Id id) {
        this.s3cc = s3cc;
        this.id = id;

        if (s3cc == null) {
            throw new IllegalArgumentException("S3ClientCache must be configured for use with S3Link");
        }
        if (id == null || id.bucket() == null || id.getKey() == null) {
            throw new IllegalArgumentException("Bucket and key must be specified for S3Link");
        }
    }

    /**
     * Deserializes from a JSON string.
     */
    public static S3Link fromJson(S3ClientCache s3cc, String json) {
        Id id = JacksonUtils.fromJsonString(json, Id.class);
        return new S3Link(s3cc, id);
    }

    private static String convertRegionToString(Region region, String bucketName) {
        return region.value();
    }

    public String getKey() {
        return id.getKey();
    }

    public String bucketName() {
        return id.bucket();
    }

    /**
     * Returns the S3 region in {@link Region} format.
     * <p>
     *     Do not use this method if {@link S3Link} is created with a region not in {@link Region} enum.
     *     Use {@link #getRegion()} instead.
     * </p>
     *
     * @return S3 region.
     */
    public Region s3Region() {
        return Region.of(getRegion());
    }

    /**
     * Returns the S3 region as string.
     *
     * @return region provided when creating the S3Link object.
     *         If no region is provided during S3Link creation, returns us-east-1.
     */
    public String getRegion() {
        return id.getRegionId() == null ? "us-east-1" : id.getRegionId();
    }

    /**
     * Serializes into a JSON string.
     *
     * @return The string representation of the link to the S3 resource.
     */
    public String toJson() {
        return id.toJson();
    }

    public S3Client getAmazonS3Client() {
        return s3cc.getClient(getRegion());
    }

    /**
     * Convenience method to synchronously upload from the given file to the
     * Amazon S3 object represented by this S3Link.
     *
     * @param source
     *            source file to upload from
     *
     * @return A {@link PutObjectResponse} object containing the information
     *         returned by Amazon S3 for the newly created object.
     */
    public PutObjectResponse uploadFrom(final File source) {
        return getAmazonS3Client().putObject(PutObjectRequest.builder()
                                                             .bucket(bucketName())
                                                             .key(getKey())
                                                             .build(), RequestBody.fromFile(source));
    }

    /**
     * Convenience method to synchronously upload from the given buffer to the
     * Amazon S3 object represented by this S3Link.
     *
     * @param buffer
     *            The buffer containing the data to upload.
     *
     * @return A {@link PutObjectResponse} object containing the information
     *         returned by Amazon S3 for the newly created object.
     */
    public PutObjectResponse uploadFrom(final byte[] buffer) {
        return getAmazonS3Client().putObject(PutObjectRequest.builder()
                                                             .bucket(bucketName())
                                                             .key(getKey())
                                                             .contentLength((long) buffer.length)
                                                             .build(), RequestBody.fromBytes(buffer));
    }

    /**
     * Sets the access control list for the object represented by this S3Link.
     *
     * Note: Executing this method requires that the object already exists in
     * Amazon S3.
     *
     * @param acl
     *            The access control list describing the new permissions for the
     *            object represented by this S3Link.
     */
    public void setAcl(ObjectCannedACL acl) {
        getAmazonS3Client().putObjectAcl(PutObjectAclRequest.builder().bucket(bucketName()).key(getKey()).acl(acl).build());
    }

    public void setAcl(AccessControlPolicy acl) {
        getAmazonS3Client().putObjectAcl(PutObjectAclRequest.builder()
                                                            .accessControlPolicy(acl)
                                                            .bucket(bucketName())
                                                            .key(getKey())
                                                            .build());
    }

    /**
     * Convenient method to synchronously download to the specified file from
     * the S3 object represented by this S3Link.
     *
     * @param destination destination file to download to
     *
     * @return All S3 object metadata for the specified object.
     *     Returns null if constraints were specified but not met.
     */
    public GetObjectResponse downloadTo(final File destination) {
        return getAmazonS3Client().getObject(GetObjectRequest.builder()
                                                             .bucket(bucketName())
                                                             .key(getKey())
                                                             .build(),
                                             ResponseTransformer.toFile(destination.toPath()));
    }

    /**
     * Downloads the data from the object represented by this S3Link to the
     * specified output stream.
     *
     * @param output
     *            The output stream to write the object's data to.
     *
     * @return The object's metadata.
     */
    public GetObjectResponse downloadTo(final OutputStream output) {
        return getAmazonS3Client().getObject(GetObjectRequest.builder()
                                                             .bucket(bucketName())
                                                             .key(getKey())
                                                             .build(),
                                             ResponseTransformer.toOutputStream(output));
    }

    /**
     * JSON wrapper of an {@link S3Link} identifier,
     * which consists of the S3 region id, bucket name and key.
     * Sample JSON serialized form:
     * <pre>
     * {"s3":{"bucket":"mybucket","key":"mykey","region":"us-west-2"}}
     * {"s3":{"bucket":"mybucket","key":"mykey","region":null}}
     * </pre>
     * Note for S3 a null region means US standard.
     * <p>
     *  @see Region
     */
    static class Id {
        @JsonProperty("s3")
        private S3 s3;

        Id() {
        } // used by Jackson to unmarshall

        Id(String bucketName, String key) {
            this.s3 = new S3(bucketName, key);
        }

        Id(String region, String bucketName, String key) {
            this.s3 = new S3(region, bucketName, key);
        }

        Id(S3 s3) {
            this.s3 = s3;
        }

        @JsonProperty("s3")
        public S3 s3() {
            return s3;
        }

        @JsonIgnore
        public String getRegionId() {
            return s3.getRegionId();
        }

        @JsonIgnore
        public String bucket() {
            return s3.bucket();
        }

        @JsonIgnore
        public String getKey() {
            return s3.getKey();
        }

        String toJson() {
            return JacksonUtils.toJsonString(this);
        }
    }

    /**
     * Internal class for JSON serialization purposes.
     * <p>
     * @see Id
     */
    private static class S3 {

        /**
         * The name of the S3 bucket containing the object to retrieve.
         */
        @JsonProperty("bucket")
        private String bucket;

        /**
         * The key under which the desired S3 object is stored.
         */
        @JsonProperty("key")
        private String key;

        /**
         * The region id of {@link Region} where the S3 object is stored.
         */
        @JsonProperty("region")
        private String regionId;

        @SuppressWarnings("unused")
        S3() {
        }  // used by Jackson to unmarshall

        /**
         * Constructs a new {@link S3} with all the required parameters.
         *
         * @param bucket
         *            The name of the bucket containing the desired object.
         * @param key
         *            The key in the specified bucket under which the object is
         *            stored.
         */
        S3(String bucket, String key) {
            this(null, bucket, key);
        }

        /**
         * Constructs a new {@link S3} with all the required parameters.
         *
         * @param region
         *            The region where the S3 object is stored.
         * @param bucket
         *            The name of the bucket containing the desired object.
         * @param key
         *            The key in the specified bucket under which the object is
         *            stored.
         */
        S3(String region, String bucket, String key) {
            this.regionId = region;
            this.bucket = bucket;
            this.key = key;
        }

        /**
         * Gets the name of the bucket containing the object to be downloaded.
         *
         * @return The name of the bucket containing the object to be downloaded.
         */
        @JsonProperty("bucket")
        public String bucket() {
            return bucket;
        }

        /**
         * Gets the key under which the object to be downloaded is stored.
         *
         * @return The key under which the object to be downloaded is stored.
         */
        @JsonProperty("key")
        public String getKey() {
            return key;
        }

        @JsonProperty("region")
        public String getRegionId() {
            return regionId;
        }
    }

    /**
     * {@link S3Link} factory.
     */
    public static final class Factory implements DynamoDbTypeConverter<String, S3Link> {
        static final Factory DEFAULT = new Factory((S3ClientCache) null);
        private final S3ClientCache s3cc;

        public Factory(final S3ClientCache s3cc) {
            this.s3cc = s3cc;
        }

        public static Factory of(final AwsCredentialsProvider provider) {
            return provider == null ? DEFAULT : new Factory(new S3ClientCache(provider));
        }

        public S3Link createS3Link(Region s3region, String bucketName, String key) {
            return createS3Link(convertRegionToString(s3region, bucketName), bucketName, key);
        }

        public S3Link createS3Link(String s3region, String bucketName, String key) {
            if (s3ClientCache() == null) {
                throw new IllegalStateException("Mapper must be constructed with S3 AWS Credentials to create S3Link");
            }
            return new S3Link(s3ClientCache(), s3region, bucketName, key);
        }

        public S3ClientCache s3ClientCache() {
            return this.s3cc;
        }

        @Override
        public String convert(final S3Link o) {
            return o.bucketName() == null || o.getKey() == null ? null : o.toJson();
        }

        @Override
        public S3Link unconvert(final String o) {
            return S3Link.fromJson(s3ClientCache(), o);
        }
    }

}