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

package software.amazon.awssdk.services.elasticache;

import org.junit.BeforeClass;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class ElastiCacheIntegrationTestBase extends AwsTestBase {

    protected static final String MEMCACHED_ENGINE = "memcached";
    protected static final String REDIS_ENGINE = "redis";

    protected static ElastiCacheClient elasticache;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        elasticache = ElastiCacheClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }
}
