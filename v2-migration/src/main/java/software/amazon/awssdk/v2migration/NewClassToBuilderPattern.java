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

package software.amazon.awssdk.v2migration;

import java.util.Arrays;
import java.util.List;
import org.openrewrite.Recipe;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Recipe that converts objects creation using {@code new} such as
 *
 * {@snippet :
 *     SendMessageRequest sendMessage = new SendMessageRequest()
 *             .withQueueUrl("url")
 *             .withMessageBody("hello world")
 *             .withMessageGroupId("my-group");
 *
 *     sqs.sendMessage(sendMessage);
 * }
 *
 * to builder pattern:
 *
 * {@snippet :
 *     SendMessageRequest sendMessage = SendMessageRequest.builder()
 *             .queueUrl("url")
 *             .messageBody("hello world")
 *             .messageGroupId("my-group").build();
 *
 *     sqs.sendMessage(sendMessage);
 * }
 */
@SdkInternalApi
public class NewClassToBuilderPattern extends Recipe {
    @Override
    public String getDisplayName() {
        return "Change new objects creation to Builder pattern";
    }

    @Override
    public String getDescription() {
        return "Transform the creation of a class using 'new' to builder pattern.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new NewClassToBuilder(),
                new V1SetterToV2()
        );
    }
}
