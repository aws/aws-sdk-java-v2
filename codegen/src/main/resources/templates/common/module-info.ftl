${fileHeader}

module ${metadata.rootPackageName}.${metadata.clientPackageName} {

    requires software.amazon.awssdk.awscore;
    requires java.xml.ws.annotation;
    requires slf4j.api;

    <#if hasValidPaginators>
    //Required if the service has paginators
    requires org.reactivestreams;
    exports ${metadata.fullClientPackageName}.paginators;
    </#if>
    <#if metadata.isJsonProtocol()>
    requires com.fasterxml.jackson.core;
    </#if>
    <#if metadata.isXmlProtocol()>
    requires java.xml;
    </#if>

    <#if customizationConfig.shareModelsWith??>
    requires ${metadata.rootPackageName}.${customizationConfig.shareModelsWith};
    </#if>
    exports ${metadata.fullClientPackageName};
    exports ${metadata.fullClientPackageName}.model;
    exports ${metadata.fullClientPackageName}.transform;
}

