/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.internal.connection;

import com.mongodb.ServerAddress;
import com.mongodb.connection.ServerDescription;
import com.mongodb.connection.ServerSettings;
import com.mongodb.connection.ServerType;
import com.mongodb.event.ServerListener;
import org.bson.types.ObjectId;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mongodb.connection.ServerConnectionState.CONNECTED;

public class TestClusterableServerFactory implements ClusterableServerFactory {
    private final Map<ServerAddress, TestServer> addressToServerMap = new HashMap<ServerAddress, TestServer>();

    @Override
    public ClusterableServer create(final ServerAddress serverAddress, final ServerListener serverListener,
                                    final ClusterClock clusterClock) {
        addressToServerMap.put(serverAddress, new TestServer(serverAddress, serverListener));
        return addressToServerMap.get(serverAddress);
    }

    @Override
    public ServerSettings getSettings() {
        return ServerSettings.builder().build();
    }

    public TestServer getServer(final ServerAddress serverAddress) {
        return addressToServerMap.get(serverAddress);
    }

    public void sendNotification(final ServerAddress serverAddress, final ServerDescription serverDescription) {
        getServer(serverAddress).sendNotification(serverDescription);
    }


    public void sendNotification(final ServerAddress serverAddress, final ServerType serverType, final List<ServerAddress> hosts) {
        sendNotification(serverAddress, serverType, hosts, "test");
    }

    public void sendNotification(final ServerAddress serverAddress, final ServerType serverType, final List<ServerAddress> hosts,
                                 final ServerAddress trueAddress) {
        sendNotification(serverAddress, serverType, hosts, "test", trueAddress);
    }

    public void sendNotification(final ServerAddress serverAddress, final ServerType serverType, final List<ServerAddress> hosts,
                                 final ObjectId electionId) {
        sendNotification(serverAddress, serverType, hosts, "test", electionId);
    }

    public void sendNotification(final ServerAddress serverAddress, final ServerType serverType, final List<ServerAddress> hosts,
                                 final List<ServerAddress> passives) {
        getServer(serverAddress).sendNotification(getBuilder(serverAddress,
                                                             serverType,
                                                             hosts,
                                                             passives,
                                                             true,
                                                             "test",
                                                             null,
                                                             null).build());
    }

    public void sendNotification(final ServerAddress serverAddress, final ServerType serverType, final List<ServerAddress> hosts,
                                 final String setName) {
        sendNotification(serverAddress, serverType, hosts, setName, (ObjectId) null);
    }

    public void sendNotification(final ServerAddress serverAddress, final ServerType serverType, final List<ServerAddress> hosts,
                                 final String setName, final ServerAddress trueAddress) {
        sendNotification(serverAddress, serverType, hosts, setName, null, trueAddress);
    }

    public void sendNotification(final ServerAddress serverAddress, final ServerType serverType, final List<ServerAddress> hosts,
                                 final String setName, final ObjectId electionId) {
        getServer(serverAddress).sendNotification(getBuilder(serverAddress, serverType, hosts, Collections.<ServerAddress>emptyList(),
                                                             true, setName, electionId, null)
                                                  .build());
    }

    public void sendNotification(final ServerAddress serverAddress, final ServerType serverType, final List<ServerAddress> hosts,
                                 final String setName, final ObjectId electionId, final ServerAddress trueAddress) {
        getServer(serverAddress).sendNotification(getBuilder(serverAddress, serverType, hosts, Collections.<ServerAddress>emptyList(),
                                                             true, setName, electionId, trueAddress)
                                                  .build());
    }

    public void sendNotification(final ServerAddress serverAddress, final ServerType serverType, final List<ServerAddress> hosts,
                                 final boolean ok) {
        getServer(serverAddress).sendNotification(getBuilder(serverAddress, serverType, hosts, Collections.<ServerAddress>emptyList(),
                                                             ok, null, null, null)
                                                  .build());
    }

    public ServerDescription getDescription(final ServerAddress server) {
        return getServer(server).getDescription();
    }

    public Set<ServerDescription> getDescriptions(final ServerAddress... servers) {
        Set<ServerDescription> serverDescriptions = new HashSet<ServerDescription>();
        for (ServerAddress cur : servers) {
            serverDescriptions.add(getServer(cur).getDescription());
        }
        return serverDescriptions;
    }

    private ServerDescription.Builder getBuilder(final ServerAddress serverAddress, final ServerType serverType,
                                                 final List<ServerAddress> hosts, final List<ServerAddress> passives, final boolean ok,
                                                 final String setName, final ObjectId electionId, final ServerAddress trueAddress) {
        Set<String> hostsSet = new HashSet<String>();
        for (ServerAddress cur : hosts) {
            hostsSet.add(cur.toString());
        }

        Set<String> passivesSet = new HashSet<String>();
        for (ServerAddress cur : passives) {
            passivesSet.add(cur.toString());
        }
        return ServerDescription.builder()
                                .address(serverAddress)
                                .type(serverType)
                                .ok(ok)
                                .state(CONNECTED)
                                .canonicalAddress(trueAddress == null ? serverAddress.toString() : trueAddress.toString())
                                .hosts(hostsSet)
                                .passives(passivesSet)
                                .setName(setName)
                                .electionId(electionId)
                                .maxWireVersion(1)
                                .setVersion(1);
    }
}
