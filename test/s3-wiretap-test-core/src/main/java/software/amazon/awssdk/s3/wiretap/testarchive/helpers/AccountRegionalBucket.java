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

package software.amazon.awssdk.s3.wiretap.testarchive.helpers;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortIncompleteMultipartUpload;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.BucketNamespace;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.EncryptionType;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionResponse;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.LifecycleExpiration;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.NoncurrentVersionExpiration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionRule;
import software.amazon.awssdk.services.sts.StsClient;

@Accessors(fluent = true)
public final class AccountRegionalBucket {

    private static final Logger LOG = LogManager.getLogger(AccountRegionalBucket.class);

    private static final String NAMESPACE_SUFFIX = "-an";
    private static final String MOCK_ACCOUNT_ID = "000000000000";
    private static final int MAX_NAME_LENGTH = 63;

    private static final int EXPIRE_OBJECTS_AFTER_DAYS = 7;
    private static final int ABORT_INCOMPLETE_MPU_AFTER_DAYS = 1;
    private static final int EXPIRE_NONCURRENT_VERSIONS_AFTER_DAYS = 1;

    private static final Region FALLBACK_REGION = Region.US_WEST_2;

    public enum Flavor {
        PLAIN("wiretap-sdk-testing", false),
        VERSIONED("wiretap-sdk-versioned", true);

        @Getter
        @Accessors(fluent = true)
        private final String prefix;
        @Getter
        @Accessors(fluent = true)
        private final boolean versioned;

        Flavor(String prefix, boolean versioned) {
            this.prefix = prefix;
            this.versioned = versioned;
        }
    }

    public static final AccountRegionalBucket MOCK =
            new AccountRegionalBucket(Flavor.PLAIN, FALLBACK_REGION, MOCK_ACCOUNT_ID);

    public static final AccountRegionalBucket MOCK_VERSIONED =
            new AccountRegionalBucket(Flavor.VERSIONED, FALLBACK_REGION, MOCK_ACCOUNT_ID);

    private static final Object LOCK = new Object();
    private static final Map<Flavor, AccountRegionalBucket> RESOLVED = new EnumMap<>(Flavor.class);
    private static final Map<Flavor, IllegalStateException> FAILURE = new EnumMap<>(Flavor.class);
    private static volatile AwsCredentialsProvider CREDENTIALS;

    @Getter
    private final Flavor flavor;
    @Getter
    private final Region region;
    @Getter
    private final String accountId;
    @Getter
    private final String name;

    private AccountRegionalBucket(final Flavor flavor, final Region region,
                                  final String accountId) {
        this.flavor = flavor;
        this.region = region;
        this.accountId = accountId;
        this.name = nameFor(flavor, region, accountId);
    }

    public boolean versioned() {
        return flavor.versioned();
    }

