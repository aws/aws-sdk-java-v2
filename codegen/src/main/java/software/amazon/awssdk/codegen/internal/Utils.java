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

package software.amazon.awssdk.codegen.internal;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Input;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.codegen.model.service.XmlNamespace;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;

public final class Utils {

    private Utils() {
    }

    public static boolean isScalar(Shape shape) {
        // enums are treated as scalars in C2j.
        return !(isListShape(shape) || isStructure(shape) || isMapShape(shape));
    }

    public static boolean isStructure(Shape shape) {
        return shape.getType().equals("structure");
    }

    public static boolean isListShape(Shape shape) {
        return shape.getType().equals("list");
    }

    public static boolean isMapShape(Shape shape) {
        return shape.getType().equals("map");
    }

    public static boolean isEnumShape(Shape shape) {
        return shape.getEnumValues() != null;
    }

    public static boolean isExceptionShape(Shape shape) {
        return shape.isException() || shape.isFault();
    }

    public static boolean isOrContainsEnumShape(Shape shape, Map<String, Shape> allShapes) {
        boolean isEnum = isEnumShape(shape);
        boolean isMapWithEnumMember = isMapShape(shape) && (isEnumShape(allShapes.get(shape.getMapKeyType().getShape())) ||
                                                            isEnumShape(allShapes.get(shape.getMapValueType().getShape())));
        boolean isListWithEnumMember = isListShape(shape) && isEnumShape(allShapes.get(shape.getListMember().getShape()));
        return isEnum || isMapWithEnumMember || isListWithEnumMember;
    }

    public static boolean isOrContainsEnum(MemberModel member) {
        boolean isEnum = member.getEnumType() != null;
        boolean isMapWithEnumMember = member.isMap() && (member.getMapModel().getKeyModel().getEnumType() != null ||
                                                         member.getMapModel().getValueModel().getEnumType() != null);
        boolean isListWithEnumMember = member.isList() && member.getListModel().getListMemberModel().getEnumType() != null;
        return isEnum || isMapWithEnumMember || isListWithEnumMember;
    }

    public static String getServiceName(ServiceMetadata metadata, CustomizationConfig customizationConfig) {
        String baseName = metadata.getServiceAbbreviation() == null ?
                metadata.getServiceFullName() :
                metadata.getServiceAbbreviation();

        baseName = baseName.replace("Amazon", "");
        baseName = baseName.replace("AWS", "");
        baseName = baseName.trim();
        baseName = baseName.replaceAll("[^A-Za-z0-9]", "");

        if (baseName.endsWith("Service")) {
            baseName = baseName.replace("Service", "");
        }

        return baseName;
    }

    public static String pascalCase(String baseName) {
        return Stream.of(baseName.split("\\s+")).map(StringUtils::lowerCase).map(Utils::capitialize).collect(joining());
    }

    public static String getClientPackageName(String serviceName, CustomizationConfig customizationConfig) {
        return getCustomizedPackageName(serviceName,
                                        Constants.PACKAGE_NAME_CLIENT_PATTERN);
    }

    public static String getModelPackageName(String serviceName, CustomizationConfig customizationConfig) {
        // Share transform package classes if we are sharing models.
        if (customizationConfig.getShareModelsWith() != null) {
            serviceName = customizationConfig.getShareModelsWith();
        }
        return getCustomizedPackageName(serviceName,
                                        Constants.PACKAGE_NAME_MODEL_PATTERN);
    }

    public static String getTransformPackageName(String serviceName, CustomizationConfig customizationConfig) {
        // Share transform package classes if we are sharing models.
        if (customizationConfig.getShareModelsWith() != null) {
            serviceName = customizationConfig.getShareModelsWith();
        }
        return getRequestTransformPackageName(serviceName, customizationConfig);
    }

    public static String getRequestTransformPackageName(String serviceName, CustomizationConfig customizationConfig) {
        return getCustomizedPackageName(serviceName,
                                        Constants.PACKAGE_NAME_TRANSFORM_PATTERN);
    }

    public static String getPaginatorsPackageName(String serviceName, CustomizationConfig customizationConfig) {
        return getCustomizedPackageName(serviceName, Constants.PACKAGE_NAME_PAGINATORS_PATTERN);
    }

    public static String getSmokeTestPackageName(String serviceName, CustomizationConfig customizationConfig) {
        return getCustomizedPackageName(serviceName,
                                        Constants.PACKAGE_NAME_SMOKE_TEST_PATTERN);
    }

    public static String getAuthPolicyPackageName(String serviceName, CustomizationConfig customizationConfig) {
        return getCustomizedPackageName(serviceName,
                                        Constants.PACKAGE_NAME_CUSTOM_AUTH_PATTERN);
    }

    private static String getCustomizedPackageName(String serviceName, String defaultPattern) {
        return String.format(defaultPattern, StringUtils.lowerCase(serviceName));
    }

    public static String unCapitialize(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        StringBuilder sb = new StringBuilder(name.length());

        int i = 0;
        do {
            sb.append(Character.toLowerCase(name.charAt(i++)));
        } while ((i < name.length() && Character.isUpperCase(name.charAt(i)))
                // not followed by a lowercase character
                && !(i < name.length() - 1 && Character.isLowerCase(name.charAt(i + 1))));

        sb.append(name.substring(i));

        return sb.toString();
    }

