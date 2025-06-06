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

package com.arangodb.http;

import com.arangodb.PackageVersion;
import com.arangodb.arch.UnstableApi;
import com.arangodb.config.HostDescription;
import com.arangodb.internal.config.ArangoConfig;
import com.arangodb.internal.net.Connection;
import com.arangodb.internal.net.ConnectionFactory;
import com.arangodb.internal.net.ConnectionPool;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UnstableApi
public class HttpConnectionFactory implements ConnectionFactory {
    private final Logger LOGGER = LoggerFactory.getLogger(HttpConnectionFactory.class);

    final HttpProtocolConfig protocolConfig;

    public HttpConnectionFactory(@UnstableApi final HttpProtocolConfig cfg) {
        protocolConfig = cfg != null ? cfg : HttpProtocolConfig.builder().build();
        if (protocolConfig.getVertx() == null && !PackageVersion.SHADED && Vertx.currentContext() != null) {
            LOGGER.warn("Found an existing Vert.x instance, you can reuse it by setting:\n" +
                    "new ArangoDB.Builder()\n" +
                    "  // ...\n" +
                    "  .protocolConfig(HttpProtocolConfig.builder().vertx(Vertx.currentContext().owner()).build())\n" +
                    "  .build();\n");
        }
    }

    @Override
    @UnstableApi
    public Connection create(@UnstableApi final ArangoConfig config,
                             final HostDescription host,
                             @UnstableApi final ConnectionPool pool) {
        return new HttpConnection(config, protocolConfig, host, pool);
    }
}
