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
package org.apache.sling.replication.agent.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.queue.impl.SingleQueueDistributionStrategy;
import org.apache.sling.replication.queue.impl.jobhandling.JobHandlingReplicationQueue;
import org.apache.sling.replication.queue.impl.jobhandling.JobHandlingReplicationQueueProvider;
import org.apache.sling.replication.rule.ReplicationRuleEngine;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilder;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProviderFactory;
import org.apache.sling.replication.transport.authentication.impl.UserCredentialsTransportAuthenticationProviderFactory;
import org.apache.sling.replication.transport.impl.HttpTransportHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An OSGi service factory for {@link ReplicationAgent}s
 */
@Component(metatype = true,
        label = "Replication Agents Factory",
        description = "OSGi configuration based ReplicationAgent service factory",
        name = ReplicationAgentServiceFactory.SERVICE_PID,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class ReplicationAgentServiceFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    static final String SERVICE_PID = "org.apache.sling.replication.agent.impl.ReplicationAgentServiceFactory";

    private static final String TRANSPORT = ReplicationAgentConfiguration.TRANSPORT;

    private static final String TRANSPORT_AUTHENTICATION_FACTORY = ReplicationAgentConfiguration.TRANSPORT_AUTHENTICATION_FACTORY;

    private static final String QUEUEPROVIDER = ReplicationAgentConfiguration.QUEUEPROVIDER;

    private static final String PACKAGING = ReplicationAgentConfiguration.PACKAGING;

    private static final String QUEUE_DISTRIBUTION = ReplicationAgentConfiguration.QUEUE_DISTRIBUTION;

    private static final String DEFAULT_TRANSPORT = "(name=" + HttpTransportHandler.NAME + ")";

    private static final String DEFAULT_AUTHENTICATION_FACTORY = "(name=" + UserCredentialsTransportAuthenticationProviderFactory.TYPE + ")";

    private static final String DEFAULT_PACKAGING = "(name="
            + FileVaultReplicationPackageBuilder.NAME + ")";

    private static final String DEFAULT_QUEUEPROVIDER = "(name="
            + JobHandlingReplicationQueueProvider.NAME + ")";

    private static final String DEFAULT_DISTRIBUTION = "(name="
            + SingleQueueDistributionStrategy.NAME + ")";

    @Property(boolValue = true)
    private static final String ENABLED = ReplicationAgentConfiguration.ENABLED;

    @Property
    private static final String NAME = ReplicationAgentConfiguration.NAME;

    @Property
    private static final String ENDPOINT = ReplicationAgentConfiguration.ENDPOINT;

    @Property
    private static final String AUTHENTICATION_PROPERTIES = ReplicationAgentConfiguration.AUTHENTICATION_PROPERTIES;

    @Property
    private static final String RULES = ReplicationAgentConfiguration.RULES;

    @Property(name = TRANSPORT, value = DEFAULT_TRANSPORT)
    @Reference(name = "TransportHandler", target = DEFAULT_TRANSPORT, policy = ReferencePolicy.DYNAMIC)
    private TransportHandler transportHandler;

    @Property(name = PACKAGING, value = DEFAULT_PACKAGING)
    @Reference(name = "ReplicationPackageBuilder", target = DEFAULT_PACKAGING, policy = ReferencePolicy.DYNAMIC)
    private ReplicationPackageBuilder packageBuilder;

    @Property(name = QUEUEPROVIDER, value = DEFAULT_QUEUEPROVIDER)
    @Reference(name = "ReplicationQueueProvider", target = DEFAULT_QUEUEPROVIDER, policy = ReferencePolicy.DYNAMIC)
    private ReplicationQueueProvider queueProvider;

    @Property(name = TRANSPORT_AUTHENTICATION_FACTORY, value = DEFAULT_AUTHENTICATION_FACTORY)
    @Reference(name = "TransportAuthenticationProviderFactory", target = DEFAULT_AUTHENTICATION_FACTORY, policy = ReferencePolicy.DYNAMIC)
    private TransportAuthenticationProviderFactory transportAuthenticationProviderFactory;

    @Property(name = QUEUE_DISTRIBUTION, value = DEFAULT_DISTRIBUTION)
    @Reference(name = "ReplicationQueueDistributionStrategy", target = DEFAULT_DISTRIBUTION, policy = ReferencePolicy.DYNAMIC)
    private ReplicationQueueDistributionStrategy queueDistributionStrategy;

    private ServiceRegistration agentReg;

    private ServiceRegistration jobReg;

    @Reference
    private ReplicationRuleEngine replicationRuleEngine;

    @Activate
    public void activate(BundleContext context, Map<String, ?> config) throws Exception {

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);
        if (enabled) {
            props.put(ENABLED, enabled);

            String name = PropertiesUtil
                    .toString(config.get(NAME), String.valueOf(new Random().nextInt(1000)));
            props.put(NAME, name);

            String endpoint = PropertiesUtil.toString(config.get(ENDPOINT), "");
            props.put(ENDPOINT, endpoint);

            String transport = PropertiesUtil.toString(config.get(TRANSPORT), "");
            props.put(TRANSPORT, transport);

            String packaging = PropertiesUtil.toString(config.get(PACKAGING), "");
            props.put(PACKAGING, packaging);

            String queue = PropertiesUtil.toString(config.get(QUEUEPROVIDER), "");
            props.put(QUEUEPROVIDER, queue);

            String distribution = PropertiesUtil.toString(config.get(QUEUE_DISTRIBUTION), "");
            props.put(QUEUE_DISTRIBUTION, distribution);

            Map<String, String> authenticationProperties = PropertiesUtil.toMap(config.get(AUTHENTICATION_PROPERTIES), new String[0]);
            props.put(AUTHENTICATION_PROPERTIES, authenticationProperties);

            String[] rules = PropertiesUtil.toStringArray(config.get(RULES), new String[0]);
            props.put(RULES, rules);

            String af = PropertiesUtil.toString(config.get(TRANSPORT_AUTHENTICATION_FACTORY), "");
            props.put(TRANSPORT_AUTHENTICATION_FACTORY, af);

            // check configuration is valid
            if (name == null || packageBuilder == null || queueProvider == null || queueDistributionStrategy == null) {
                throw new AgentConfigurationException("configuration for this agent is not valid");
            }

            TransportAuthenticationProvider transportAuthenticationProvider = null;
            if (transportAuthenticationProviderFactory != null) {
                transportAuthenticationProvider = transportAuthenticationProviderFactory.createAuthenticationProvider(authenticationProperties);
                if (transportHandler != null && !transportHandler.supportsAuthenticationProvider(transportAuthenticationProvider)) {
                    throw new Exception("authentication handler " + transportAuthenticationProvider
                            + " not supported by transport handler " + transportHandler);
                }
            }

            if (log.isInfoEnabled()) {
                log.info("bound services for {} :  {} - {} - {} - {} - {} - {}", new Object[]{name,
                        transportHandler, transportAuthenticationProvider, endpoint, packageBuilder, queueProvider, queueDistributionStrategy});
            }

            ReplicationAgent agent = new SimpleReplicationAgent(name, endpoint, rules, transportHandler, packageBuilder, queueProvider, transportAuthenticationProvider, queueDistributionStrategy);

            // register agent service
            agentReg = context.registerService(ReplicationAgent.class.getName(), agent, props);

            // apply rules if any
            if (rules.length > 0) {
                replicationRuleEngine.applyRules(agent, rules);
            }

            // eventually register job consumer for sling job handling based queues
            if (DEFAULT_QUEUEPROVIDER.equals(queue) && (transportHandler != null && endpoint != null && endpoint.length() > 0)) {
                Dictionary<String, Object> jobProps = new Hashtable<String, Object>();
                String topic = JobHandlingReplicationQueue.REPLICATION_QUEUE_TOPIC + '/' + name;
                String childTopic = topic + "/*";
                jobProps.put(JobConsumer.PROPERTY_TOPICS, new String[]{topic, childTopic});
                jobReg = context.registerService(JobConsumer.class.getName(), new ReplicationAgentJobConsumer(agent, packageBuilder), jobProps);
            }
        }
    }

    @Deactivate
    private void deactivate(BundleContext context) throws Exception {
        if (agentReg != null) {
            ServiceReference reference = agentReg.getReference();
            ReplicationAgent replicationAgent = (ReplicationAgent) context.getService(reference);

            String[] rules = replicationAgent.getRules();
            if (rules != null) {
                replicationRuleEngine.unapplyRules(replicationAgent, rules);
            }

            if (jobReg != null) {
                jobReg.unregister();
            }

            if (agentReg != null) {
                agentReg.unregister();
            }
        }
    }
}
