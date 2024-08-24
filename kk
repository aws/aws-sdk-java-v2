diff --git a/core/json-utils/src/main/java/software/amazon/awssdk/protocols/jsoncore/JsonNodeParser.java b/core/json-utils/src/main/java/software/amazon/awssdk/protocols/jsoncore/JsonNodeParser.java
index f87100eab1f..d85e1efbfad 100644
--- a/core/json-utils/src/main/java/software/amazon/awssdk/protocols/jsoncore/JsonNodeParser.java
+++ b/core/json-utils/src/main/java/software/amazon/awssdk/protocols/jsoncore/JsonNodeParser.java
@@ -25,12 +25,8 @@ import java.util.List;
 import java.util.Map;
 import software.amazon.awssdk.annotations.SdkProtectedApi;
 import software.amazon.awssdk.protocols.jsoncore.internal.ArrayJsonNode;
-import software.amazon.awssdk.protocols.jsoncore.internal.BooleanJsonNode;
 import software.amazon.awssdk.protocols.jsoncore.internal.EmbeddedObjectJsonNode;
-import software.amazon.awssdk.protocols.jsoncore.internal.NullJsonNode;
-import software.amazon.awssdk.protocols.jsoncore.internal.NumberJsonNode;
 import software.amazon.awssdk.protocols.jsoncore.internal.ObjectJsonNode;
-import software.amazon.awssdk.protocols.jsoncore.internal.StringJsonNode;
 import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
 import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;
 import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;
@@ -55,10 +51,12 @@ public final class JsonNodeParser {
 
     private final boolean removeErrorLocations;
     private final JsonFactory jsonFactory;
+    private final JsonValueNodeFactory jsonValueNodeFactory;
 
     private JsonNodeParser(Builder builder) {
         this.removeErrorLocations = builder.removeErrorLocations;
         this.jsonFactory = builder.jsonFactory;
+        this.jsonValueNodeFactory = builder.jsonValueNodeFactory;
     }
 
     /**
@@ -144,16 +142,12 @@ public final class JsonNodeParser {
         }
         switch (token) {
             case VALUE_STRING:
-                return new StringJsonNode(parser.getText());
             case VALUE_FALSE:
-                return new BooleanJsonNode(false);
             case VALUE_TRUE:
-                return new BooleanJsonNode(true);
             case VALUE_NULL:
-                return NullJsonNode.instance();
             case VALUE_NUMBER_FLOAT:
             case VALUE_NUMBER_INT:
-                return new NumberJsonNode(parser.getText());
+                return jsonValueNodeFactory.node(parser, token);
             case START_OBJECT:
                 return parseObject(parser);
             case START_ARRAY:
@@ -191,6 +185,7 @@ public final class JsonNodeParser {
      */
     public static final class Builder {
         private JsonFactory jsonFactory = DEFAULT_JSON_FACTORY;
+        private JsonValueNodeFactory jsonValueNodeFactory = JsonValueNodeFactory.DEFAULT;
         private boolean removeErrorLocations = false;
 
         private Builder() {
@@ -221,6 +216,17 @@ public final class JsonNodeParser {
             return this;
         }
 
+        /**
+         * Factory to create JsonNode out of JSON tokens. This allows JSON variants, such as CBOR, to produce actual values
+         * instead of having to parse them out of strings.
+         *
+         * <p>By default, this is {@link JsonValueNodeFactory#DEFAULT}.
+         */
+        public Builder jsonValueNodeFactory(JsonValueNodeFactory jsonValueNodeFactory) {
+            this.jsonValueNodeFactory = jsonValueNodeFactory;
+            return this;
+        }
+
         /**
          * Build a {@link JsonNodeParser} based on the current configuration of this builder.
          */
diff --git a/core/protocols/aws-json-protocol/src/main/java/software/amazon/awssdk/protocols/json/BaseAwsJsonProtocolFactory.java b/core/protocols/aws-json-protocol/src/main/java/software/amazon/awssdk/protocols/json/BaseAwsJsonProtocolFactory.java
index 693c70497dc..8164f05ea6c 100644
--- a/core/protocols/aws-json-protocol/src/main/java/software/amazon/awssdk/protocols/json/BaseAwsJsonProtocolFactory.java
+++ b/core/protocols/aws-json-protocol/src/main/java/software/amazon/awssdk/protocols/json/BaseAwsJsonProtocolFactory.java
@@ -50,6 +50,7 @@ import software.amazon.awssdk.protocols.json.internal.unmarshall.AwsJsonResponse
 import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
 import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonResponseHandler;
 import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
+import software.amazon.awssdk.protocols.jsoncore.JsonValueNodeFactory;
 
 @SdkProtectedApi
 public abstract class BaseAwsJsonProtocolFactory {
@@ -88,6 +89,7 @@ public abstract class BaseAwsJsonProtocolFactory {
             .builder()
             .parser(JsonNodeParser.builder()
                                   .jsonFactory(getSdkFactory().getJsonFactory())
+                                  .jsonValueNodeFactory(getJsonValueNodeFactory())
                                   .build())
             .defaultTimestampFormats(getDefaultTimestampFormats())
             .build();
@@ -134,15 +136,15 @@ public abstract class BaseAwsJsonProtocolFactory {
     public final HttpResponseHandler<AwsServiceException> createErrorResponseHandler(
         JsonOperationMetadata errorResponseMetadata) {
         return timeUnmarshalling(AwsJsonProtocolErrorUnmarshaller
-            .builder()
-            .jsonProtocolUnmarshaller(protocolUnmarshaller)
-            .exceptions(modeledExceptions)
-            .errorCodeParser(getSdkFactory().getErrorCodeParser(customErrorCodeFieldName))
-            .hasAwsQueryCompatible(hasAwsQueryCompatible)
-            .errorMessageParser(AwsJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER)
-            .jsonFactory(getSdkFactory().getJsonFactory())
-            .defaultExceptionSupplier(defaultServiceExceptionSupplier)
-            .build());
+                                     .builder()
+                                     .jsonProtocolUnmarshaller(protocolUnmarshaller)
+                                     .exceptions(modeledExceptions)
+                                     .errorCodeParser(getSdkFactory().getErrorCodeParser(customErrorCodeFieldName))
+                                     .hasAwsQueryCompatible(hasAwsQueryCompatible)
+                                     .errorMessageParser(AwsJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER)
+                                     .jsonFactory(getSdkFactory().getJsonFactory())
+                                     .defaultExceptionSupplier(defaultServiceExceptionSupplier)
+                                     .build());
     }
 
     private <T> MetricCollectingHttpResponseHandler<T> timeUnmarshalling(HttpResponseHandler<T> delegate) {
@@ -169,7 +171,7 @@ public abstract class BaseAwsJsonProtocolFactory {
     @SdkTestInternalApi
     public final String getContentType() {
         return protocolMetadata.contentType() != null ? protocolMetadata.contentType()
-                : getContentTypeResolver().resolveContentType(protocolMetadata);
+                                                      : getContentTypeResolver().resolveContentType(protocolMetadata);
     }
 
     /**
@@ -186,6 +188,10 @@ public abstract class BaseAwsJsonProtocolFactory {
         return AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY;
     }
 
+    protected JsonValueNodeFactory getJsonValueNodeFactory() {
+        return JsonValueNodeFactory.DEFAULT;
+    }
+
     /**
      * @return The default timestamp format for unmarshalling for each location in the response. This
      * can be overridden by subclasses to customize behavior.
