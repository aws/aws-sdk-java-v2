/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.Test;
import software.amazon.awssdk.core.auth.AwsSessionCredentials;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.util.StringInputStream;

/**
 * Validate the functionality of {@link ProfileFile}.
 */
public class ProfileFileTest {
    @Test
    public void emptyFilesHaveNoProfiles() {
        assertThat(configFileProfiles("")).isEmpty();
    }

    @Test
    public void emptyProfilesHaveNoProperties() {
        assertThat(configFileProfiles("[profile foo]"))
                .isEqualTo(profiles(profile("foo")));
    }

    @Test
    public void profileDefinitionsMustEndWithBrackets() {
        assertThatThrownBy(() -> configFileProfiles("[profile foo"))
                .hasMessageContaining("Profile definition must end with ']'");
    }

    @Test
    public void profileNamesShouldBeTrimmed() {
        assertThat(configFileProfiles("[profile \tfoo \t]"))
                .isEqualTo(profiles(profile("foo")));
    }

    @Test
    public void tabsCanSeparateProfileNamesFromProfilePrefix() {
        assertThat(configFileProfiles("[profile\tfoo]"))
                .isEqualTo(profiles(profile("foo")));
    }

    @Test
    public void propertiesMustBeDefinedInAProfile() {
        assertThatThrownBy(() -> configFileProfiles("name = value"))
                .hasMessageContaining("Expected a profile definition");
    }

