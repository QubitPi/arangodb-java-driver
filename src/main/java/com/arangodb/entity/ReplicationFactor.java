/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

import com.fasterxml.jackson.annotation.JsonValue;

public interface ReplicationFactor {

    static NumericReplicationFactor of(int value) {
        return new NumericReplicationFactor(value);
    }

    static SatelliteReplicationFactor ofSatellite() {
        return SatelliteReplicationFactor.INSTANCE;
    }

    @JsonValue
    Object getValue();

    final class NumericReplicationFactor implements ReplicationFactor {

        private final Integer value;

        public NumericReplicationFactor(Integer value) {
            this.value = value;
        }

        @Override
        public Integer getValue() {
            return value;
        }
    }

    enum SatelliteReplicationFactor implements ReplicationFactor {
        INSTANCE;

        @Override
        public String getValue() {
            return "satellite";
        }
    }
}
