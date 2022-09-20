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

package software.amazon.awssdk.profiles.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Enum describing all the valid section names supported by SDK.
 * The section declares that the attributes that follow are part of a named collection of attributes.
 */
@SdkInternalApi
public enum ProfileSection {

    /**
     *  An `sso-session` section declares that the attributes that follow (until another section definition is encountered)
     *  are part of a named collection of attributes.
     *  This `sso-session` section is referenced by the user when configuring a profile to derive an SSO token.
     */
    SSO_SESSION("sso-session", "sso_session");

    private final String sectionTitle;
    private final String propertyKeyName;

    ProfileSection(String title, String propertyKeyName) {
        this.sectionTitle = title;
        this.propertyKeyName = propertyKeyName;
    }

    /**
     *
     * @param sectionTitle The section title in the config or credential file.
     * @return ProfileSection enum that has title name as sectionTitle
     */
    public static ProfileSection fromSectionTitle(String sectionTitle) {
        if (sectionTitle == null) {
            return null;
        }
        for (ProfileSection profileSection : values()) {
            if (profileSection.sectionTitle.equals(sectionTitle)) {
                return profileSection;
            }
        }
        throw new IllegalArgumentException("Unknown enum value for ProfileSection : " + sectionTitle);
    }

    /**
     *
     * @param propertyName The property definition of a key that points to a Section.
     * @return ProfileSection enum that corresponds to a propertyKeyName for the given propertyName.
     */
    public static ProfileSection fromPropertyKeyName(String propertyName) {
        if (propertyName == null) {
            return null;
        }

        for (ProfileSection section : values()) {
            if (section.getPropertyKeyName().equals(propertyName)) {
                return section;
            }
        }
        throw new IllegalArgumentException("Unknown enum value for ProfileSection : " + propertyName);
    }

    /**
     *
     * @return Gets the section title name for the given {@link ProfileSection}.
     */
    public String getSectionTitle() {
        return sectionTitle;
    }

    /**
     *
     * @return Gets the property Hey name for the given {@link ProfileSection}.
     */
    public String getPropertyKeyName() {
        return propertyKeyName;
    }
}