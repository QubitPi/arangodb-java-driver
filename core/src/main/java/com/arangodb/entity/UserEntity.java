/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.entity;

import java.util.Map;
import java.util.Objects;

/**
 * @author Mark Vollmary
 */
public final class UserEntity {

    private String user;
    private Boolean active;
    private Map<String, Object> extra;
    private Boolean changePassword;

    /**
     * @return The name of the user as a string
     */
    public String getUser() {
        return user;
    }

    /**
     * @return An flag that specifies whether the user is active
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * @return An object with arbitrary extra data about the user
     */
    public Map<String, Object> getExtra() {
        return extra;
    }

    public Boolean getChangePassword() {
        return changePassword;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserEntity)) return false;
        UserEntity that = (UserEntity) o;
        return Objects.equals(user, that.user) && Objects.equals(active, that.active) && Objects.equals(extra, that.extra) && Objects.equals(changePassword, that.changePassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, active, extra, changePassword);
    }
}
