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

import static software.amazon.awssdk.codegen.internal.Constant.AWS_DOCS_HOST;
import static software.amazon.awssdk.codegen.model.intermediate.ShapeType.Model;
import static software.amazon.awssdk.codegen.model.intermediate.ShapeType.Request;
import static software.amazon.awssdk.codegen.model.intermediate.ShapeType.Response;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

public final class DocumentationUtils {

    private static final String DEFAULT_SETTER = "Sets the value of the %s property for this object.";

    private static final String DEFAULT_SETTER_PARAM = "The new value for the %s property for this object.";

    private static final String DEFAULT_GETTER = "Returns the value of the %s property for this object.";

    private static final String DEFAULT_GETTER_PARAM = "The value of the %s property for this object.";

    private static final String DEFAULT_EXISTENCE_CHECK = "Returns true if the %s property was specified by the sender "
                                                          + "(it may be empty), or false if the sender did not specify "
                                                          + "the value (it will be empty). For responses returned by the SDK, "
                                                          + "the sender is the AWS service.";

    private static final String DEFAULT_FLUENT_RETURN =
            "Returns a reference to this object so that method calls can be chained together.";

    //TODO probably should move this to a custom config in each service
    private static final Set<String> SERVICES_EXCLUDED_FROM_CROSS_LINKING = new HashSet<>(Arrays.asList(
            "apigateway", "budgets", "cloudsearch", "cloudsearchdomain",
            "discovery", "elastictranscoder", "es", "glacier",
            "iot", "data.iot", "machinelearning", "rekognition", "s3", "sdb", "swf"
                                                                                                       ));

    private DocumentationUtils() {
    }

    /**
     * Returns a documentation with HTML tags prefixed and suffixed removed, or
     * returns empty string if the input is empty or null. This method is to be
     * used when constructing documentation for method parameters.
     *
     * @param documentation
     *            unprocessed input documentation
     * @return HTML tag stripped documentation or empty string if input was
     *         null.
     */
    public static String stripHtmlTags(String documentation) {
        if (documentation == null) {
            return "";
        }

        if (documentation.startsWith("<")) {
            int startTagIndex = documentation.indexOf(">");
            int closingTagIndex = documentation.lastIndexOf("<");
            if (closingTagIndex > startTagIndex) {
                documentation = stripHtmlTags(documentation.substring(startTagIndex + 1, closingTagIndex));
            } else {
                documentation = stripHtmlTags(documentation.substring(startTagIndex + 1));
            }
        }

        return documentation.trim();
    }

    /**
     * Escapes Java comment breaking illegal character sequences.
     *
     * @param documentation
     *            unprocessed input documentation
     * @return escaped documentation, or empty string if input was null
     */
    public static String escapeIllegalCharacters(String documentation) {
        if (documentation == null) {
            return "";
        }

        /*
         * this specifically handles a case where a '* /' sequence may
         * be present in documentation and inadvertently terminate that Java
         * comment line, resulting in broken code.
         */
        documentation = documentation.replaceAll("\\*\\/", "*&#47;");

        return documentation;
    }

    /**
     * Create the HTML for a link to the operation/shape core AWS docs site
     *
     * @param metadata  the UID for the service from that services metadata
     * @param name the name of the shape/request/operation
     *
     * @return a '@see also' HTML link to the doc
     */
    public static String createLinkToServiceDocumentation(Metadata metadata, String name) {
        if (isCrossLinkingEnabledForService(metadata)) {
            return String.format("<a href=\"http://%s/goto/WebAPI/%s/%s\" target=\"_top\">AWS API Documentation</a>",
                                 AWS_DOCS_HOST,
                                 metadata.getUid(),
                                 name);
        }
        return "";
    }

    /**
     * Create the HTML for a link to the operation/shape core AWS docs site
     *
     * @param metadata  the UID for the service from that services metadata
     * @param shapeModel the model of the shape
     *
     * @return a '@see also' HTML link to the doc
     */
    public static String createLinkToServiceDocumentation(Metadata metadata, ShapeModel shapeModel) {
        return isRequestResponseOrModel(shapeModel) ? createLinkToServiceDocumentation(metadata,
                                                                                       shapeModel.getDocumentationShapeName())
                                                    : "";
    }

    public static String removeFromEnd(String string, String stringToRemove) {
        return string.endsWith(stringToRemove) ? string.substring(0, string.length() - stringToRemove.length()) : string;
    }

    private static boolean isRequestResponseOrModel(ShapeModel shapeModel) {
        return shapeModel.getShapeType() == Model || shapeModel.getShapeType() == Request ||
               shapeModel.getShapeType() == Response;
    }

    private static boolean isCrossLinkingEnabledForService(Metadata metadata) {
        return metadata.getUid() != null && metadata.getEndpointPrefix() != null &&
               !SERVICES_EXCLUDED_FROM_CROSS_LINKING.contains(metadata.getEndpointPrefix());
    }

    public static String defaultSetter() {
        return DEFAULT_SETTER;
    }

    public static String defaultSetterParam() {
        return DEFAULT_SETTER_PARAM;
    }

    public static String defaultGetter() {
        return DEFAULT_GETTER;
    }

    public static String defaultGetterParam() {
        return DEFAULT_GETTER_PARAM;
    }

    public static String defaultFluentReturn() {
        return DEFAULT_FLUENT_RETURN;
    }

    public static String defaultExistenceCheck() {
        return DEFAULT_EXISTENCE_CHECK;
    }
}
