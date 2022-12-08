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

package com.arangodb.async;

import com.arangodb.*;
import com.arangodb.entity.*;
import com.arangodb.internal.config.FileConfigPropertiesProvider;
import com.arangodb.internal.serde.SerdeUtils;
import com.arangodb.model.*;
import com.arangodb.util.RawJson;
import com.arangodb.util.TestUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Mark Vollmary
 * @author Michele Rastelli
 */
class ArangoDBTest {

    private static final String ROOT = "root";
    private static final String USER = "mit dem mund";
    private static final String PW = "machts der hund";
    private static Boolean extendedNames;

    private final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder()
            .loadProperties(new FileConfigPropertiesProvider())
            .build();
    private final ArangoDB arangoDBSync = new ArangoDB.Builder()
            .loadProperties(new FileConfigPropertiesProvider())
            .build();

    private boolean isEnterprise() {
        return arangoDBSync.getVersion().getLicense() == License.ENTERPRISE;
    }

    private boolean isCluster() {
        return arangoDBSync.getRole() == ServerRole.COORDINATOR;
    }

    private boolean isAtLeastVersion(final int major, final int minor) {
        return com.arangodb.util.TestUtils.isAtLeastVersion(arangoDBSync.getVersion().getVersion(), major, minor, 0);
    }

    private boolean isLessThanVersion(final int major, final int minor) {
        return com.arangodb.util.TestUtils.isLessThanVersion(arangoDBSync.getVersion().getVersion(), major, minor, 0);
    }

    private boolean supportsExtendedNames() {
        if (extendedNames == null) {
            try {
                ArangoDatabase testDb = arangoDBSync.db(DbName.of("test-" + TestUtils.generateRandomDbName(20, true)));
                testDb.create();
                extendedNames = true;
                testDb.drop();
            } catch (ArangoDBException e) {
                extendedNames = false;
            }
        }
        return extendedNames;
    }

    @Test
    void getVersion() throws InterruptedException, ExecutionException {
        arangoDB.getVersion()
                .whenComplete((version, ex) -> {
                    assertThat(version).isNotNull();
                    assertThat(version.getServer()).isNotNull();
                    assertThat(version.getVersion()).isNotNull();
                })
                .get();
    }

