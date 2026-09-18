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

package software.amazon.awssdk.services.stringarray.endpoints.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.stringarray.endpoints.StringArrayEndpointParams;
import software.amazon.awssdk.services.stringarray.jmespath.internal.JmesPathRuntime.Value;
import software.amazon.awssdk.services.stringarray.model.ListOfObjectsOperationRequest;
import software.amazon.awssdk.services.stringarray.model.ListOfUnionsOperationRequest;
import software.amazon.awssdk.services.stringarray.model.MapOperationRequest;
import software.amazon.awssdk.services.stringarray.model.NestedMapOperationRequest;
import software.amazon.awssdk.services.stringarray.model.ObjectMember;
import software.amazon.awssdk.services.stringarray.model.UnionMember;

/**
 * Verifies that the codegen-lowered {@code operationContextParams} bindings for the synthetic string array service
 * produce the same values as the reflective {@code JmesPathRuntime} evaluation, which remains the fallback for
 * unsupported expressions and is used here as the equivalence oracle. Covers a projection behind a nullable struct
 * ("nested.listOfObjects[*].key"), a multiselect-list + flatten ("listOfUnions[*][string, object.key][]"),
 * {@code keys()} over a map ("keys(map)"), and {@code keys()} behind a nullable struct ("keys(nestedMap.map)").
 *
 * <p>Each lowered binding is invoked directly via its generated, private
 * {@code setOperationContextParams(builder, request)} overload, so the check is isolated to the binding itself.
 */
public class OperationContextParamsBindingEquivalenceTest {