    @Test
    public void profilesCanContainProperties() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name = value"))
                .isEqualTo(profiles(profile("foo", property("name", "value"))));
    }

    @Test
    public void windowsStyleLineEndingsAreSupported() {
        assertThat(configFileProfiles("[profile foo]\r\n" +
                                      "name = value"))
                .isEqualTo(profiles(profile("foo", property("name", "value"))));
    }

    @Test
    public void equalsSignsAreSupportedInPropertyNames() {
        assertThat(configFileProfiles("[profile foo]\r\n" +
                                      "name = val=ue"))
                .isEqualTo(profiles(profile("foo", property("name", "val=ue"))));
    }

    @Test
    public void unicodeCharactersAreSupportedInPropertyValues() {
        assertThat(configFileProfiles("[profile foo]\r\n" +
                                      "name = \uD83D\uDE02"))
                .isEqualTo(profiles(profile("foo", property("name", "\uD83D\uDE02"))));
    }

    @Test
    public void profilesCanContainMultipleProperties() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name = value\n" +
                                      "name2 = value2"))
                .isEqualTo(profiles(profile("foo",
                                            property("name", "value"),
                                            property("name2", "value2"))));
    }

    @Test
    public void propertyKeysAndValuesAreTrimmed() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name \t=  \tvalue \t"))
                .isEqualTo(profiles(profile("foo", property("name", "value"))));
    }

    @Test
    public void propertyValuesCanBeEmpty() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name ="))
                .isEqualTo(profiles(profile("foo", property("name", ""))));
    }

    @Test
    public void propertyKeysCannotBeEmpty() {
        assertThatThrownBy(() -> configFileProfiles("[profile foo]\n" +
                                                    "= value"))
                .hasMessageContaining("Property did not have a name");
    }

    @Test
    public void propertyDefinitionsMustContainAnEqualsSign() {
        assertThatThrownBy(() -> configFileProfiles("[profile foo]\n" +
                                                    "key : value"))
                .hasMessageContaining("Expected an '=' sign defining a property");
    }

    @Test
    public void multipleProfilesCanBeEmpty() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "[profile bar]"))
                .isEqualTo(profiles(profile("foo"),
                                    profile("bar")));
    }

    @Test
    public void multipleProfilesCanHaveProperties() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name = value\n" +
                                      "[profile bar]\n" +
                                      "name2 = value2"))
                .isEqualTo(profiles(profile("foo", property("name", "value")),
                                    profile("bar", property("name2", "value2"))));
    }

    @Test
    public void blankLinesAreIgnored() {
        assertThat(configFileProfiles("\t \n" +
                                      "[profile foo]\n" +
                                      "\t\n" +
                                      " \n" +
                                      "name = value\n" +
                                      "\t \n" +
                                      "[profile bar]\n" +
                                      " \t"))
                .isEqualTo(profiles(profile("foo", property("name", "value")),
                                    profile("bar")));
    }

    @Test
    public void poundSignCommentsAreIgnored() {
        assertThat(configFileProfiles("# Comment\n" +
                                      "[profile foo] # Comment\n" +
                                      "name = value # Comment with # sign"))
                .isEqualTo(profiles(profile("foo", property("name", "value"))));
    }

    @Test
    public void semicolonCommentsAreIgnored() {
        assertThat(configFileProfiles("; Comment\n" +
                                      "[profile foo] ; Comment\n" +
                                      "name = value ; Comment with ; sign"))
                .isEqualTo(profiles(profile("foo", property("name", "value"))));
    }

    @Test
    public void commentTypesCanBeUsedTogether() {
        assertThat(configFileProfiles("# Comment\n" +
                                      "[profile foo] ; Comment\n" +
                                      "name = value # Comment with ; sign"))
                .isEqualTo(profiles(profile("foo", property("name", "value"))));
    }

    @Test
    public void commentsCanBeEmpty() {
        assertThat(configFileProfiles(";\n" +
                                      "[profile foo];\n" +
                                      "name = value ;\n"))
                .isEqualTo(profiles(profile("foo", property("name", "value"))));
    }

    @Test
    public void commentsCanBeAdjacentToProfileNames() {
        assertThat(configFileProfiles("[profile foo]; Adjacent semicolons\n" +
                                      "[profile bar]# Adjacent pound signs"))
                .isEqualTo(profiles(profile("foo"),
                                    profile("bar")));
    }

    @Test
    public void commentsAdjacentToValuesAreIncludedInTheValue() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name = value; Adjacent semicolons\n" +
                                      "name2 = value# Adjacent pound signs"))
                .isEqualTo(profiles(profile("foo",
                                            property("name", "value; Adjacent semicolons"),
                                            property("name2", "value# Adjacent pound signs"))));
    }

    @Test
    public void propertyValuesCanBeContinuedOnTheNextLine() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name = value\n" +
                                      " -continued"))
                .isEqualTo(profiles(profile("foo",
                                            property("name", "value\n-continued"))));
    }

    @Test
    public void propertyValuesCanBeContinuedAcrossMultipleLines() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name = value\n" +
                                      " -continued\n" +
                                      " -and-continued"))
                .isEqualTo(profiles(profile("foo",
                                            property("name", "value\n-continued\n-and-continued"))));
    }

    @Test
    public void continuationValuesIncludeSemicolonComments() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name = value\n" +
                                      " -continued ; Comment"))
                .isEqualTo(profiles(profile("foo",
                                            property("name", "value\n-continued ; Comment"))));
    }

    @Test
    public void continuationsCannotBeUsedOutsideOfAProfile() {
        assertThatThrownBy(() -> configFileProfiles(" -continued"))
                .hasMessageContaining("Expected a profile or property definition");
    }

    @Test
    public void continuationsCannotBeUsedOutsideOfAProperty() {
        assertThatThrownBy(() -> configFileProfiles("[profile foo]\n" +
                                                    " -continued"))
                .hasMessageContaining("Expected a profile or property definition");
    }

    @Test
    public void continuationsResetWithProfileDefinition() {
        assertThatThrownBy(() -> configFileProfiles("[profile foo]\n" +
                                                    "name = value\n" +
                                                    "[profile foo]\n" +
                                                    " -continued"))
                .hasMessageContaining("Expected a profile or property definition");
    }

    @Test
    public void duplicateProfilesInTheSameFileMergeProperties() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name = value\n" +
                                      "[profile foo]\n" +
                                      "name2 = value2"))
                .isEqualTo(profiles(profile("foo",
                                            property("name", "value"),
                                            property("name2", "value2"))));
    }

    @Test
    public void duplicatePropertiesInAProfileUseTheLastOneDefined() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name = value\n" +
                                      "name = value2"))
                .isEqualTo(profiles(profile("foo", property("name", "value2"))));
    }

    @Test
    public void duplicatePropertiesInDuplicateProfilesUseTheLastOneDefined() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "name = value\n" +
                                      "[profile foo]\n" +
                                      "name = value2"))
                .isEqualTo(profiles(profile("foo", property("name", "value2"))));
    }

    @Test
    public void defaultProfileWithProfilePrefixOverridesDefaultProfileWithoutPrefixWhenPrefixedIsFirst() {
        assertThat(configFileProfiles("[profile default]\n" +
                                      "name = value\n" +
                                      "[default]\n" +
                                      "name2 = value2"))
                .isEqualTo(profiles(profile("default", property("name", "value"))));
    }

    @Test
    public void defaultProfileWithProfilePrefixOverridesDefaultProfileWithoutPrefixWhenPrefixedIsLast() {
        assertThat(configFileProfiles("[default]\n" +
                                      "name2 = value2\n" +
                                      "[profile default]\n" +
                                      "name = value"))
                .isEqualTo(profiles(profile("default", property("name", "value"))));
    }

    @Test
    public void invalidProfilesNamesAreIgnored() {
        assertThat(aggregateFileProfiles("[profile in valid]\n" +
                                         "name = value\n",
                                         "[in valid 2]\n" +
                                         "name2 = value2"))
                .isEqualTo(profiles());
    }

    @Test
    public void invalidPropertyNamesAreIgnored() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "in valid = value"))
                .isEqualTo(profiles(profile("foo")));
    }

    @Test
    public void allValidProfileNameCharactersAreSupported() {
        assertThat(configFileProfiles("[profile ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_]"))
                .isEqualTo(profiles(profile("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_")));
    }

    @Test
    public void allValidPropertyNameCharactersAreSupported() {
        assertThat(configFileProfiles("[profile foo]\nABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_ = value"))
                .isEqualTo(profiles(profile("foo", property("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_", "value"))));
    }

    @Test
    public void propertiesCanHaveSubProperties() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "s3 =\n" +
                                      " name = value"))
                .isEqualTo(profiles(profile("foo", property("s3", "\nname = value"))));
    }

    @Test
    public void invalidSubPropertiesCauseAnError() {
        assertThatThrownBy(() -> configFileProfiles("[profile foo]\n" +
                                                    "s3 =\n" +
                                                    " invalid"))
                .hasMessageContaining("Expected an '=' sign defining a property");
    }

    @Test
    public void subPropertiesCanHaveEmptyValues() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "s3 =\n" +
                                      " name ="))
                .isEqualTo(profiles(profile("foo", property("s3", "\nname ="))));
    }

    @Test
    public void invalidSubPropertiesCannotHaveEmptyNames() {
        assertThatThrownBy(() -> configFileProfiles("[profile foo]\n" +
                                                    "s3 =\n" +
                                                    " = value"))
                .hasMessageContaining("Property did not have a name");
    }

    @Test
    public void subPropertiesCanHaveInvalidNames() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "s3 =\n" +
                                      " in valid = value"))
                .isEqualTo(profiles(profile("foo", property("s3", "\nin valid = value"))));
    }

    @Test
    public void subPropertiesCanHaveBlankLines() {
        assertThat(configFileProfiles("[profile foo]\n" +
                                      "s3 =\n" +
                                      " name = value\n" +
                                      "\t \n" +
                                      " name2 = value2"))
                .isEqualTo(profiles(profile("foo", property("s3", "\nname = value\nname2 = value2"))));
    }

    @Test
    public void profilesDuplicatedInMultipleFilesAreMerged() {
        assertThat(aggregateFileProfiles("[profile foo]\n" +
                                         "name = value\n",
                                         "[foo]\n" +
                                         "name2 = value2"))
                .isEqualTo(profiles(profile("foo",
                                            property("name", "value"),
                                            property("name2", "value2"))));
    }

    @Test
    public void defaultProfilesWithMixedPrefixesInConfigFileIgnoreOneWithoutPrefixWhenMerging() {
        assertThat(configFileProfiles("[profile default]\n" +
                                      "name = value\n" +
                                      "[default]\n" +
                                      "name2 = value2\n" +
                                      "[profile default]\n" +
                                      "name3 = value3"))
                .isEqualTo(profiles(profile("default",
                                            property("name", "value"),
                                            property("name3", "value3"))));
    }

    @Test
    public void duplicatePropertiesBetweenFilesUsesCredentialsProperty() {
        assertThat(aggregateFileProfiles("[profile foo]\n" +
                                         "name = value",
                                         "[foo]\n" +
                                         "name = value2"))
                .isEqualTo(profiles(profile("foo", property("name", "value2"))));
    }

    @Test
    public void configProfilesWithoutPrefixAreIgnored() {
        assertThat(configFileProfiles("[foo]\n" +
                                      "name = value"))
                .isEqualTo(profiles());
    }

    @Test
    public void credentialsProfilesWithPrefixAreIgnored() {
        assertThat(credentialFileProfiles("[profile foo]\n" +
                                          "name = value"))
                .isEqualTo(profiles());
    }

    @Test
    public void roleProfileCanInheritFromAnotherFile() {
        String sourceProperties =
                "aws_access_key_id=defaultAccessKey\n" +
                "aws_secret_access_key=defaultSecretAccessKey";

        String childProperties =
                "source_profile=source\n" +
                "role_arn=arn:aws:iam::123456789012:role/testRole";

        String configSource = "[profile source]\n" + sourceProperties;
        String credentialsSource = "[source]\n" + sourceProperties;
        String configChild = "[profile child]\n" + childProperties;
        String credentialsChild = "[child]\n" + childProperties;

        Consumer<Map<String, Profile>> profileValidator = p ->
            assertThatThrownBy(() -> p.get("child").credentialsProvider())
                    .hasMessageContaining("the 'sts' service module must be on the class path");

        assertThat(aggregateFileProfiles(configSource, credentialsChild)).satisfies(profileValidator);
        assertThat(aggregateFileProfiles(configChild, credentialsSource)).satisfies(profileValidator);
    }

    @Test
    public void roleProfileWithMissingSourceThrowsException() {
        assertThatThrownBy(() -> configFile("[profile test]\n" +
                                            "source_profile=source\n" +
                                            "role_arn=arn:aws:iam::123456789012:role/testRole").profile("test").get()
                                                                                               .credentialsProvider())
                .hasMessageContaining("source profile has no credentials configured.");
    }

    @Test
    public void roleProfileWithSourceThatHasNoCredentialsThrowsExceptionWhenLoadingCredentials() {
        ProfileFile profiles = configFile("[profile source]\n" +
                                          "[profile test]\n" +
                                          "source_profile=source\n" +
                                          "role_arn=arn:aws:iam::123456789012:role/testRole");

        assertThat(profiles.profile("test")).hasValueSatisfying(profile -> {
            assertThatThrownBy(profile::credentialsProvider).hasMessageContaining("source profile has no credentials configured");
        });
    }

    @Test
    public void profileFileWithRegionLoadsCorrectly() {
        assertThat(allTypesProfile().profile("profile-with-region")).hasValueSatisfying(profile -> {
            assertThat(profile.region()).hasValue(Region.of("us-east-1"));
        });
    }

    @Test
    public void profileFileWithStaticCredentialsLoadsCorrectly() {
        assertThat(allTypesProfile().profile("default")).hasValueSatisfying(profile -> {
            assertThat(profile.name()).isEqualTo("default");
            assertThat(profile.property(ProfileProperties.AWS_ACCESS_KEY_ID)).hasValue("defaultAccessKey");
            assertThat(profile.toString()).contains("default");
            assertThat(profile.region()).isNotPresent();
            assertThat(profile.credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider.getCredentials()).satisfies(credentials -> {
                    assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
                    assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
                });
            });
        });
    }

    @Test
    public void profileFileWithSessionCredentialsLoadsCorrectly() {
        assertThat(allTypesProfile().profile("profile-with-session-token")).hasValueSatisfying(profile -> {
            assertThat(profile.region()).isNotPresent();
            assertThat(profile.credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider.getCredentials()).satisfies(credentials -> {
                    assertThat(credentials).isInstanceOf(AwsSessionCredentials.class);
                    assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
                    assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
                    assertThat(((AwsSessionCredentials) credentials).sessionToken()).isEqualTo("awsSessionToken");
                });
            });
        });
    }

    @Test
    public void profileFileWithAssumeRoleThrowsExceptionWhenRetrievingCredentialsProvider() {
        assertThat(allTypesProfile().profile("profile-with-assume-role")).hasValueSatisfying(profile -> {
            assertThat(profile.region()).isNotPresent();
            assertThatThrownBy(profile::credentialsProvider).isInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    public void profileFileWithCircularDependencyThrowsExceptionWhenResolvingCredentials() {
        assertThatThrownBy(() -> configFile("[profile source]\n" +
                                            "aws_access_key_id=defaultAccessKey\n" +
                                            "aws_secret_access_key=defaultSecretAccessKey\n" +
                                            "\n" +
                                            "[profile test]\n" +
                                            "source_profile=test3\n" +
                                            "role_arn=arn:aws:iam::123456789012:role/testRole\n" +
                                            "\n" +
                                            "[profile test2]\n" +
                                            "source_profile=test\n" +
                                            "role_arn=arn:aws:iam::123456789012:role/testRole2\n" +
                                            "\n" +
                                            "[profile test3]\n" +
                                            "source_profile=test2\n" +
                                            "role_arn=arn:aws:iam::123456789012:role/testRole3").profile("test").get()
                                                                                                .credentialsProvider())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid profile file: Circular relationship detected with profiles");
    }

    @Test
    public void loadingDefaultProfileFileWorks() {
        ProfileFile.defaultProfileFile();
    }

    private ProfileFile allTypesProfile() {
        return configFile("[default]\n" +
                          "aws_access_key_id = defaultAccessKey\n" +
                          "aws_secret_access_key = defaultSecretAccessKey\n" +
                          "\n" +
                          "[profile profile-with-session-token]\n" +
                          "aws_access_key_id = defaultAccessKey\n" +
                          "aws_secret_access_key = defaultSecretAccessKey\n" +
                          "aws_session_token = awsSessionToken\n" +
                          "\n" +
                          "[profile profile-with-region]\n" +
                          "region = us-east-1\n" +
                          "\n" +
                          "[profile profile-with-assume-role]\n" +
                          "source_profile=default\n" +
                          "role_arn=arn:aws:iam::123456789012:role/testRole\n");
    }

    private ProfileFile configFile(String configFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(configFile))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

    private Map<String, Profile> configFileProfiles(String configFile) {
        return configFile(configFile).profiles();
    }

    private ProfileFile credentialFile(String credentialFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(credentialFile))
                          .type(ProfileFile.Type.CREDENTIALS)
                          .build();
    }

    private Map<String, Profile> credentialFileProfiles(String credentialFile) {
        return credentialFile(credentialFile).profiles();
    }

    private Map<String, Profile> aggregateFileProfiles(String configFile, String credentialFile) {
        return ProfileFile.aggregator()
                          .addFile(credentialFile(credentialFile))
                          .addFile(configFile(configFile))
                          .build()
                          .profiles();
    }

    private Map<String, Profile> profiles(Profile... profiles) {
        Map<String, Profile> result = new HashMap<>();
        Stream.of(profiles).forEach(p -> result.put(p.name(), p));
        return result;
    }

    @SafeVarargs
    private final Profile profile(String name, Map.Entry<String, String>... properties) {
        Map<String, String> propertiesMap = new HashMap<>();
        Stream.of(properties).forEach(p -> propertiesMap.put(p.getKey(), p.getValue()));
        return Profile.builder().name(name).properties(propertiesMap).build();
    }

    private Map.Entry<String, String> property(String name, String value) {
        return new AbstractMap.SimpleImmutableEntry<>(name, value);
    }
}
