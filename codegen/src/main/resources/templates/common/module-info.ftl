${fileHeader}

module ${metadata.rootPackageName}.${metadata.clientPackageName} {

    requires software.amazon.awssdk.awscore;
    requires java.xml.ws.annotation;
    requires slf4j.api;

    <#if hasPaginators>
    //Required if the service has paginators
    requires org.reactivestreams;
    </#if>
    <#if metadata.isJsonProtocol()>
    requires com.fasterxml.jackson.core;
    </#if>
    <#if metadata.isXmlProtocol()>
    requires java.xml;
    </#if>
    exports ${metadata.fullClientPackageName};
}

