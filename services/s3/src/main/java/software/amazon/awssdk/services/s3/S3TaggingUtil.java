/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.util.SdkHttpUtils;

@ReviewBeforeRelease("This is not hooked up yet. Implement customization to have PutObject tagging member" +
                     "be a map and have the SDK handle marshalling/encoding.")
public class S3TaggingUtil {

    public static String toQueryString(Tagging tagging) {
        StringBuilder tagBuilder = new StringBuilder();

        Tagging taggingClone = tagging.toBuilder().build();

        Tag firstTag = taggingClone.tagSet().remove(0);
        tagBuilder.append(SdkHttpUtils.urlEncode(firstTag.key(), false));
        tagBuilder.append("=");
        tagBuilder.append(SdkHttpUtils.urlEncode(firstTag.value(), false));

        taggingClone.tagSet().forEach(t -> {
            tagBuilder.append("&");
            tagBuilder.append(SdkHttpUtils.urlEncode(t.key(), false));
            tagBuilder.append("=");
            tagBuilder.append(SdkHttpUtils.urlEncode(t.value(), false));
        });

        return tagBuilder.toString();
    }
}
