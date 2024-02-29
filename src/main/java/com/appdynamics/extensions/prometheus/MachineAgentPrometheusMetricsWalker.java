package com.appdynamics.extensions.prometheus;

import static com.appdynamics.extensions.prometheus.utils.Constants.METRIC_SEPARATOR;

import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.DefaultMetricProperties;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.metrics.MetricProperties;
import com.appdynamics.extensions.metrics.MetricPropertiesBuilder;
import com.appdynamics.extensions.metrics.derived.DerivedMetricsCalculator;
import com.appdynamics.extensions.prometheus.utils.Constants;
import com.appdynamics.extensions.prometheus.utils.MetricBuilder;
import com.appdynamics.extensions.prometheus.utils.MetricConfigMap;
import com.appdynamics.extensions.util.MetricPathUtils;
import com.google.common.collect.Lists;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import prometheus.types.Counter;
import prometheus.types.Gauge;
import prometheus.types.Histogram;
import prometheus.types.MetricFamily;
import prometheus.types.Summary;
import prometheus.types.Untyped;
import prometheus.types.Histogram.Bucket;
import prometheus.walkers.PrometheusMetricsWalker;

public class MachineAgentPrometheusMetricsWalker implements PrometheusMetricsWalker {

    private MonitorContextConfiguration monitorContextConfiguration;
    private MetricWriteHelper metricWriteHelper;
    private long previousTimeStamp;
    private long currentTimeStamp;
    private Logger logger = LoggerFactory.getLogger(MachineAgentPrometheusMetricsWalker.class);
    private final List<Metric> metricsToPublish = Lists.newArrayList();
    private Map<String, Double> previousValues;
    private MetricConfigMap metricConfigMap;

    public MachineAgentPrometheusMetricsWalker(MonitorContextConfiguration monitorContextConfiguration,
            MetricWriteHelper metricWriteHelper, long previousTimeStamp, long currentTimeStamp,
            Map<String, Double> previousValues, MetricConfigMap metricConfigMap) {
        this.monitorContextConfiguration = monitorContextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.previousTimeStamp = previousTimeStamp;
        this.currentTimeStamp = currentTimeStamp;
        this.previousValues = previousValues;
        this.metricConfigMap = metricConfigMap;
    }

    @Override
    public void walkStart() {
        logger.debug("walkStart");
    }

    @Override
    public void walkFinish(int familiesProcessed, int metricsProcessed) {
        logger.info("publishing {} metrics", metricsToPublish.size());
        metricWriteHelper.transformAndPrintMetrics(metricsToPublish);
        logger.debug("walkFinish");
    }

    @Override
    public void walkMetricFamily(MetricFamily family, int index) {
        logger.debug(String.format("walkMetricFamily: %s (%s): %s",
                family.getName(), family.getType(), family.getHelp()));
    }

    @Override
    public void walkCounterMetric(MetricFamily family, Counter counter, int index) {
        if (!metricConfigMap.hasFamily(family.getName())) {
            logger.debug("metric family {} is not wanted", family.getName());
            return;
        }

        Double value = counter.getValue();
        if (value == null || value < 0 || value >= Long.MAX_VALUE) {
            logger.debug(String.format("skipping invalid counter value %f for %s%s",
                    counter.getValue(), family.getName(), counter.getLabels().toString()));
            return;
        }

        Metric metric = metricConfigMap.getMetric(family.getName());
        metric = new MetricBuilder(metric)
                .setValue(value)
                .setLabels(counter.getLabels())
                .setRollupTypes(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE)
                .build();
        metricsToPublish.add(metric);

        String metricPath = metric.getMetricPath();
        Boolean hasPrevious = previousValues.containsKey(metricPath);
        if (hasPrevious) {
            Double timeDelta = (double) (currentTimeStamp - previousTimeStamp);
            Double metricDelta = counter.getValue() - previousValues.get(metricPath);
            Double delta = metricDelta / timeDelta * 1000.0;
            if (delta >= 0.0) {
                Metric rate = new Metric(metric.getMetricName() + "_per_sec", "-1",
                        metricPath + "_per_sec");
                rate = new MetricBuilder(rate)
                        .setValue(delta)
                        .setRollupTypes(metric.getAggregationType(),
                                metric.getTimeRollUpType(),
                                metric.getClusterRollUpType())
                        .build();
                metricsToPublish.add(rate);
            }
        }
        previousValues.put(metricPath, counter.getValue());
    }

