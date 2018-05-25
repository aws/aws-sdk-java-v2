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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AdvancedConfiguration;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * A smart Map for {@link S3Client} objects. {@link S3ClientCache} keeps the
 * clients organized by region, and if provided {@link AwsCredentials} will
 * create clients on the fly. Otherwise it just return clients given to it with
 * {@link #useClient(S3Client, Region)}.
 */
public class S3ClientCache {
    private final ConcurrentMap<String, S3Client> clientsByRegion = new ConcurrentHashMap<String, S3Client>();

    private final AwsCredentialsProvider awscredentialsProvider;

    @Deprecated
    S3ClientCache(AwsCredentials credentials) {
        this(StaticCredentialsProvider.create(credentials));
    }

    /**
     * Create a client cache using the given AWSCredentialsProvider. If
     * {@link #getClient(Region)} is called and a client has not been
     * provided for the region, the cache will instantiate one from the
     * provided {@link AwsCredentialsProvider}.
     *
     * @param awsCredentialsProvider
     *            The credentials provider to use when creating new
     *            {@link S3Client}.
     */
    S3ClientCache(AwsCredentialsProvider awsCredentialsProvider) {
        this.awscredentialsProvider = awsCredentialsProvider;
    }


    /**
     * Force the client cache to provide a certain client for the region which
     * that client is configured. This can be useful to provide clients with
     * different {@link S3AdvancedConfiguration}.
     *
     * @param client
     *            An {@link S3Client} to use in the cache. Its region will
     *            be detected automatically.
     */
    public void useClient(S3Client client, Region region) {
        clientsByRegion.put(region.value(), client);
    }

    /**
     * Returns a client for the requested region, or throws an exception when
     * unable.
     *
     * @param region
     *            The region the returned {@link S3Client} will be
     *            configured to use.
     * @return A client for the given region from the cache, either instantiated
     *         automatically from the provided {@link AwsCredentials} or
     *         provided with {@link #useClient(S3Client, Region)}.
     * @throws IllegalArgumentException
     *             When a region is requested that has not been provided to the
     *             cache with {@link #useClient(S3Client, Region)}, and the cache
     *             has no {@link AwsCredentials} with which a client may be
     *             instantiated.
     */
    public S3Client getClient(Region region) {
        if (region == null) {
            throw new IllegalArgumentException("S3 region must be specified");
        }
        return getClient(region.value());
    }

    /**
     * Returns a client for the requested region, or throws an exception when
     * unable.
     *
     * @param region
     *            The region the returned {@link S3Client} will be
     *            configured to use.
     * @return A client for the given region from the cache, either instantiated
     *         automatically from the provided {@link AwsCredentials} or
     *         provided with {@link #useClient(S3Client, Region)}.
     * @throws IllegalArgumentException
     *             When a region is requested that has not been provided to the
     *             cache with {@link #useClient(S3Client, Region)}, and the cache
     *             has no {@link AwsCredentials} with which a client may be
     *             instantiated.
     */
    public S3Client getClient(String region) {
        if (region == null) {
            throw new IllegalArgumentException("S3 region must be specified");
        }
        S3Client client = clientsByRegion.get(region);
        return client != null ? client : cacheClient(region);
    }

    /**
     * Returns a new client with region configured to
     * region.
     * Also updates the clientsByRegion map by associating the
     * new client with region.
     *
     * @param region
     *            The region the returned {@link S3Client} will be
     *            configured to use.
     * @return A new {@link S3Client} client with region set to region.
     */
    private S3Client cacheClient(String region) {
        if (awscredentialsProvider == null) {
            throw new IllegalArgumentException("No credentials provider found to connect to S3");
        }
        S3Client client = S3Client.builder().credentialsProvider(awscredentialsProvider).region(Region.of(region)).build();
        clientsByRegion.put(region, client);
        return client;
    }
}