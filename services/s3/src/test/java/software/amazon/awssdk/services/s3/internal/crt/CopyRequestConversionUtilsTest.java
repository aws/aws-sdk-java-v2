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

package software.amazon.awssdk.services.s3.internal.crt;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3ResponseMetadata;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.utils.Logger;

class CopyRequestConversionUtilsTest {
    private static final Logger log = Logger.loggerFor(CopyRequestConversionUtils.class);

    private static final Random RNG = new Random();

    @Test
    void toHeadObject_shouldCopyProperties() {
        CopyObjectRequest randomCopyObject = randomCopyObjectRequest();
        HeadObjectRequest convertedToHeadObject = CopyRequestConversionUtils.toHeadObjectRequest(randomCopyObject);
        Set<String> fieldsToIgnore = new HashSet<>(Arrays.asList("ExpectedBucketOwner",
                                                                 "RequestPayer",
                                                                 "Bucket",
                                                                 "Key",
                                                                 "SSECustomerKeyMD5",
                                                                 "SSECustomerKey",
                                                                 "SSECustomerAlgorithm"));
        verifyFieldsAreCopied(randomCopyObject, convertedToHeadObject, fieldsToIgnore,
                              CopyObjectRequest.builder().sdkFields(),
                              HeadObjectRequest.builder().sdkFields());
    }

    @Test
    void toCompletedPart_shouldCopyProperties() {
        CopyPartResult.Builder fromObject = CopyPartResult.builder();
        setFieldsToRandomValues(fromObject.sdkFields(), fromObject);
        CopyPartResult result = fromObject.build();

        CompletedPart convertedCompletedPart = CopyRequestConversionUtils.toCompletedPart(result, 1);
        verifyFieldsAreCopied(result, convertedCompletedPart, new HashSet<>(),
                              CopyPartResult.builder().sdkFields(),
                              CompletedPart.builder().sdkFields());
        assertThat(convertedCompletedPart.partNumber()).isEqualTo(1);
    }

    @Test
    void toCreateMultipartUploadRequest_shouldCopyProperties() {
        CopyObjectRequest randomCopyObject = randomCopyObjectRequest();
        CreateMultipartUploadRequest convertedRequest = CopyRequestConversionUtils.toCreateMultipartUploadRequest(randomCopyObject);
        Set<String> fieldsToIgnore = new HashSet<>();
        verifyFieldsAreCopied(randomCopyObject, convertedRequest, fieldsToIgnore,
                              CopyObjectRequest.builder().sdkFields(),
                              CreateMultipartUploadRequest.builder().sdkFields());
    }

    @Test
    void toCopyObjectResponse_shouldCopyProperties() {
        CompleteMultipartUploadResponse.Builder responseBuilder = CompleteMultipartUploadResponse.builder();
        setFieldsToRandomValues(responseBuilder.sdkFields(), responseBuilder);
        S3ResponseMetadata s3ResponseMetadata = S3ResponseMetadata.create(DefaultAwsResponseMetadata.create(new HashMap<>()));
        SdkHttpFullResponse sdkHttpFullResponse = SdkHttpFullResponse.builder().statusCode(200).build();
        responseBuilder.responseMetadata(s3ResponseMetadata).sdkHttpResponse(sdkHttpFullResponse);
        CompleteMultipartUploadResponse result = responseBuilder.build();

        CopyObjectResponse convertedRequest = CopyRequestConversionUtils.toCopyObjectResponse(result);
        Set<String> fieldsToIgnore = new HashSet<>();
        verifyFieldsAreCopied(result, convertedRequest, fieldsToIgnore,
                              CompleteMultipartUploadResponse.builder().sdkFields(),
                              CopyObjectResponse.builder().sdkFields());

        assertThat(convertedRequest.sdkHttpResponse()).isEqualTo(sdkHttpFullResponse);
        assertThat(convertedRequest.responseMetadata()).isEqualTo(s3ResponseMetadata);
    }

    @Test
    void toAbortMultipartUploadRequest_shouldCopyProperties() {
        CopyObjectRequest randomCopyObject = randomCopyObjectRequest();
        AbortMultipartUploadRequest convertedRequest = CopyRequestConversionUtils.toAbortMultipartUploadRequest(randomCopyObject,
                                                                                                                "id");
        Set<String> fieldsToIgnore = new HashSet<>();
        verifyFieldsAreCopied(randomCopyObject, convertedRequest, fieldsToIgnore,
                              CopyObjectRequest.builder().sdkFields(),
                              AbortMultipartUploadRequest.builder().sdkFields());

        assertThat(convertedRequest.uploadId()).isEqualTo("id");
    }

