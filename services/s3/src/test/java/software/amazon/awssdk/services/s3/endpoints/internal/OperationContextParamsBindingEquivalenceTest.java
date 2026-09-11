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

package software.amazon.awssdk.services.s3.endpoints.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.jmespath.internal.JmesPathRuntime;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

/**
 * Verifies that the codegen-lowered {@code operationContextParams} binding for S3 DeleteObjects
 * ("Delete.Objects[*].Key") produces the same value as the reflective {@link JmesPathRuntime} evaluation,
 * which remains the fallback for unsupported expressions and is used here as the equivalence oracle.
 *
 * <p>The lowered binding is invoked directly via its generated, private
 * {@code setOperationContextParams(builder, request)} overload, so the check is isolated to the binding
 * itself, independent of the rest of endpoint parameter resolution.
 */
public class OperationContextParamsBindingEquivalenceTest {

    private static List<String> reflective(DeleteObjectsRequest request) {
        JmesPathRuntime.Value input = new JmesPathRuntime.Value(request);
        return input.field("Delete").field("Objects").wildcard().field("Key").stringValues();
    }

    private static List<String> lowered(DeleteObjectsRequest request) {
        try {
            Method binding = S3EndpointResolverUtils.class.getDeclaredMethod(
                "setOperationContextParams", S3EndpointParams.Builder.class, DeleteObjectsRequest.class);
            binding.setAccessible(true);
            S3EndpointParams.Builder builder = S3EndpointParams.builder();
            binding.invoke(null, builder, request);
            return builder.build().deleteObjectKeys();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertEquivalent(DeleteObjectsRequest request, List<String> expected) {
        List<String> low = lowered(request);
        assertEquals(reflective(request), low, "lowered binding must equal reflective evaluation");
        assertEquals(expected, low, "lowered binding must equal the hand-computed expectation");
    }

    private static ObjectIdentifier oid(String key) {
        return ObjectIdentifier.builder().key(key).build();
    }

    private static DeleteObjectsRequest req(ObjectIdentifier... objects) {
        return DeleteObjectsRequest.builder()
                                   .bucket("bucket")
                                   .delete(Delete.builder().objects(Arrays.asList(objects)).build())
                                   .build();
    }

    @Test
    public void deleteContainerNull() {
        assertEquivalent(DeleteObjectsRequest.builder().bucket("bucket").build(), Collections.emptyList());
    }

    /**
     * A null projection prefix yields {@code Collections.emptyList()} from {@code stringValues()}, so the lowered
     * binding must not substitute a mutable list: the endpoint params class stores and returns the reference as-is.
     */
    @Test
    public void deleteContainerNullYieldsSameMutabilityAsReflective() {
        DeleteObjectsRequest request = DeleteObjectsRequest.builder().bucket("bucket").build();
        assertThrows(UnsupportedOperationException.class, () -> reflective(request).add("x"),
                     "oracle assumption: reflective evaluation returns an immutable list for a null prefix");
        assertThrows(UnsupportedOperationException.class, () -> lowered(request).add("x"),
                     "lowered binding must match the reflective list's mutability for a null prefix");
    }

    @Test
    public void objectsEmpty() {
        assertEquivalent(DeleteObjectsRequest.builder()
                                             .bucket("bucket")
                                             .delete(Delete.builder().objects(Collections.emptyList()).build())
                                             .build(),
                         Collections.emptyList());
    }

    @Test
    public void singleKey() {
        assertEquivalent(req(oid("k1")), Collections.singletonList("k1"));
    }

    @Test
    public void multipleKeys() {
        assertEquivalent(req(oid("k1"), oid("k2"), oid("k3")), Arrays.asList("k1", "k2", "k3"));
    }

    @Test
    public void nullLeafKeyDropped() {
        assertEquivalent(req(oid("k1"), oid(null), oid("k3")), Arrays.asList("k1", "k3"));
    }

    @Test
    public void nullListElementDropped() {
        List<ObjectIdentifier> objects = new ArrayList<>();
        objects.add(oid("k1"));
        objects.add(null);
        objects.add(oid("k2"));
        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                                                           .bucket("bucket")
                                                           .delete(Delete.builder().objects(objects).build())
                                                           .build();
        assertEquivalent(request, Arrays.asList("k1", "k2"));
    }
}
