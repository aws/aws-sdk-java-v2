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

package software.amazon.awssdk.codegen.internal;

import static software.amazon.awssdk.codegen.model.service.ShapeType.List;
import static software.amazon.awssdk.codegen.model.service.ShapeType.Map;
import static software.amazon.awssdk.codegen.model.service.ShapeType.Structure;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.core.SdkBytes;

/**
 * Used to determine the Java types for the service model.
 */
public class TypeUtils {
    public static final class TypeKey {
        public static final String LIST_INTERFACE = "listInterface";

        public static final String LIST_DEFAULT_IMPL = "listDefaultImpl";

        public static final String MAP_INTERFACE = "mapInterface";

        public static final String MAP_DEFAULT_IMPL = "mapDefaultImpl";
    }

    private static final Map<String, String> DATA_TYPE_MAPPINGS = new HashMap<>();

    private static final Map<String, String> MARSHALLING_TYPE_MAPPINGS = new HashMap<>();

    static {
        DATA_TYPE_MAPPINGS.put("string", String.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("boolean", Boolean.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("int", Integer.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("any", Object.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("integer", Integer.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("double", Double.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("short", Short.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("long", Long.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("float", Float.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("byte", Byte.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("timestamp", Instant.class.getName());
        DATA_TYPE_MAPPINGS.put("blob", SdkBytes.class.getName());
        DATA_TYPE_MAPPINGS.put("stream", InputStream.class.getName());
        DATA_TYPE_MAPPINGS.put("bigdecimal", BigDecimal.class.getName());
        DATA_TYPE_MAPPINGS.put("biginteger", BigInteger.class.getName());
        DATA_TYPE_MAPPINGS.put("list", List.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put("map", Map.class.getSimpleName());
        DATA_TYPE_MAPPINGS.put(TypeKey.LIST_INTERFACE, List.class.getName());
        DATA_TYPE_MAPPINGS.put(TypeKey.LIST_DEFAULT_IMPL, ArrayList.class.getName());
        DATA_TYPE_MAPPINGS.put(TypeKey.MAP_INTERFACE, Map.class.getName());
        DATA_TYPE_MAPPINGS.put(TypeKey.MAP_DEFAULT_IMPL, HashMap.class.getName());

        MARSHALLING_TYPE_MAPPINGS.put("String", "STRING");
        MARSHALLING_TYPE_MAPPINGS.put("Integer", "INTEGER");
        MARSHALLING_TYPE_MAPPINGS.put("Long", "LONG");
        MARSHALLING_TYPE_MAPPINGS.put("Float", "FLOAT");
        MARSHALLING_TYPE_MAPPINGS.put("Double", "DOUBLE");
        MARSHALLING_TYPE_MAPPINGS.put("Instant", "INSTANT");
        MARSHALLING_TYPE_MAPPINGS.put("SdkBytes", "SDK_BYTES");
        MARSHALLING_TYPE_MAPPINGS.put("Boolean", "BOOLEAN");
        MARSHALLING_TYPE_MAPPINGS.put("BigDecimal", "BIG_DECIMAL");
        MARSHALLING_TYPE_MAPPINGS.put("InputStream", "STREAM");
        MARSHALLING_TYPE_MAPPINGS.put(null, "NULL");
    }

    private final NamingStrategy namingStrategy;

    public TypeUtils(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public static String getMarshallingType(String simpleType) {
        return MARSHALLING_TYPE_MAPPINGS.get(simpleType);
    }

    public static boolean isSimple(String type) {
        return DATA_TYPE_MAPPINGS.containsKey(type) || DATA_TYPE_MAPPINGS.containsValue(type);
    }

    public static String getDataTypeMapping(String type) {
        return DATA_TYPE_MAPPINGS.get(type);
    }

    /**
     * Returns the default Java type of the specified shape.
     */
    public String getJavaDataType(Map<String, Shape> shapes, String shapeName) {
        return getJavaDataType(shapes, shapeName, null);
    }

    /**
     * Returns the Java type of the specified shape with potential customization (such as
     * auto-construct list or map).
     */
    public String getJavaDataType(Map<String, Shape> shapes, String shapeName,
                                  CustomizationConfig customConfig) {

        if (shapeName == null || shapeName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot derive shape type. Shape name cannot be null or empty");
        }

        Shape shape = shapes.get(shapeName);

        if (shape == null) {
            throw new IllegalArgumentException(
                    "Cannot derive shape type. No shape information available for " + shapeName);
        }

        String shapeType = shape.getType();

        if (Structure.getName().equals(shapeType)) {
            return namingStrategy.getShapeClassName(shapeName);
        } else if (List.getName().equals(shapeType)) {
            String listContainerType = DATA_TYPE_MAPPINGS.get(TypeKey.LIST_INTERFACE);
            return listContainerType + "<" +
                    getJavaDataType(shapes, shape.getListMember().getShape()) + ">";
        } else if (Map.getName().equals(shapeType)) {
            String mapContainerType = DATA_TYPE_MAPPINGS.get(TypeKey.MAP_INTERFACE);
            return mapContainerType + "<" +
                    getJavaDataType(shapes, shape.getMapKeyType().getShape()) + "," +
                    getJavaDataType(shapes, shape.getMapValueType().getShape()) + ">";
        } else {

            if (shape.isStreaming()) {
                return DATA_TYPE_MAPPINGS.get("stream");
            }

            // scalar type.
            String dataType = DATA_TYPE_MAPPINGS.get(shapeType);
            if (dataType == null) {
                throw new RuntimeException(
                        "Equivalent Java data type cannot be found for data type : " + shapeType);
            }
            return dataType;
        }
    }
}