    public static String capitialize(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        return name.length() < 2 ? StringUtils.upperCase(name) : StringUtils.upperCase(name.substring(0, 1))
                + name.substring(1);
    }

    /**
     * * @param serviceModel Service model to get prefix for.
     * * @return Prefix to use when writing model files (service and intermediate).
     */
    public static String getFileNamePrefix(ServiceModel serviceModel) {
        return String.format("%s-%s", serviceModel.getMetadata().getEndpointPrefix(), serviceModel.getMetadata().getApiVersion());
    }

    /**
     * Converts a directory to a Java package name.
     *
     * @param directoryPath Directory to convert.
     * @return Package name
     */
    public static String directoryToPackage(String directoryPath) {
        return directoryPath.replace('/', '.');
    }

    /**
     * Converts a Java package name to a directory.
     *
     * @param packageName Java package to convert.
     * @return directory
     */
    public static String packageToDirectory(String packageName) {
        return packageName.replace('.', '/');
    }

    public static String getDefaultEndpointWithoutHttpProtocol(String endpoint) {

        if (endpoint == null) {
            return null;
        }
        if (endpoint.startsWith("http://")) {
            return endpoint.substring("http://".length());
        }
        if (endpoint.startsWith("https://")) {
            return endpoint.substring("https://".length());
        }
        return endpoint;
    }

    public static File createDirectory(String path) {
        if (isNullOrEmpty(path)) {
            throw new IllegalArgumentException(
                    "Invalid path directory. Path directory cannot be null or empty");
        }

        final File dir = new File(path);
        createDirectory(dir);
        return dir;
    }

    public static void createDirectory(File dir) {
        if (!(dir.exists())) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Not able to create directory. "
                        + dir.getAbsolutePath());
            }
        }
    }

    public static File createFile(String dir, String fileName) throws IOException {

        if (isNullOrEmpty(fileName)) {
            throw new IllegalArgumentException(
                    "Invalid file name. File name cannot be null or empty");
        }

        createDirectory(dir);

        File file = new File(dir, fileName);

        if (!(file.exists())) {
            if (!(file.createNewFile())) {
                throw new RuntimeException("Not able to create file . "
                        + file.getAbsolutePath());
            }
        }

        return file;
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static void closeQuietly(Closeable closeable) {
        IoUtils.closeQuietly(closeable, null);
    }

    /**
     * Return an InputStream of the specified resource, failing if it can't be found.
     *
     * @param location Location of resource
     */
    public static InputStream getRequiredResourceAsStream(Class<?> clzz, String location) {
        InputStream resourceStream = clzz.getResourceAsStream(location);

        if (resourceStream == null) {
            // Try with a leading "/"
            if (!location.startsWith("/")) {
                resourceStream = clzz.getResourceAsStream("/" + location);
            }
            if (resourceStream == null) {
                throw new RuntimeException("Resource file was not found at location " + location);
            }
        }

        return resourceStream;
    }

    /**
     * Search for intermediate shape model by its c2j name.
     *
     * @return ShapeModel
     * @throws IllegalArgumentException if the specified c2j name is not found in the intermediate model.
     */
    public static ShapeModel findShapeModelByC2jName(IntermediateModel intermediateModel, String shapeC2jName)
            throws IllegalArgumentException {
        ShapeModel shapeModel = findShapeModelByC2jNameIfExists(intermediateModel, shapeC2jName);
        if (shapeModel != null) {
            return shapeModel;
        } else {
            throw new IllegalArgumentException(
                    shapeC2jName + " shape (c2j name) does not exist in the intermediate model.");
        }
    }

    /**
     * Search for intermediate shape model by its c2j name.
     *
     * @return ShapeModel or null if the shape doesn't exist (if it's primitive or container type for example)
     */
    public static ShapeModel findShapeModelByC2jNameIfExists(IntermediateModel intermediateModel, String shapeC2jName) {
        for (ShapeModel shape : intermediateModel.getShapes().values()) {
            if (shape.getC2jName().equals(shapeC2jName)) {
                return shape;
            }
        }
        return null;
    }

    public static List<ShapeModel> findShapesByC2jName(IntermediateModel intermediateModel, String shapeC2jName) {
        return intermediateModel.getShapes().values().stream().filter(s -> s.getC2jName().equals(shapeC2jName)).collect(toList());
    }

    /**
     * Create the ShapeMarshaller to the input shape from the specified Operation.
     * The input shape in the operation could be empty.
     */
    public static ShapeMarshaller createInputShapeMarshaller(ServiceMetadata service, Operation operation) {

        if (operation == null) {
            throw new IllegalArgumentException(
                    "The operation parameter must be specified!");
        }

        ShapeMarshaller marshaller = new ShapeMarshaller()
                .withAction(operation.getName())
                .withVerb(operation.getHttp().getMethod())
                .withRequestUri(operation.getHttp().getRequestUri());
        Input input = operation.getInput();
        if (input != null) {
            marshaller.setLocationName(input.getLocationName());
            // Pass the xmlNamespace trait from the input reference
            XmlNamespace xmlNamespace = input.getXmlNamespace();
            if (xmlNamespace != null) {
                marshaller.setXmlNameSpaceUri(xmlNamespace.getUri());
            }
        }
        if (!StringUtils.isEmpty(service.getTargetPrefix()) && Metadata.isNotRestProtocol(service.getProtocol())) {
            marshaller.setTarget(service.getTargetPrefix() + "." + operation.getName());
        }
        return marshaller;

    }
}