    private static List<String> lowered(Object request) {
        try {
            Method binding = StringArrayEndpointResolverUtils.class.getDeclaredMethod(
                "setOperationContextParams", StringArrayEndpointParams.Builder.class, request.getClass());
            binding.setAccessible(true);
            StringArrayEndpointParams.Builder builder = StringArrayEndpointParams.builder();
            binding.invoke(null, builder, request);
            return builder.build().stringArrayParam();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> reflectiveObjects(ListOfObjectsOperationRequest request) {
        return new Value(request).field("nested").field("listOfObjects").wildcard().field("key").stringValues();
    }

    private static List<String> reflectiveUnions(ListOfUnionsOperationRequest request) {
        Function<Value, Value> string = v -> v.field("string");
        Function<Value, Value> objectKey = v -> v.field("object").field("key");
        return new Value(request).field("listOfUnions").wildcard()
                                 .multiSelectList(string, objectKey).flatten().stringValues();
    }

    private static List<String> reflectiveMap(MapOperationRequest request) {
        return new Value(request).field("map").keys().stringValues();
    }

    private static void assertObjectsEquivalent(ListOfObjectsOperationRequest request, List<String> expected) {
        List<String> low = lowered(request);
        assertEquals(reflectiveObjects(request), low, "lowered binding must equal reflective evaluation");
        assertEquals(expected, low, "lowered binding must equal the hand-computed expectation");
    }

    private static void assertUnionsEquivalent(ListOfUnionsOperationRequest request, List<String> expected) {
        List<String> low = lowered(request);
        assertEquals(reflectiveUnions(request), low, "lowered binding must equal reflective evaluation");
        assertEquals(expected, low, "lowered binding must equal the hand-computed expectation");
    }

    private static ObjectMember object(String key) {
        return ObjectMember.builder().key(key).build();
    }

    @Test
    public void objectsNestedNull() {
        assertObjectsEquivalent(ListOfObjectsOperationRequest.builder().build(), Collections.emptyList());
    }

    /**
     * A null projection prefix yields {@code Collections.emptyList()} from {@code stringValues()}, so the lowered
     * binding must not substitute a mutable list.
     */
    @Test
    public void objectsNestedNullYieldsSameMutabilityAsReflective() {
        ListOfObjectsOperationRequest request = ListOfObjectsOperationRequest.builder().build();
        assertThrows(UnsupportedOperationException.class, () -> reflectiveObjects(request).add("x"),
                     "oracle assumption: reflective evaluation returns an immutable list for a null prefix");
        assertThrows(UnsupportedOperationException.class, () -> lowered(request).add("x"),
                     "lowered binding must match the reflective list's mutability for a null prefix");
    }

    @Test
    public void objectsMultipleKeysOrdered() {
        ListOfObjectsOperationRequest request = ListOfObjectsOperationRequest.builder()
            .nested(n -> n.listOfObjects(object("k1"), object("k2"), object("k3")))
            .build();
        assertObjectsEquivalent(request, Arrays.asList("k1", "k2", "k3"));
    }

    @Test
    public void objectsNullKeyAndNullElementDropped() {
        List<ObjectMember> objects = new ArrayList<>();
        objects.add(object("k1"));
        objects.add(null);
        objects.add(object(null));
        objects.add(object("k2"));
        ListOfObjectsOperationRequest request = ListOfObjectsOperationRequest.builder()
            .nested(n -> n.listOfObjects(objects))
            .build();
        assertObjectsEquivalent(request, Arrays.asList("k1", "k2"));
    }

    @Test
    public void unionsEmptyRequest() {
        assertUnionsEquivalent(ListOfUnionsOperationRequest.builder().build(), Collections.emptyList());
    }

    @Test
    public void unionsStringAndObjectBranchesOrdered() {
        // Within one union the multiselect order is [string, object.key].
        ListOfUnionsOperationRequest request = ListOfUnionsOperationRequest.builder()
            .listOfUnions(UnionMember.builder().string("s1").object(object("o1")).build(),
                          UnionMember.builder().object(object("o2")).build(),
                          UnionMember.builder().string("s3").build())
            .build();
        assertUnionsEquivalent(request, Arrays.asList("s1", "o1", "o2", "s3"));
    }

    @Test
    public void unionsEmptyMemberAndNullLeafDropped() {
        List<UnionMember> unions = new ArrayList<>();
        unions.add(UnionMember.builder().string("s1").build());
        unions.add(UnionMember.builder().build());
        unions.add(UnionMember.builder().object(object(null)).build());
        unions.add(null);
        ListOfUnionsOperationRequest request = ListOfUnionsOperationRequest.builder()
            .listOfUnions(unions)
            .build();
        assertUnionsEquivalent(request, Collections.singletonList("s1"));
    }

    @Test
    public void mapEmptyRequest() {
        MapOperationRequest request = MapOperationRequest.builder().build();
        List<String> low = lowered(request);
        assertEquals(reflectiveMap(request), low, "lowered binding must equal reflective evaluation");
        assertEquals(Collections.emptyList(), low);
    }

    @Test
    public void mapKeysMatchReflectiveHashOrdering() {
        // Keys whose insertion order differs from their HashMap iteration order: the reflective runtime wraps maps
        // as new HashMap<>(map), so the lowered binding must reproduce that hash ordering positionally.
        String[] keys = {"zebra", "mango", "apple", "delta", "foxtrot", "bravo", "yankee", "tango", "kilo", "echo"};
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : keys) {
            map.put(key, "value");
        }
        MapOperationRequest request = MapOperationRequest.builder().map(map).build();
        List<String> low = lowered(request);
        assertEquals(reflectiveMap(request), low, "lowered binding must equal reflective evaluation");
        assertEquals(new HashSet<>(Arrays.asList(keys)), new HashSet<>(low), "content must match");
    }

    private static List<String> reflectiveNestedMap(NestedMapOperationRequest request) {
        return new Value(request).field("nestedMap").field("map").keys().stringValues();
    }

    private static void assertNestedMapEquivalent(NestedMapOperationRequest request, List<String> expectedContent) {
        List<String> low = lowered(request);
        assertEquals(reflectiveNestedMap(request), low, "lowered binding must equal reflective evaluation");
        assertEquals(new HashSet<>(expectedContent), new HashSet<>(low), "content must match");
    }

    @Test
    public void nestedMapNullPrefix() {
        assertNestedMapEquivalent(NestedMapOperationRequest.builder().build(), Collections.emptyList());
    }

    /**
     * {@code keys()} normalizes a null prefix to an empty list value, whose {@code stringValues()} is mutable, so the
     * lowered binding must hand the endpoint params a mutable list here, unlike a null projection prefix.
     */
    @Test
    public void nestedMapNullPrefixYieldsSameMutabilityAsReflective() {
        NestedMapOperationRequest request = NestedMapOperationRequest.builder().build();
        assertDoesNotThrow(() -> reflectiveNestedMap(request).add("x"),
                           "oracle assumption: reflective keys() returns a mutable list for a null prefix");
        assertDoesNotThrow(() -> lowered(request).add("x"),
                           "lowered binding must match the reflective list's mutability for a null prefix");
    }

    @Test
    public void nestedMapKeys() {
        NestedMapOperationRequest request = NestedMapOperationRequest.builder()
            .nestedMap(n -> n.map(Collections.singletonMap("table", "value")))
            .build();
        assertNestedMapEquivalent(request, Collections.singletonList("table"));
    }

    /**
     * The equivalence assertions above are only meaningful if the generated methods are actually lowered: a codegen
     * regression that sends these operations back to the reflective path would make every comparison
     * oracle-against-itself. The resolver's class file must not reference the reflective runtime.
     */
    @Test
    public void operationContextParamBindingsDoNotUseReflectiveFallback() throws IOException {
        byte[] classBytes = readClassBytes(StringArrayEndpointResolverUtils.class);
        assertFalse(new String(classBytes, StandardCharsets.ISO_8859_1).contains("JmesPathRuntime"),
                    "generated resolver references JmesPathRuntime; bindings fell back to the reflective path");
    }

    private static byte[] readClassBytes(Class<?> clazz) throws IOException {
        try (InputStream in = clazz.getResourceAsStream(clazz.getSimpleName() + ".class");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
