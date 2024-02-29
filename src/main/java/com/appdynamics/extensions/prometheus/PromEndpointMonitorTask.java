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

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.prometheus.utils.MetricConfigMap;
// import com.appdynamics.extensions.util.CryptoUtils;
// import com.appdynamics.extensions.util.SSLUtils;
// import com.appdynamics.extensions.util.StringUtils;
import com.google.common.base.Strings;

import prometheus.PrometheusScraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
// import javax.net.ssl.HostnameVerifier;
// import javax.net.ssl.SSLContext;
// import javax.net.ssl.SSLParameters;
// import javax.net.ssl.SSLSocketFactory;
import java.util.Map;

class PromEndpointMonitorTask implements AMonitorTaskRunnable {

    private static final Logger logger = LoggerFactory.getLogger(PromEndpointMonitorTask.class);
    private MonitorContextConfiguration contextConfiguration;
    private Map<String, ?> server;
    private Map<String, Double> previousValues;
    private MetricConfigMap metricConfigMap;
    private MetricWriteHelper metricWriteHelper;
    private long previousTimeStamp;
    private long currentTimeStamp;

    PromEndpointMonitorTask(MonitorContextConfiguration contextConfiguration,
            TasksExecutionServiceProvider serviceProvider, Map<String, ?> server, long previousTimeStamp,
            long currentTimeStamp, Map<String, Double> previousValues, MetricConfigMap metricConfigMap) {
        this.contextConfiguration = contextConfiguration;
        this.server = server;
        this.metricWriteHelper = serviceProvider.getMetricWriteHelper();
        this.previousTimeStamp = previousTimeStamp;
        this.currentTimeStamp = currentTimeStamp;
        this.previousValues = previousValues;
        this.metricConfigMap = metricConfigMap;
    }

    public void run() {
        logger.debug("starting monitor task");
        populateAndPrintMetrics();
    }

    private void populateAndPrintMetrics() {
        String name = (String) server.get("name");
        String host = (String) server.get("host");
        String port = (String) server.get("port");
        String path = (String) server.get("path");

        if (!Strings.isNullOrEmpty(host) && !Strings.isNullOrEmpty(name)) {
            Integer portNumber = 9090;
            if (!Strings.isNullOrEmpty(port)) {
                portNumber = Integer.parseInt(port);
            }
            if (Strings.isNullOrEmpty(path)) {
                path = "/metrics";
            }

            getMetricsFromEndpoint(host, portNumber, path);
        } else {
            logger.debug("The url and name fields of the server : {} need to be specified", server);
        }
    }

    private void getMetricsFromEndpoint(String host, Integer port, String path) {
        try {
            PrometheusScraper scraper = new PrometheusScraper(host, port, path);
            MachineAgentPrometheusMetricsWalker walker = new MachineAgentPrometheusMetricsWalker(contextConfiguration,
                    metricWriteHelper, previousTimeStamp, currentTimeStamp, previousValues, metricConfigMap);
            scraper.scrape(walker);
        } catch (MalformedURLException e) {
            logger.error("Malformed endpoint URL", e);
        } catch (IOException e) {
            logger.error("I/O exception while scraping endpoints", e);
        }
    }

    @Override
    public void onTaskComplete() {
        logger.debug("monitor task complete");
    }
}
