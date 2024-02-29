package com.appdynamics.extensions.prometheus.utils;

import java.util.Arrays;
import java.util.Map;

import com.appdynamics.extensions.metrics.DefaultMetricProperties;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.metrics.MetricProperties;
import com.appdynamics.extensions.util.MetricPathUtils;

public class MetricBuilder {
    protected Metric metric;

    public MetricBuilder(String metricName, String metricValue, String metricPath,
            String aggregationType, String timeRollUpType, String clusteRollUpType) {
        this.metric = new Metric(metricName, metricValue, metricPath,
                aggregationType, timeRollUpType, clusteRollUpType);
    }

    public MetricBuilder(Metric metric) {
        this(metric.getMetricName(), metric.getMetricValue(), metric.getMetricPath(),
                metric.getAggregationType(), metric.getTimeRollUpType(), metric.getClusterRollUpType());
    }

    public Metric build() {
        return metric;
    }

    public MetricBuilder setAlias(String alias) {
        metric.getMetricProperties().setAlias(alias, metric.getMetricName());
        return this;
    }

    public MetricBuilder setValue(Double value) {
        metric.setMetricValue(Double.toString(value));
        return this;
    }

    public MetricBuilder setValue(Long value) {
        metric.setMetricValue(Long.toString(value));
        return this;
    }

    public MetricBuilder setDelta(boolean value) {
        metric.getMetricProperties().setDelta(Boolean.toString(value));
        return this;
    }

    public static String popMetricPath(String path) {
        String[] basePathTokens = path.split("\\" + Constants.METRIC_SEPARATOR);
        basePathTokens = Arrays.copyOf(basePathTokens, basePathTokens.length - 1);
        return String.join(Constants.METRIC_SEPARATOR, basePathTokens);
    }

    public MetricBuilder setName(String metricName) {
        String basePath = popMetricPath(metric.getMetricPath());
        String fullPath = basePath + Constants.METRIC_SEPARATOR + metric.getMetricName();

        metric = new Metric(metricName, metric.getMetricValue(), fullPath,
                metric.getAggregationType(), metric.getTimeRollUpType(), metric.getClusterRollUpType());

        return this;
    }

    public MetricBuilder setLabels(Map<String, String> labels) {
        String basePath = popMetricPath(metric.getMetricPath());

        if (!labels.isEmpty()) {
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                basePath += Constants.METRIC_SEPARATOR + entry.getKey() + "=" + entry.getValue();
            }
        }

        String fullPath = basePath + Constants.METRIC_SEPARATOR + metric.getMetricName();
        metric.setMetricPath(fullPath);

        return this;

    }

    public MetricBuilder setRollupTypes(String defaultAggregationType,
            String defaultTimeRollUpType, String defaultClusterRollUpType) {
        MetricProperties props = metric.getMetricProperties();

        if (props instanceof DefaultMetricProperties) {
            props.setAggregationType(defaultAggregationType);
            props.setTimeRollUpType(defaultTimeRollUpType);
            props.setClusterRollUpType(defaultClusterRollUpType);
        }

        return this;
    }
}
