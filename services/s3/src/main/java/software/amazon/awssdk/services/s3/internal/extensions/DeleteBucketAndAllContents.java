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

package software.amazon.awssdk.services.s3.internal.extensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.extensions.S3ClientSdkExtension;
import software.amazon.awssdk.services.s3.model.DeleteMarkerEntry;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class DeleteBucketAndAllContents implements S3ClientSdkExtension {
    private static final Logger log = Logger.loggerFor(DeleteBucketAndAllContents.class);
    private static final int MAX_DELETE_OBJECTS_SIZE = 1_000;

    private final S3Client s3;

    public DeleteBucketAndAllContents(S3Client s3) {
        this.s3 = Validate.notNull(s3, "s3");
    }

    @Override
    public void deleteBucketAndAllContents(String bucket) {
        Validate.notEmpty(bucket, "bucket");
        log.debug(() -> "Emptying bucket: " + bucket);
        deleteAllObjects(bucket);
        deleteAllVersions(bucket);
        log.debug(() -> "Deleting bucket: " + bucket);
        s3.deleteBucket(r -> r.bucket(bucket));
    }

    private void deleteAllObjects(String bucket) {
        Stream<ObjectIdentifier> stream = s3.listObjectsV2Paginator(r -> r.bucket(bucket))
                                            .contents()
                                            .stream()
                                            .map(this::toDeletePojo);
        deleteObjectStream(bucket, stream);
    }

    private void deleteAllVersions(String bucket) {
        Stream<ObjectIdentifier> stream = s3.listObjectVersionsPaginator(r -> r.bucket(bucket))
                                            .stream()
                                            .map(this::toDeletePojos)
                                            .flatMap(Collection::stream);
        deleteObjectStream(bucket, stream);
    }

    private void deleteObjectStream(String bucket, Stream<ObjectIdentifier> stream) {
        Iterator<List<ObjectIdentifier>> batchIterator = batch(stream.iterator(), MAX_DELETE_OBJECTS_SIZE);
        batchIterator.forEachRemaining(objects -> {
            log.debug(() -> String.format("Deleting %s objects: %s", objects.size(), objects));
            DeleteObjectsResponse response = s3.deleteObjects(r -> r
                .bucket(bucket)
                .delete(d -> d
                    .objects(objects)
                    .quiet(true)));
            if (!response.errors().isEmpty()) {
                throw SdkClientException.create("Failed to delete objects: " + response);
            }
        });
    }

    private ObjectIdentifier toDeletePojo(S3Object object) {
        return ObjectIdentifier.builder()
                               .key(object.key())
                               .build();
    }

    private List<ObjectIdentifier> toDeletePojos(ListObjectVersionsResponse responsePage) {
        List<ObjectIdentifier> pojos = new ArrayList<>(responsePage.versions().size() + responsePage.deleteMarkers().size());
        responsePage.versions().stream().map(this::toDeletePojo).forEach(pojos::add);
        responsePage.deleteMarkers().stream().map(this::toDeletePojo).forEach(pojos::add);
        return pojos;
    }

    private ObjectIdentifier toDeletePojo(ObjectVersion object) {
        return ObjectIdentifier.builder()
                               .key(object.key())
                               .versionId(object.versionId())
                               .build();
    }

    private ObjectIdentifier toDeletePojo(DeleteMarkerEntry object) {
        return ObjectIdentifier.builder()
                               .key(object.key())
                               .versionId(object.versionId())
                               .build();
    }

    private static <T> Iterator<List<T>> batch(Iterator<T> iterator, int size) {
        Validate.notNull(iterator, "iterator");
        Validate.isPositive(size, "size");
        return new Iterator<List<T>>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public List<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                List<T> list = new ArrayList<>(size);
                for (int i = 0; i < size && iterator.hasNext(); i++) {
                    list.add(iterator.next());
                }
                return list;
            }
        };
    }
}
