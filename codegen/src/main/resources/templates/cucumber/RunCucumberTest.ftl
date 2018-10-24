${fileHeader}
package ${metadata.smokeTestsPackageName};

import software.amazon.awssdk.annotations.Generated;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@CucumberOptions(glue = {"software.amazon.awssdk.testutils.smoketest"})
@RunWith(Cucumber.class)
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class RunCucumberTest {

}