    @Override
    public void walkGaugeMetric(MetricFamily family, Gauge gauge, int index) {
        if (!metricConfigMap.hasFamily(family.getName())) {
            logger.debug("metric family {} is not wanted", family.getName());
            return;
        }

        Double value = gauge.getValue();
        if (value == null || value < 0 || value >= Long.MAX_VALUE) {
            logger.debug(String.format("skipping invalid gauge value %f for %s%s",
                    gauge.getValue(), family.getName(), gauge.getLabels().toString()));
            return;
        }

        Metric metric = metricConfigMap.getMetric(family.getName());
        metric = new MetricBuilder(metric)
                .setValue(value)
                .setLabels(gauge.getLabels())
                .setRollupTypes(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE)
                .build();

        if (metric.getMetricValue() == null || metric.getMetricValue() == "-1") {
            throw new InvalidParameterException(String.format("%s has value -1",
                    metric.getMetricPath()));
        }

        metricsToPublish.add(metric);
    }

    @Override
    public void walkSummaryMetric(MetricFamily family, Summary summary, int index) {
        if (!metricConfigMap.hasFamily(family.getName())) {
            logger.debug("metric family {} is not wanted", family.getName());
            return;
        }

        Metric metric = metricConfigMap.getMetric(family.getName());
        Metric countMetric = new MetricBuilder(metric)
                .setName(summary.getName() + "_count")
                .setValue(summary.getSampleCount())
                .setLabels(summary.getLabels())
                .setRollupTypes(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE)
                .build();
        metricsToPublish.add(countMetric);

        Metric sumMetric = new MetricBuilder(metric)
                .setName(summary.getName() + "_sum")
                .setValue(summary.getSampleSum())
                .setLabels(summary.getLabels())
                .setRollupTypes(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE)
                .build();
        metricsToPublish.add(sumMetric);

        for (Summary.Quantile quantile : summary.getQuantiles()) {
            Double value = quantile.getValue();
            if (value >= 0 && value < Long.MAX_VALUE) {
                Map<String, String> labels = summary.getLabels();
                labels.put("quantile", Double.toString(quantile.getQuantile()));
                Metric quantileMetric = new MetricBuilder(metric)
                        .setName(summary.getName())
                        .setValue(value)
                        .setLabels(summary.getLabels())
                        .setRollupTypes(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE)
                        .build();
                metricsToPublish.add(quantileMetric);
            } else {
                logger.debug(String.format("skipping invalid gauge value %f for %s_bucket%s",
                        quantile.getValue(), family.getName(), summary.getLabels().toString()));
            }
        }
    }

    @Override
    public void walkHistogramMetric(MetricFamily family, Histogram histogram, int index) {
        if (!metricConfigMap.hasFamily(family.getName())) {
            logger.debug("metric family {} is not wanted", family.getName());
            return;
        }

        Metric metric = metricConfigMap.getMetric(family.getName());
        Metric countMetric = new MetricBuilder(metric)
                .setName(histogram.getName() + "_count")
                .setValue(histogram.getSampleCount())
                .setLabels(histogram.getLabels())
                .setRollupTypes(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE)
                .build();
        metricsToPublish.add(countMetric);

        Metric sumMetric = new MetricBuilder(metric)
                .setName(histogram.getName() + "_sum")
                .setValue(histogram.getSampleSum())
                .setLabels(histogram.getLabels())
                .setRollupTypes(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE)
                .build();
        metricsToPublish.add(sumMetric);

        for (Bucket bucket : histogram.getBuckets()) {
            Long value = bucket.getCumulativeCount();
            if (value >= 0 && value < Long.MAX_VALUE) {
                Map<String, String> labels = histogram.getLabels();
                labels.put("le", Double.toString(bucket.getUpperBound()));
                Metric bucketMetric = new MetricBuilder(metric)
                        .setName(histogram.getName() + "_bucket")
                        .setValue(value)
                        .setLabels(histogram.getLabels())
                        .setRollupTypes(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE)
                        .build();
                metricsToPublish.add(bucketMetric);
            } else {
                logger.debug(String.format("skipping invalid gauge value %l for %s_bucket%s",
                        bucket.getCumulativeCount(), family.getName(), histogram.getLabels().toString()));
            }
        }
    }

    @Override
    public void walkUntypedMetric(MetricFamily family, Untyped untyped, int index) {
        if (!metricConfigMap.hasFamily(family.getName())) {
            logger.debug("metric family {} is not wanted", family.getName());
            return;
        }

        Double value = untyped.getValue();
        if (value == null || value < 0 || value >= Long.MAX_VALUE) {
            logger.debug(String.format("skipping invalid gauge value %f for %s%s",
                    untyped.getValue(), family.getName(), untyped.getLabels().toString()));
            return;
        }

        Metric metric = metricConfigMap.getMetric(family.getName());
        metric = new MetricBuilder(metric)
                .setValue(value)
                .setLabels(untyped.getLabels())
                .setRollupTypes(MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE)
                .build();

        if (metric.getMetricValue() == null || metric.getMetricValue() == "-1") {
            throw new InvalidParameterException(String.format("%s has value -1",
                    metric.getMetricPath()));
        }

        metricsToPublish.add(metric);
    }

}
