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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.UUID;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.pojos.S3LinksTestClass;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;

@Ignore
// Revisit S3
public class DynamoDBS3IntegrationTest extends DynamoDBS3IntegrationTestBase {

    private static final long OBJECT_SIZE = 123;

    @Test
    public void testCredentialContext() throws Exception {
        tryCreateItem(new DynamoDbMapper(dynamo, CREDENTIALS_PROVIDER_CHAIN));
    }

    @Test
    public void testManuallyFilledContext() throws Exception {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo, CREDENTIALS_PROVIDER_CHAIN);
        S3ClientCache s3cc = mapper.s3ClientCache();
        s3cc.useClient(s3East, Region.US_EAST_1);
        s3cc.useClient(s3West, Region.US_WEST_2);
        tryCreateItem(mapper);
    }

    public void tryCreateItem(DynamoDbMapper mapper) throws Exception {
        String westKey = UUID.randomUUID().toString();
        String eastKey = UUID.randomUUID().toString();

        S3LinksTestClass obj = new S3LinksTestClass();
        obj.setKey("" + ++startKey);
        S3Link linkWest = mapper.createS3Link(Region.US_WEST_2, DynamoDBS3IntegrationTestBase.WEST_BUCKET, westKey);
        obj.setS3LinkWest(linkWest);
        mapper.save(obj);
        obj = mapper.load(S3LinksTestClass.class, obj.getKey());

        assertObjectDoesntExist(s3West, obj.s3LinkWest().bucketName(), westKey);

        linkWest.getAmazonS3Client().putObject(PutObjectRequest.builder()
                                                               .bucket(linkWest.bucketName())
                                                               .key(linkWest.getKey())
                                                               .build(),
                                               RequestBody.fromFile(new RandomTempFile(westKey, OBJECT_SIZE)));

        assertObjectExists(s3West, obj.s3LinkWest().bucketName(), westKey);

        S3Link linkEast = mapper.createS3Link(Region.US_EAST_1, DynamoDBS3IntegrationTestBase.EAST_BUCKET, eastKey);
        obj.setS3LinkEast(linkEast);
        assertObjectDoesntExist(s3East, obj.s3LinkEast().bucketName(), eastKey);

        linkEast.getAmazonS3Client().putObject(PutObjectRequest.builder()
                                                               .bucket(linkEast.bucketName())
                                                               .key(linkEast.getKey())
                                                               .build(),
                                               RequestBody.fromFile(new RandomTempFile(westKey, OBJECT_SIZE)));
        mapper.save(obj);

        assertObjectExists(s3West, obj.s3LinkWest().bucketName(), westKey);
        assertObjectExists(s3East, obj.s3LinkEast().bucketName(), eastKey);

        obj = mapper.load(S3LinksTestClass.class, obj.getKey());

        assertEquals(westKey, obj.s3LinkWest().getKey());
        assertEquals(eastKey, obj.s3LinkEast().getKey());
        System.err.println(obj.s3LinkWest().toJson());
        System.err.println(obj.s3LinkEast().toJson());
        mapper.delete(obj);

        // Test the convenience methods on S3Link
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        linkEast.downloadTo(baos);
        assertEquals(OBJECT_SIZE, baos.toByteArray().length);
    }
}