package com.appdynamics.extensions.prometheus.utils;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.metrics.Metric;

public class MetricConfigMap {
    private static final Logger logger = LoggerFactory.getLogger(MetricConfigMap.class);
    private final Map<String, Metric> metricMap = new HashMap<>();
    private Map<String, ?> configMap;
    private String serverName;
    private String metricPrefix;

    public MetricConfigMap(Map<String, ?> configMap, String serverName, String metricPrefix) {
        this.configMap = configMap;
        this.serverName = serverName;
        this.metricPrefix = metricPrefix;
        buildMetricMap();
    }

    public Map<String, ?> getConfigMap() {
        return configMap;
    }

    public String getServerName() {
        return serverName;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public Map<String, Metric> getMetricMap() {
        return metricMap;
    }

    public Metric getMetric(String familyName) {
        return (Metric) metricMap.get(familyName);
    }

    public boolean hasFamily(String familyName) {
        return metricMap.containsKey(familyName);
    }

    protected void buildMetricMap() {
        String prefix = this.metricPrefix + Constants.METRIC_SEPARATOR + this.serverName;
        walkMetricConf(this.configMap, prefix);
    }

    @SuppressWarnings("unchecked")
    private void walkMetricConf(Map<String, ?> node, String nodePrefix) {
        logger.debug("walking metric config: '{}'", nodePrefix);
        for (Map.Entry<String, ?> entry : node.entrySet()) {
            String newPrefix = nodePrefix + Constants.METRIC_SEPARATOR + entry.getKey();
            if (entry.getValue() instanceof Map) {
                Map<String, ?> childNodes = (Map<String, ?>) entry.getValue();
                walkMetricConf(childNodes, newPrefix);
            } else if (entry.getValue() instanceof List) {
                List<Map<String, Map<String, ?>>> entries = (List<Map<String, Map<String, ?>>>) entry
                        .getValue();
                addFamilies(entries, newPrefix);
            } else {
                throw new InvalidParameterException("expected a list or a map here");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addFamilies(List<Map<String, Map<String, ?>>> entryList, String nodePrefix) {
        for (Object entry : entryList) {
            if (entry instanceof String) {
                String familyName = (String) entry;
                String fullPath = nodePrefix + Constants.METRIC_SEPARATOR + familyName;
                Metric newMetric = new Metric(familyName, "-1", fullPath);
                this.metricMap.put(familyName, newMetric);
            } else if (entry instanceof Map) {
                Map<String, Map<String, ?>> entryMap = (Map<String, Map<String, ?>>) entry;
                if (entryMap.size() != 1) {
                    throw new InvalidParameterException("too many map entries");
                }
                String familyName = entryMap.keySet().iterator().next();
                String fullPath = nodePrefix + Constants.METRIC_SEPARATOR + familyName;
                Map<String, ?> metricProps = entryMap.values().iterator().next();
                Metric newMetric = new Metric(familyName, "-1", fullPath, metricProps);
                this.metricMap.put(familyName, newMetric);
            }
        }
    }

}
