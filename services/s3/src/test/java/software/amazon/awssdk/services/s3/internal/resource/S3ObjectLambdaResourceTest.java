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

package software.amazon.awssdk.services.s3.internal.resource;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link S3ObjectLambdaResource}
 */
public class S3ObjectLambdaResourceTest {
    private static final String PARTITION = "partition";
    private static final String REGION = "region";
    private static final String ACCOUNT_ID = "123456789012";
    private static final String ACCESS_POINT_NAME = "object-lambda";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void build_allPropertiesSet_returnedFromObject() {
        S3ObjectLambdaResource accessPoint = S3ObjectLambdaResource.builder()
                                                                     .partition(PARTITION)
                                                                     .region(REGION)
                                                                     .accountId(ACCOUNT_ID)
                                                                     .accessPointName(ACCESS_POINT_NAME)
                                                                     .build();
        assertThat(accessPoint.partition().get(), equalTo(PARTITION));
        assertThat(accessPoint.region().get(), equalTo(REGION));
        assertThat(accessPoint.accountId().get(), equalTo(ACCOUNT_ID));
        assertThat(accessPoint.accessPointName(), equalTo(ACCESS_POINT_NAME));
    }

    @Test
    public void build_noPartitionSet_throwsNullPointerException() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("partition");

        S3ObjectLambdaResource.builder()
                               .region(REGION)
                               .accountId(ACCOUNT_ID)
                               .accessPointName(ACCESS_POINT_NAME)
                               .build();
    }

    @Test
    public void build_noRegionSet_throwsNullPointerException() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("region");

        S3ObjectLambdaResource.builder()
                               .partition(PARTITION)
                               .accountId(ACCOUNT_ID)
                               .accessPointName(ACCESS_POINT_NAME)
                               .build();
    }

    @Test
    public void build_noAccountIdSet_throwsNullPointerException() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("account");

        S3ObjectLambdaResource.builder()
                               .partition(PARTITION)
                               .region(REGION)
                               .accessPointName(ACCESS_POINT_NAME)
                               .build();
    }

    @Test
    public void build_noAccessPointNameSet_throwsNullPointerException() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("accessPointName");

        S3ObjectLambdaResource.builder()
                               .partition(PARTITION)
                               .region(REGION)
                               .accountId(ACCOUNT_ID)
                               .build();
    }

    @Test
    public void build_noPartitionSet_throwsIllegalArgumentException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("partition");

        S3ObjectLambdaResource.builder()
                               .partition("")
                               .region(REGION)
                               .accountId(ACCOUNT_ID)
                               .accessPointName(ACCESS_POINT_NAME)
                               .build();
    }

    @Test
    public void build_noRegionSet_throwsIllegalArgumentException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("region");

        S3ObjectLambdaResource.builder()
                               .partition(PARTITION)
                               .region("")
                               .accountId(ACCOUNT_ID)
                               .accessPointName(ACCESS_POINT_NAME)
                               .build();
    }

    @Test
    public void build_noAccountIdSet_throwsIllegalArgumentException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("account");

        S3ObjectLambdaResource.builder()
                               .partition(PARTITION)
                               .region(REGION)
                               .accountId("")
                               .accessPointName(ACCESS_POINT_NAME)
                               .build();
    }

    @Test
    public void build_noAccessPointNameSet_throwsIllegalArgumentException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("accessPointName");

        S3ObjectLambdaResource.builder()
                               .partition(PARTITION)
                               .region(REGION)
                               .accountId(ACCOUNT_ID)
                               .accessPointName("")
                               .build();
    }

    @Test
    public void hashCode_sameValues_equal() {
        S3ObjectLambdaResource resource1 = S3ObjectLambdaResource.builder()
                                                                   .partition(PARTITION)
                                                                   .region(REGION)
                                                                   .accountId(ACCOUNT_ID)
                                                                   .accessPointName(ACCESS_POINT_NAME)
                                                                   .build();

        S3ObjectLambdaResource resource2 = S3ObjectLambdaResource.builder()
                                                                   .partition(PARTITION)
                                                                   .region(REGION)
                                                                   .accountId(ACCOUNT_ID)
                                                                   .accessPointName(ACCESS_POINT_NAME)
                                                                   .build();

        assertThat(resource1.hashCode(), equalTo(resource2.hashCode()));
    }

    @Test
    public void hashCode_incorporatesAllMembers_notEqual() {
        String dummy = "foo";

        S3ObjectLambdaResource accessPoint = S3ObjectLambdaResource.builder()
                                                                     .partition(PARTITION)
                                                                     .region(REGION)
                                                                     .accountId(ACCOUNT_ID)
                                                                     .accessPointName(ACCESS_POINT_NAME)
                                                                     .build();

        S3ObjectLambdaResource partitionDifferent = S3ObjectLambdaResource.builder()
                                                                            .partition(dummy)
                                                                            .region(REGION)
                                                                            .accountId(ACCOUNT_ID)
                                                                            .accessPointName(ACCESS_POINT_NAME)
                                                                            .build();

        assertThat(accessPoint.hashCode(), not(equalTo(partitionDifferent.hashCode())));

        S3ObjectLambdaResource regionDifferent = S3ObjectLambdaResource.builder()
                                                                         .partition(PARTITION)
                                                                         .region(dummy)
                                                                         .accountId(ACCOUNT_ID)
                                                                         .accessPointName(ACCESS_POINT_NAME)
                                                                         .build();

        assertThat(accessPoint.hashCode(), not(equalTo(regionDifferent.hashCode())));

        S3ObjectLambdaResource accountDifferent = S3ObjectLambdaResource.builder()
                                                                          .partition(PARTITION)
                                                                          .region(REGION)
                                                                          .accountId(dummy)
                                                                          .accessPointName(ACCESS_POINT_NAME)
                                                                          .build();

        assertThat(accessPoint.hashCode(), not(equalTo(accountDifferent.hashCode())));

        S3ObjectLambdaResource accessPointNameDifferent = S3ObjectLambdaResource.builder()
                                                                                  .partition(PARTITION)
                                                                                  .region(REGION)
                                                                                  .accountId(ACCOUNT_ID)
                                                                                  .accessPointName(dummy)
                                                                                  .build();

        assertThat(accessPoint.hashCode(), not(equalTo(accessPointNameDifferent.hashCode())));
    }

    @Test
    public void equals_sameValues_isTrue() {
        S3ObjectLambdaResource resource1 = S3ObjectLambdaResource.builder()
                                                                   .partition(PARTITION)
                                                                   .region(REGION)
                                                                   .accountId(ACCOUNT_ID)
                                                                   .accessPointName(ACCESS_POINT_NAME)
                                                                   .build();

        S3ObjectLambdaResource resource2 = S3ObjectLambdaResource.builder()
                                                                   .partition(PARTITION)
                                                                   .region(REGION)
                                                                   .accountId(ACCOUNT_ID)
                                                                   .accessPointName(ACCESS_POINT_NAME)
                                                                   .build();

        assertThat(resource1.equals(resource2), is(true));
    }

    @Test
    public void equals_incorporatesAllMembers_isFalse() {
        String dummy = "foo";

        S3ObjectLambdaResource accessPoint = S3ObjectLambdaResource.builder()
                                                                     .partition(PARTITION)
                                                                     .region(REGION)
                                                                     .accountId(ACCOUNT_ID)
                                                                     .accessPointName(ACCESS_POINT_NAME)
                                                                     .build();

        S3ObjectLambdaResource partitionDifferent = S3ObjectLambdaResource.builder()
                                                                            .partition(dummy)
                                                                            .region(REGION)
                                                                            .accountId(ACCOUNT_ID)
                                                                            .accessPointName(ACCESS_POINT_NAME)
                                                                            .build();

        assertThat(accessPoint.equals(partitionDifferent), is(false));

        S3ObjectLambdaResource regionDifferent = S3ObjectLambdaResource.builder()
                                                                         .partition(PARTITION)
                                                                         .region(dummy)
                                                                         .accountId(ACCOUNT_ID)
                                                                         .accessPointName(ACCESS_POINT_NAME)
                                                                         .build();

        assertThat(accessPoint.equals(regionDifferent), is(false));

        S3ObjectLambdaResource accountDifferent = S3ObjectLambdaResource.builder()
                                                                          .partition(PARTITION)
                                                                          .region(REGION)
                                                                          .accountId(dummy)
                                                                          .accessPointName(ACCESS_POINT_NAME)
                                                                          .build();

        assertThat(accessPoint.equals(accountDifferent), is(false));

        S3ObjectLambdaResource accessPointNameDifferent = S3ObjectLambdaResource.builder()
                                                                                  .partition(PARTITION)
                                                                                  .region(REGION)
                                                                                  .accountId(ACCOUNT_ID)
                                                                                  .accessPointName(dummy)
                                                                                  .build();

        assertThat(accessPoint.equals(accessPointNameDifferent), is(false));
    }

}
