#parse ( "global.vm")
package ${package};

import software.amazon.awssdk.services.${servicePackage}.${serviceClientClassName};


public class Handler {
    private final ${serviceClientClassName} ${serviceClientVariable}Client;

    public Handler() {
        ${serviceClientVariable}Client = DependencyFactory.${serviceClientVariable}Client();
    }

    public void sendRequest() {
        // TODO: invoking the api calls using ${serviceClientVariable}Client.
    }
}
