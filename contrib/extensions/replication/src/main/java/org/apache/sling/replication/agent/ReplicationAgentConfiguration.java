/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.replication.agent;

import java.util.Arrays;
import java.util.Dictionary;
import org.apache.sling.commons.osgi.PropertiesUtil;

/**
 * configuration for {@link ReplicationAgent}s
 */
public class ReplicationAgentConfiguration {

    public static final String TRANSPORT = "TransportHandler.target";

    public static final String TRANSPORT_AUTHENTICATION_FACTORY = "TransportAuthenticationProviderFactory.target";

    public static final String QUEUEPROVIDER = "ReplicationQueueProvider.target";

    public static final String PACKAGING = "ReplicationPackageBuilder.target";

    public static final String NAME = "name";

    public static final String ENDPOINT = "endpoint";

    public static final String AUTHENTICATION_PROPERTIES = "authentication.properties";

    public static final String QUEUE_DISTRIBUTION = "ReplicationQueueDistributionStrategy.target";

    public static final String RULES = "rules";

    public static final String ENABLED = "enabled";

    private final String name;

    private final String endpoint;

    private final String targetTransportHandler;

    private final String targetReplicationPackageBuilder;

    private final String targetReplicationQueueProvider;

    private final String targetReplicationQueueDistributionStrategy;

    private final String targetAuthenticationHandlerFactory;

    private final String[] authenticationProperties;

    private final String[] rules;

    public ReplicationAgentConfiguration(Dictionary<?, ?> dictionary) {
        this.name = PropertiesUtil.toString(dictionary.get(NAME), "");
        this.endpoint = PropertiesUtil.toString(dictionary.get(ENDPOINT), "");
        this.targetAuthenticationHandlerFactory = PropertiesUtil.toString(
                dictionary.get(TRANSPORT_AUTHENTICATION_FACTORY), "");
        this.targetReplicationPackageBuilder = PropertiesUtil.toString(dictionary.get(PACKAGING), "");
        this.targetReplicationQueueProvider = PropertiesUtil.toString(
                dictionary.get(QUEUEPROVIDER), "");
        this.targetReplicationQueueDistributionStrategy = PropertiesUtil.toString(dictionary.get(QUEUE_DISTRIBUTION), "");
        this.targetTransportHandler = PropertiesUtil.toString(dictionary.get(TRANSPORT), "");
        String[] ap = PropertiesUtil.toStringArray(dictionary.get(AUTHENTICATION_PROPERTIES));
        this.authenticationProperties = ap != null ? ap : new String[0];
        this.rules = PropertiesUtil.toStringArray(dictionary.get(RULES), new String[0]);
    }

    public String[] getAuthenticationProperties() {
        return authenticationProperties;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getName() {
        return name;
    }

    public String getTargetAuthenticationHandlerFactory() {
        return targetAuthenticationHandlerFactory;
    }

    public String getTargetReplicationPackageBuilder() {
        return targetReplicationPackageBuilder;
    }

    public String getTargetReplicationQueueProvider() {
        return targetReplicationQueueProvider;
    }

    public String getTargetTransportHandler() {
        return targetTransportHandler;
    }

    public String getTargetReplicationQueueDistributionStrategy() { return targetReplicationQueueDistributionStrategy; }

    @Override
    public String toString() {
        return "{\"" + NAME + "\":\"" + name + "\", \""
                + ENDPOINT + "\":\"" + endpoint + "\", \""
                + TRANSPORT + "\":\"" + targetTransportHandler + "\", \""
                + PACKAGING + "\":\"" + targetReplicationPackageBuilder + "\", \""
                + QUEUEPROVIDER + "\":\"" + targetReplicationQueueProvider + "\", \""
                + QUEUE_DISTRIBUTION + "\":\"" + targetReplicationQueueDistributionStrategy+ "\", \""
                + TRANSPORT_AUTHENTICATION_FACTORY + "\":\"" + targetAuthenticationHandlerFactory + "\", \""
                + AUTHENTICATION_PROPERTIES + "\":\"" + Arrays.toString(authenticationProperties) + "\", \""
                + RULES + "\":\"" + Arrays.toString(rules) + "\"}";
    }

}