    static String nameFor(final Flavor flavor, final Region region, final String accountId) {
        String name = flavor.prefix() + "-" + accountId + "-" + region.id() + NAMESPACE_SUFFIX;
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalStateException("Derived bucket name is " + name.length()
                    + " characters, over S3's limit of " + MAX_NAME_LENGTH + ": " + name
                    + " — shorten " + flavor + "'s prefix in AccountRegionalBucket.Flavor.");
        }
        return name;
    }

    public static AccountRegionalBucket resolved() {
        return resolved(Flavor.PLAIN);
    }

    public static AccountRegionalBucket resolvedVersioned() {
        return resolved(Flavor.VERSIONED);
    }

    public static AccountRegionalBucket resolved(final Flavor flavor) {
        synchronized (LOCK) {
            IllegalStateException failure = FAILURE.get(flavor);
            if (failure != null) {
                throw new IllegalStateException(failure.getMessage(), failure);
            }
            AccountRegionalBucket cached = RESOLVED.get(flavor);
            if (cached != null) {
                return cached;
            }
            try {
                AccountRegionalBucket resolved = resolve(flavor);
                RESOLVED.put(flavor, resolved);
                return resolved;
            } catch (RuntimeException e) {
                IllegalStateException wrapped = e instanceof IllegalStateException
                        ? (IllegalStateException) e
                        : new IllegalStateException(e.getMessage(), e);
                FAILURE.put(flavor, wrapped);
                throw wrapped;
            }
        }
    }

    public static AwsCredentialsProvider credentials() {
        synchronized (LOCK) {
            if (CREDENTIALS == null) {
                CREDENTIALS = DefaultCredentialsProvider.create();
            }
            return CREDENTIALS;
        }
    }

    private static AccountRegionalBucket resolve(final Flavor flavor) {
        Region region = resolveRegion();
        AccountRegionalBucket bucket =
                new AccountRegionalBucket(flavor, region, accountId(region));
        ensureExists(bucket);
        LOG.info("real-S3 target: bucket {} (flavor {}, account {}, region {})",
                bucket.name(), flavor, bucket.accountId(), region);
        return bucket;
    }

    private static Region resolveRegion() {
        try {
            Region region = new DefaultAwsRegionProviderChain().getRegion();
            LOG.info("region {} resolved from the environment (AWS_REGION / profile)", region);
            return region;
        } catch (SdkException e) {
            LOG.info("no region configured in the environment, falling back to {} — set AWS_REGION"
                    + " to run the real-S3 tests elsewhere", FALLBACK_REGION);
            return FALLBACK_REGION;
        }
    }

    private static String accountId(final Region region) {
        try (StsClient sts = StsClient.builder()
                .region(region)
                .credentialsProvider(credentials())
                .httpClientBuilder(ApacheHttpClient.builder())
                .build()) {
            return sts.getCallerIdentity().account();
        } catch (SdkException e) {
            throw new IllegalStateException("Could not identify the AWS account for the real-S3"
                    + " (FORWARD) tests via sts:GetCallerIdentity in " + region + ". These tests"
                    + " take credentials from the default chain: set AWS_PROFILE=<profile>, or"
                    + " refresh them (ada credentials update --account <id> --role <role>"
                    + " --once); or run the sweep with -PmockOnly to skip the real-S3 rows.", e);
        }
    }

    private static void ensureExists(final AccountRegionalBucket bucket) {
        try (S3Client s3 = S3Client.builder()
                .region(bucket.region())
                .credentialsProvider(credentials())
                .httpClientBuilder(ApacheHttpClient.builder())
                .build()) {
            CreateBucketRequest.Builder create = CreateBucketRequest.builder()
                    .bucket(bucket.name())
                    .bucketNamespace(BucketNamespace.ACCOUNT_REGIONAL);
            if (!Region.US_EAST_1.equals(bucket.region())) {
                create.createBucketConfiguration(c -> c.locationConstraint(bucket.region().id()));
            }
            boolean created = true;
            try {
                s3.createBucket(create.build());
            } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
                LOG.info("bucket {} already exists", bucket.name());
                created = false;
            } catch (SdkException e) {
                throw new IllegalStateException("Could not create the account-regional bucket "
                        + bucket.name() + " in " + bucket.region() + ". The real-S3 (FORWARD)"
                        + " tests provision their own bucket, which needs s3:CreateBucket and"
                        + " s3:PutLifecycleConfiguration (plus s3:GetBucketVersioning and"
                        + " s3:PutBucketVersioning for the versioned one); run with -PmockOnly to"
                        + " skip them.", e);
            }
            if (created) {
                LOG.info("created bucket {}", bucket.name());
                expireObjects(s3, bucket);
            }
            if (bucket.versioned()) {
                ensureVersioned(s3, bucket);
            }
            ensureSseCAllowed(s3, bucket);
        }
    }

    private static void ensureSseCAllowed(final S3Client s3, final AccountRegionalBucket bucket) {
        GetBucketEncryptionResponse enc;
        try {
            enc = s3.getBucketEncryption(b -> b.bucket(bucket.name()));
        } catch (S3Exception e) {
            if (e.awsErrorDetails() != null
                    && "ServerSideEncryptionConfigurationNotFoundError".equals(e.awsErrorDetails().errorCode())) {
                return;
            }
            LOG.warn("could not read the default-encryption configuration of bucket {} ({}); leaving it"
                            + " unchanged. Only the SSE-C repro needs it — that test will fail with a 403"
                            + " if SSE-C is blocked. Reading it needs s3:GetEncryptionConfiguration.",
                    bucket.name(), e.awsErrorDetails() == null ? e.toString() : e.awsErrorDetails().errorCode());
            return;
        }
        var config = enc.serverSideEncryptionConfiguration();
        if (config.rules().stream().noneMatch(AccountRegionalBucket::blocksSseC)) {
            return;
        }
        var rewritten = config.rules().stream()
                .map(r -> blocksSseC(r)
                        ? r.toBuilder()
                                .blockedEncryptionTypes(b -> b.encryptionType(EncryptionType.NONE))
                                .build()
                        : r)
                .toList();
        try {
            s3.putBucketEncryption(b -> b.bucket(bucket.name())
                    .serverSideEncryptionConfiguration(c -> c.rules(rewritten)));
        } catch (S3Exception e) {
            LOG.warn("could not permit SSE-C uploads on bucket {} ({}); the SSE-C repro will fail with a"
                            + " 403. Changing it needs s3:PutEncryptionConfiguration.",
                    bucket.name(), e.awsErrorDetails() == null ? e.toString() : e.awsErrorDetails().errorCode());
            return;
        }
        LOG.info("bucket {} now permits SSE-C uploads (blocked encryption types set to [NONE], was [SSE-C])",
                bucket.name());
    }

    private static boolean blocksSseC(final ServerSideEncryptionRule rule) {
        return rule.blockedEncryptionTypes() != null
                && rule.blockedEncryptionTypes().encryptionType().contains(EncryptionType.SSE_C);
    }

    private static void ensureVersioned(final S3Client s3, final AccountRegionalBucket bucket) {
        GetBucketVersioningResponse current =
                s3.getBucketVersioning(b -> b.bucket(bucket.name()));
        if (current.status() == BucketVersioningStatus.ENABLED) {
            return;
        }
        s3.putBucketVersioning(b -> b.bucket(bucket.name())
                .versioningConfiguration(c -> c.status(BucketVersioningStatus.ENABLED)));
        LOG.info("bucket {} now has versioning ENABLED (was {})",
                bucket.name(), current.status());
    }

    private static void expireObjects(final S3Client s3, final AccountRegionalBucket bucket) {
        String name = bucket.name();
        LifecycleRule.Builder rule = LifecycleRule.builder()
                .id("DeleteAfter" + EXPIRE_OBJECTS_AFTER_DAYS + "Days")
                .status(ExpirationStatus.ENABLED)
                .filter(LifecycleRuleFilter.builder().prefix("").build())
                .expiration(LifecycleExpiration.builder().days(EXPIRE_OBJECTS_AFTER_DAYS).build())
                .abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder()
                        .daysAfterInitiation(ABORT_INCOMPLETE_MPU_AFTER_DAYS).build());
        if (bucket.versioned()) {
            rule.noncurrentVersionExpiration(NoncurrentVersionExpiration.builder()
                    .noncurrentDays(EXPIRE_NONCURRENT_VERSIONS_AFTER_DAYS).build());
        }
        LifecycleRule built = rule.build();
        s3.putBucketLifecycleConfiguration(b -> b
                .bucket(name)
                .lifecycleConfiguration(c -> c.rules(List.of(built))));
        LOG.info("bucket {} expires objects after {} days{}", name, EXPIRE_OBJECTS_AFTER_DAYS,
                bucket.versioned()
                        ? " and noncurrent versions after " + EXPIRE_NONCURRENT_VERSIONS_AFTER_DAYS
                        : "");
    }
}
