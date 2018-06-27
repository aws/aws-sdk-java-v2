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

package software.amazon.awssdk.services.iam;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Before;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.User;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Base class for IAM integration tests. Provides convenience methods for
 * creating test data, and automatically loads AWS credentials from a properties
 * file on disk and instantiates clients for the individual tests to use.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class IntegrationTestBase extends AwsTestBase {

    /** The IAM client for all tests to use. */
    protected IamClient iam;

    /**
     * Loads the AWS account info for the integration tests and creates an
     * IAM client for tests to use.
     */
    @Before
    public void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        iam = IamClient.builder()
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .overrideConfiguration(c -> c.retryPolicy(RetryPolicy.builder().numRetries(50).build()))
                       .region(Region.AWS_GLOBAL)
                       .build();
        System.out.println(iam);
    }

    void waitForUsersToBeCreated(String... users) {
        Stream.of(users).forEach(user -> Waiter.run(() -> iam.getUser(r -> r.userName(user)))
                                               .ignoringException(IamException.class)
                                               .orFail());
    }

    void waitForGroupsToBeCreated(String... groups) {
        Stream.of(groups).forEach(user -> Waiter.run(() -> iam.getGroup(r -> r.groupName(user)))
                                                .ignoringException(IamException.class)
                                                .orFail());
    }

    void deleteGroupsAndTheirUsersQuietly(String... groupNames) {
        Stream.of(groupNames)
              .forEach(groupName -> quietly(() -> {
                  List<User> usersInGroup = iam.getGroup(g -> g.groupName(groupName)).users();
                  usersInGroup.forEach(user -> {
                      iam.removeUserFromGroup(r -> r.userName(user.userName()).groupName(groupName));
                      iam.deleteUser(r -> r.userName(user.userName()));
                  });
                  iam.deleteGroup(r -> r.groupName(groupName));
              }));
    }
    
    private void quietly(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            // Well, we tried.
        }
    }
}