    @Test
    void toUploadPartCopyRequest_shouldCopyProperties() {
        CopyObjectRequest randomCopyObject = randomCopyObjectRequest();
        UploadPartCopyRequest convertedObject = CopyRequestConversionUtils.toUploadPartCopyRequest(randomCopyObject, 1, "id",
                                                                                                         "bytes=0-1024");
        Set<String> fieldsToIgnore = new HashSet<>(Collections.singletonList("CopySource"));
        verifyFieldsAreCopied(randomCopyObject, convertedObject, fieldsToIgnore,
                              CopyObjectRequest.builder().sdkFields(),
                              UploadPartCopyRequest.builder().sdkFields());
    }

    private static void verifyFieldsAreCopied(SdkPojo requestConvertedFrom,
                                              SdkPojo requestConvertedTo,
                                              Set<String> fieldsToIgnore,
                                              List<SdkField<?>> requestConvertedFromSdkFields,
                                              List<SdkField<?>> requestConvertedToSdkFields) {

        Map<String, SdkField<?>> toFields = sdkFieldMap(requestConvertedToSdkFields);
        Map<String, SdkField<?>> fromFields = sdkFieldMap(requestConvertedFromSdkFields);
        List<String> fieldsNotMatch = new ArrayList<>();

        for (Map.Entry<String, SdkField<?>> toObjectEntry : toFields.entrySet()) {
            SdkField<?> toField = toObjectEntry.getValue();

            if (fieldsToIgnore.contains(toField.memberName())) {
                log.info(() -> "Ignoring fields: " + toField.locationName());
                continue;
            }

            SdkField<?> fromField = fromFields.get(toObjectEntry.getKey());

            if (fromField == null) {
                log.info(() -> String.format("Ignoring field [%s] because the object to convert from does not have such field ",
                                             toField.locationName()));
                continue;
            }

            Object destinationObjValue = toField.getValueOrDefault(requestConvertedTo);
            Object sourceObjValue = fromField.getValueOrDefault(requestConvertedFrom);

            if (!sourceObjValue.equals(destinationObjValue)) {
                fieldsNotMatch.add(toField.memberName());
            }
        }
        assertThat(fieldsNotMatch).withFailMessage("Below fields do not match " + fieldsNotMatch).isEmpty();
    }

    private CopyObjectRequest randomCopyObjectRequest() {
        CopyObjectRequest.Builder builder = CopyObjectRequest.builder();
        setFieldsToRandomValues(builder.sdkFields(), builder);
        return builder.build();
    }

    private void setFieldsToRandomValues(Collection<SdkField<?>> fields, Object builder) {
        for (SdkField<?> f : fields) {
            setFieldToRandomValue(f, builder);
        }
    }

    private static void setFieldToRandomValue(SdkField<?> sdkField, Object obj) {
        Class<?> targetClass = sdkField.marshallingType().getTargetClass();
        if (targetClass.equals(String.class)) {
            sdkField.set(obj, RandomStringUtils.randomAlphanumeric(8));
        } else if (targetClass.equals(Integer.class)) {
            sdkField.set(obj, randomInteger());
        } else if (targetClass.equals(Instant.class)) {
            sdkField.set(obj, randomInstant());
        } else if (targetClass.equals(Map.class)) {
            sdkField.set(obj, new HashMap<>());
        } else if (targetClass.equals(Boolean.class)) {
            sdkField.set(obj, true);
        } else {
            throw new IllegalArgumentException("Unknown SdkField type: " + targetClass + " name: " + sdkField.memberName());
        }
    }

    private static Map<String, SdkField<?>> sdkFieldMap(Collection<? extends SdkField<?>> sdkFields) {
        Map<String, SdkField<?>> map = new HashMap<>(sdkFields.size());
        for (SdkField<?> f : sdkFields) {
            String locName = f.locationName();
            if (map.put(locName, f) != null) {
                throw new IllegalArgumentException("Multiple SdkFields map to same location name");
            }
        }
        return map;
    }

    private static Instant randomInstant() {
        return Instant.ofEpochMilli(RNG.nextLong());
    }

    private static Integer randomInteger() {
        return RNG.nextInt();
    }
}
