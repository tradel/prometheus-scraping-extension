/**
 * Copyright 2017 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.prometheus;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.metrics.MetricProperties;
import com.appdynamics.extensions.metrics.MetricPropertiesBuilder;
import com.appdynamics.extensions.prometheus.utils.Constants;
import com.appdynamics.extensions.prometheus.utils.MetricConfigMap;
import com.appdynamics.extensions.util.AssertUtils;

import org.eclipse.aether.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unix4j.util.Assert;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.appdynamics.extensions.prometheus.utils.Constants.DEFAULT_METRIC_PREFIX;

public class PromEndpointMonitor extends ABaseMonitor {

    private static final Logger logger = LoggerFactory.getLogger(PromEndpointMonitor.class);
    private static long previousTimeStamp = System.currentTimeMillis();
    private static long currentTimeStamp = System.currentTimeMillis();
    private Map<String, Map<String, Double>> previousValues = new HashMap<>();
    private Map<String, ?> metricConfig;
    private MetricConfigMap metricConfigMap;

    @Override
    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "Prometheus Endpoint Monitor";
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {
        logger.info("starting collection run");
        previousTimeStamp = currentTimeStamp;
        currentTimeStamp = System.currentTimeMillis();
        metricConfig = getMetricConfig();

        List<Map<String, ?>> servers = getServers();
        for (Map<String, ?> server : servers) {
            String serverName = (String) server.get("name");
            metricConfigMap = buildMetricConfigMap(serverName);
            if (!previousValues.containsKey(serverName)) {
                previousValues.put(serverName, new HashMap<>());
            }
            Map<String, Double> serverPreviousValues = previousValues.get(server.get("name"));
            PromEndpointMonitorTask task = new PromEndpointMonitorTask(getContextConfiguration(), serviceProvider,
                    server, previousTimeStamp, currentTimeStamp, serverPreviousValues, metricConfigMap);
            serviceProvider.submit((String) server.get("name"), task);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, ?> getMetricConfig() {
        Map<String, ?> metricsMap = (Map<String, ?>) this.getContextConfiguration().getConfigYml().get("metrics");
        AssertUtils.assertNotNull(metricsMap, "The 'metrics' section in config.yml is not initialised");
        return metricsMap;
    }

    protected MetricConfigMap buildMetricConfigMap(String serverName) {
        return new MetricConfigMap(metricConfig, serverName, getContextConfiguration().getMetricPrefix());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Map<String, ?>> getServers() {
        List<Map<String, ?>> servers = (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get("servers");
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        return servers;
    }

}
