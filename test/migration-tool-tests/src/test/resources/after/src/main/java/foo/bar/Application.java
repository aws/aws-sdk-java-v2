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

package foo.bar;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;

public class Application {

    private Application() {

    }

    public static void main(String... args) {
        SqsClient sqs = SqsClient.builder()
                                       .region(Region.US_WEST_2)
                                       .credentials(CredentialsDependencyFactory.defaultCredentialsProviderChain())
                                       .build();
        ListQueuesRequest request = ListQueuesRequest.builder()
            .maxResults(5)
            .queueNamePrefix("MyQueue-")
            .nextToken("token").build();
        ListQueuesResponse listQueuesResult = sqs.listQueues(request);
        System.out.println(listQueuesResult);
    }
}
