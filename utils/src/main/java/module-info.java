/**
 * Class javadoc
 */
module software.amazon.awssdk.utils {
    requires software.amazon.awssdk.annotation;
    requires slf4j.api;
    requires java.xml;
    exports software.amazon.awssdk.utils.builder;
    exports software.amazon.awssdk.utils;
    exports software.amazon.awssdk.utils.http;
    exports software.amazon.awssdk.utils.cache;
}