    @Test
    @Timeout(2)
    void nestedGetVersion() {
        for (int i = 0; i < 100; i++) {
            try {
                arangoDB.getVersion()
                        .whenComplete((v1, ex1) -> {
                            assertThat(v1).isNotNull();
                            assertThat(v1.getServer()).isNotNull();
                            assertThat(v1.getVersion()).isNotNull();
                            try {
                                arangoDB.getVersion()
                                        .whenComplete((v2, ex2) -> {
                                            assertThat(v2).isNotNull();
                                            assertThat(v2.getServer()).isNotNull();
                                            assertThat(v2.getVersion()).isNotNull();
                                            try {
                                                arangoDB.getVersion()
                                                        .whenComplete((v3, ex3) -> {
                                                            assertThat(v3).isNotNull();
                                                            assertThat(v3.getServer()).isNotNull();
                                                            assertThat(v3.getVersion()).isNotNull();
                                                        })
                                                        .get();
                                            } catch (InterruptedException | ExecutionException e) {
                                                e.printStackTrace();
                                                fail();
                                            }
                                        })
                                        .get();
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                                fail();
                            }
                        })
                        .get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    @Test
    void createDatabase() throws InterruptedException, ExecutionException {
        arangoDB.createDatabase(BaseTest.TEST_DB)
                .whenComplete((result, ex) -> assertThat(result).isEqualTo(true))
                .get();
        arangoDB.db(BaseTest.TEST_DB).drop().get();
    }

    @Test
    void createDatabaseWithOptions() throws ExecutionException, InterruptedException {
        assumeTrue(isCluster());
        assumeTrue(isAtLeastVersion(3, 6));

        final DbName dbName = DbName.of("testDB-" + TestUtils.generateRandomDbName(20, supportsExtendedNames()));
        final Boolean resultCreate = arangoDB.createDatabase(new DBCreateOptions()
                .name(dbName)
                .options(new DatabaseOptions()
                        .writeConcern(2)
                        .replicationFactor(2)
                        .sharding("")
                )
        ).get();
        assertThat(resultCreate).isTrue();

        DatabaseEntity info = arangoDB.db(dbName).getInfo().get();
        assertThat(info.getReplicationFactor().getValue()).isEqualTo(2);
        assertThat(info.getWriteConcern()).isEqualTo(2);
        assertThat(info.getSharding()).isEmpty();

        final Boolean resultDelete = arangoDB.db(dbName).drop().get();
        assertThat(resultDelete).isTrue();
    }

    @Test
    void createDatabaseWithOptionsSatellite() throws ExecutionException, InterruptedException {
        assumeTrue(isCluster());
        assumeTrue(isEnterprise());
        assumeTrue(isAtLeastVersion(3, 6));

        final DbName dbName = DbName.of("testDB-" + TestUtils.generateRandomDbName(20, supportsExtendedNames()));
        final Boolean resultCreate = arangoDB.createDatabase(new DBCreateOptions()
                .name(dbName)
                .options(new DatabaseOptions()
                        .writeConcern(2)
                        .replicationFactor(ReplicationFactor.ofSatellite())
                        .sharding("")
                )
        ).get();
        assertThat(resultCreate).isTrue();

        DatabaseEntity info = arangoDB.db(dbName).getInfo().get();
        assertThat(info.getReplicationFactor()).isEqualTo(ReplicationFactor.ofSatellite());
        assertThat(info.getWriteConcern()).isEqualTo(2);
        assertThat(info.getSharding()).isEmpty();

        final Boolean resultDelete = arangoDB.db(dbName).drop().get();
        assertThat(resultDelete).isTrue();
    }

    @Test
    void deleteDatabase() throws InterruptedException, ExecutionException {
        final Boolean resultCreate = arangoDB.createDatabase(BaseTest.TEST_DB).get();
        assertThat(resultCreate).isTrue();
        arangoDB.db(BaseTest.TEST_DB).drop()
                .whenComplete((resultDelete, ex) -> assertThat(resultDelete).isEqualTo(true))
                .get();
    }

    @Test
    void getDatabases() throws InterruptedException, ExecutionException {
        Collection<String> dbs = arangoDB.getDatabases().get();
        assertThat(dbs).isNotNull();
        assertThat(dbs).isNotEmpty();
        final int dbCount = dbs.size();
        assertThat(dbs).contains("_system");
        arangoDB.createDatabase(BaseTest.TEST_DB).get();
        dbs = arangoDB.getDatabases().get();
        assertThat(dbs).hasSizeGreaterThan(dbCount);
        assertThat(dbs).contains("_system");
        assertThat(dbs).contains(BaseTest.TEST_DB.get());
        arangoDB.db(BaseTest.TEST_DB).drop().get();
    }

    @Test
    void getAccessibleDatabases() throws InterruptedException, ExecutionException {
        arangoDB.getAccessibleDatabases()
                .whenComplete((dbs, ex) -> {
                    assertThat(dbs).isNotNull();
                    assertThat(dbs).isNotEmpty();
                    assertThat(dbs).contains("_system");
                })
                .get();
    }

    @Test
    void getAccessibleDatabasesFor() throws InterruptedException, ExecutionException {
        arangoDB.getAccessibleDatabasesFor("root")
                .whenComplete((dbs, ex) -> {
                    assertThat(dbs).isNotNull();
                    assertThat(dbs).isNotNull();
                    assertThat(dbs).isNotEmpty();
                    assertThat(dbs).contains("_system");
                })
                .get();
    }

    @Test
    void createUser() throws InterruptedException, ExecutionException {
        try {
            arangoDB.createUser(USER, PW, null)
                    .whenComplete((result, ex) -> {
                        assertThat(result).isNotNull();
                        assertThat(result.getUser()).isEqualTo(USER);
                    })
                    .get();
        } finally {
            arangoDB.deleteUser(USER).get();
        }
    }

    @Test
    void deleteUser() throws InterruptedException, ExecutionException {
        arangoDB.createUser(USER, PW, null).get();
        arangoDB.deleteUser(USER).get();
    }

    @Test
    void getUserRoot() throws InterruptedException, ExecutionException {
        arangoDB.getUser(ROOT)
                .whenComplete((user, ex) -> {
                    assertThat(user).isNotNull();
                    assertThat(user.getUser()).isEqualTo(ROOT);
                })
                .get();
    }

    @Test
    void getUser() throws InterruptedException, ExecutionException {
        try {
            arangoDB.createUser(USER, PW, null).get();
            arangoDB.getUser(USER)
                    .whenComplete((user, ex) -> assertThat(user.getUser()).isEqualTo(USER))
                    .get();
        } finally {
            arangoDB.deleteUser(USER).get();
        }

    }

    @Test
    void getUsersOnlyRoot() throws InterruptedException, ExecutionException {
        arangoDB.getUsers()
                .whenComplete((users, ex) -> {
                    assertThat(users).isNotNull();
                    assertThat(users).isNotEmpty();
                })
                .get();
    }

    @Test
    void getUsers() throws InterruptedException, ExecutionException {
        try {
            arangoDB.createUser(USER, PW, null).get();
            arangoDB.getUsers()
                    .whenComplete((users, ex) -> {
                        assertThat(users).isNotNull();
                        assertThat(users).hasSizeGreaterThanOrEqualTo(2);
                        assertThat(
                                users.stream().map(UserEntity::getUser).collect(Collectors.toList())
                        ).contains(ROOT, USER);
                    })
                    .get();
        } finally {
            arangoDB.deleteUser(USER).get();
        }
    }

    @Test
    void updateUserNoOptions() throws InterruptedException, ExecutionException {
        try {
            arangoDB.createUser(USER, PW, null).get();
            arangoDB.updateUser(USER, null).get();
        } finally {
            arangoDB.deleteUser(USER).get();
        }
    }

    @Test
    void updateUser() throws InterruptedException, ExecutionException {
        try {
            final Map<String, Object> extra = new HashMap<>();
            extra.put("hund", false);
            arangoDB.createUser(USER, PW, new UserCreateOptions().extra(extra)).get();
            extra.put("hund", true);
            extra.put("mund", true);
            {
                arangoDB.updateUser(USER, new UserUpdateOptions().extra(extra))
                        .whenComplete((user, ex) -> {
                            assertThat(user).isNotNull();
                            assertThat(user.getExtra()).hasSize(2);
                            assertThat(user.getExtra().get("hund")).isNotNull();
                            assertThat(Boolean.valueOf(String.valueOf(user.getExtra().get("hund")))).isTrue();
                        })
                        .get();
            }
            arangoDB.getUser(USER)
                    .whenComplete((user2, ex) -> {
                        assertThat(user2.getExtra()).hasSize(2);
                        assertThat(user2.getExtra().get("hund")).isNotNull();
                        assertThat(Boolean.valueOf(String.valueOf(user2.getExtra().get("hund")))).isTrue();
                    })
                    .get();
        } finally {
            arangoDB.deleteUser(USER).get();
        }
    }

    @Test
    void replaceUser() throws InterruptedException, ExecutionException {
        try {
            final Map<String, Object> extra = new HashMap<>();
            extra.put("hund", false);
            arangoDB.createUser(USER, PW, new UserCreateOptions().extra(extra)).get();
            extra.remove("hund");
            extra.put("mund", true);
            {
                arangoDB.replaceUser(USER, new UserUpdateOptions().extra(extra))
                        .whenComplete((user, ex) -> {
                            assertThat(user).isNotNull();
                            assertThat(user.getExtra()).hasSize(1);
                            assertThat(user.getExtra().get("mund")).isNotNull();
                            assertThat(Boolean.valueOf(String.valueOf(user.getExtra().get("mund")))).isTrue();
                        })
                        .get();
            }
            {
                arangoDB.getUser(USER)
                        .whenComplete((user2, ex) -> {
                            assertThat(user2.getExtra()).hasSize(1);
                            assertThat(user2.getExtra().get("mund")).isNotNull();
                            assertThat(Boolean.valueOf(String.valueOf(user2.getExtra().get("mund")))).isTrue();
                        })
                        .get();
            }
        } finally {
            arangoDB.deleteUser(USER).get();
        }
    }

    @Test
    void updateUserDefaultDatabaseAccess() throws InterruptedException, ExecutionException {
        try {
            arangoDB.createUser(USER, PW).get();
            arangoDB.grantDefaultDatabaseAccess(USER, Permissions.RW).get();
        } finally {
            arangoDB.deleteUser(USER).get();
        }
    }

    @Test
    void updateUserDefaultCollectionAccess() throws InterruptedException, ExecutionException {
        try {
            arangoDB.createUser(USER, PW).get();
            arangoDB.grantDefaultCollectionAccess(USER, Permissions.RW).get();
        } finally {
            arangoDB.deleteUser(USER).get();
        }
    }

    @Test
    void authenticationFailPassword() throws InterruptedException {
        final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder()
                .loadProperties(new FileConfigPropertiesProvider())
                .password("no").jwt(null).build();
        try {
            arangoDB.getVersion().get();
            fail();
        } catch (final ExecutionException exception) {
            assertThat(exception.getCause()).isInstanceOf(ArangoDBException.class);
        }
    }

    @Test
    void authenticationFailUser() throws InterruptedException {
        final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder()
                .loadProperties(new FileConfigPropertiesProvider())
                .user("no").jwt(null).build();
        try {
            arangoDB.getVersion().get();
            fail();
        } catch (final ExecutionException exception) {
            assertThat(exception.getCause()).isInstanceOf(ArangoDBException.class);
        }
    }

    @Test
    void execute() throws InterruptedException, ExecutionException {
        arangoDB
                .execute(Request.builder()
                        .db(DbName.SYSTEM)
                        .method(Request.Method.GET)
                        .path("/_api/version")
                        .build(), RawJson.class)
                .whenComplete((response, ex) -> {
                    assertThat(response.getBody()).isNotNull();
                    assertThat(SerdeUtils.INSTANCE.parseJson(response.getBody().getValue()).get("version").isTextual()).isTrue();
                })
                .get();
    }

    @Test
    void execute_acquireHostList_enabled() throws InterruptedException, ExecutionException {
        final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder()
                .loadProperties(new FileConfigPropertiesProvider())
                .acquireHostList(true).build();
        arangoDB
                .execute(Request.builder()
                        .db(DbName.SYSTEM)
                        .method(Request.Method.GET)
                        .path("/_api/version")
                        .build(), RawJson.class)
                .whenComplete((response, ex) -> {
                    assertThat(response.getBody()).isNotNull();
                    assertThat(SerdeUtils.INSTANCE.parseJson(response.getBody().getValue()).get("version").isTextual()).isTrue();
                })
                .get();
    }

    @Test
    void getLogEntries() throws InterruptedException, ExecutionException {
        assumeTrue(isAtLeastVersion(3, 8));
        arangoDB.getLogEntries(null)
                .whenComplete((logs, ex) -> {
                    assertThat(logs).isNotNull();
                    assertThat(logs.getTotal()).isPositive();
                    assertThat((long) logs.getMessages().size()).isEqualTo(logs.getTotal());
                })
                .get();
    }

    @Test
    void getLogLevel() throws InterruptedException, ExecutionException {
        assumeTrue(isAtLeastVersion(3, 7)); // it fails in 3.6 active-failover (BTS-362)
        arangoDB.getLogLevel()
                .whenComplete((logLevel, ex) -> {
                    assertThat(logLevel).isNotNull();
                    assertThat(logLevel.getAgency()).isEqualTo(LogLevelEntity.LogLevel.INFO);
                })
                .get();
    }

    @Test
    void setLogLevel() throws InterruptedException, ExecutionException {
        assumeTrue(isAtLeastVersion(3, 7)); // it fails in 3.6 active-failover (BTS-362)
        final LogLevelEntity entity = new LogLevelEntity();
        try {
            entity.setAgency(LogLevelEntity.LogLevel.ERROR);
            arangoDB.setLogLevel(entity)
                    .whenComplete((logLevel, ex) -> {
                        assertThat(logLevel).isNotNull();
                        assertThat(logLevel.getAgency()).isEqualTo(LogLevelEntity.LogLevel.ERROR);
                    })
                    .get();
        } finally {
            entity.setAgency(LogLevelEntity.LogLevel.INFO);
            arangoDB.setLogLevel(entity).get();
        }
    }

    @Test
    @Disabled("Manual execution only")
    void queueTime() throws InterruptedException, ExecutionException {
        List<CompletableFuture<ArangoCursorAsync<Void>>> reqs = IntStream.range(0, 80)
                .mapToObj(__ -> arangoDB.db().query("RETURN SLEEP(1)", Void.class))
                .collect(Collectors.toList());
        for (CompletableFuture<ArangoCursorAsync<Void>> req : reqs) {
            req.get();
        }

        QueueTimeMetrics qt = arangoDB.metrics().getQueueTime();
        double avg = qt.getAvg();
        QueueTimeSample[] values = qt.getValues();
        if (isAtLeastVersion(3, 9)) {
            assertThat(values).hasSize(20);
            for (int i = 0; i < values.length; i++) {
                assertThat(values[i]).isNotNull();
                assertThat(values[i].value).isGreaterThanOrEqualTo(0.0);
                if (i > 0) {
                    assertThat(values[i].timestamp).isGreaterThanOrEqualTo(values[i - 1].timestamp);
                }
            }
            assertThat(avg).isGreaterThan(0.0);
        } else {
            assertThat(avg).isEqualTo(0.0);
            assertThat(values).isEmpty();
        }
    }

    @Test
    void getQueryOptimizerRules() throws ExecutionException, InterruptedException {
        assumeTrue(isAtLeastVersion(3, 10));
        final Collection<QueryOptimizerRule> rules = arangoDB.getQueryOptimizerRules().get();
        assertThat(rules).isNotEmpty();
        for (QueryOptimizerRule rule : rules) {
            assertThat(rule).isNotNull();
            assertThat(rule.getName()).isNotNull();
            QueryOptimizerRule.Flags flags = rule.getFlags();
            assertThat(flags.getHidden()).isNotNull();
            assertThat(flags.getClusterOnly()).isNotNull();
            assertThat(flags.getCanBeDisabled()).isNotNull();
            assertThat(flags.getCanCreateAdditionalPlans()).isNotNull();
            assertThat(flags.getDisabledByDefault()).isNotNull();
            assertThat(flags.getEnterpriseOnly()).isNotNull();
        }
    }


}