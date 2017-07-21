${fileHeader}
package ${metadata.fullSmokeTestsPackageName};

import javax.annotation.Generated;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import cucumber.api.guice.CucumberModules;
import cucumber.runtime.java.guice.InjectorSource;

import ${metadata.fullClientPackageName}.${metadata.syncClient};

/**
 * Injector that binds the AmazonWebServiceClient interface to the
 *  ${metadata.fullClientPackageName}.${metadata.syncClient}
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${metadata.syncInterface}ModuleInjector implements InjectorSource {

    @Override
    public Injector getInjector() {
        return Guice.createInjector(Stage.PRODUCTION, CucumberModules.SCENARIO, new  ${metadata.syncInterface}Module());
    }

    static class  ${metadata.syncInterface}Module extends AbstractModule {

        @Override
        protected void configure() {
            bind(AmazonWebServiceClient.class).to(${metadata.syncClient}.class);
        }
    }
}
