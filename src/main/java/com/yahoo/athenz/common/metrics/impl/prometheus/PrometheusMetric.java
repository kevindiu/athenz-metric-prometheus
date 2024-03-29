package com.yahoo.athenz.common.metrics.impl.prometheus;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.SimpleCollector;
import io.prometheus.client.Summary;

import com.yahoo.athenz.common.metrics.Metric;

public class PrometheusMetric implements Metric {

    public static final String REQUEST_DOMAIN_LABEL_NAME = "domain";
    public static final String PRINCIPAL_DOMAIN_LABEL_NAME = "principal";

    public static final String METRIC_NAME_DELIMITER = "_";
    public static final String COUNTER_SUFFIX = "total";
    public static final String TIMER_UNIT = "seconds";

    private final CollectorRegistry registry;
    private final ConcurrentMap<String, Collector> namesToCollectors;
    private final PrometheusExporter exporter;
    private String namespace;
    private boolean isLabelRequestDomainNameEnable;
    private boolean isLabelPrincipalDomainNameEnable;

    /**
     * @param registry CollectorRegistry of all metrics
     * @param exporter Prometheus metrics exporter
     * @param namespace prefix of all metrics
     */
    public PrometheusMetric(CollectorRegistry registry, ConcurrentMap<String, Collector> namesToCollectors, PrometheusExporter exporter, String namespace) {
        this(registry, namesToCollectors, exporter, namespace, false, false);
    }

    /**
     * @param registry CollectorRegistry of all metrics
     * @param exporter Prometheus metrics exporter
     * @param namespace prefix of all metrics
     * @param isLabelRequestDomainNameEnable enable requestDomainName label
     * @param isLabelPrincipalDomainNameEnable enable principalDomainName label
     */
    public PrometheusMetric(CollectorRegistry registry, ConcurrentMap<String, Collector> namesToCollectors, PrometheusExporter exporter, String namespace, boolean isLabelRequestDomainNameEnable, boolean isLabelPrincipalDomainNameEnable) {
        this.registry = registry;
        this.namesToCollectors = namesToCollectors;
        this.exporter = exporter;
        this.namespace = namespace;

        this.isLabelRequestDomainNameEnable = isLabelRequestDomainNameEnable;
        this.isLabelPrincipalDomainNameEnable = isLabelPrincipalDomainNameEnable;
    }

    @Override
    public void increment(String metricName) {
        increment(metricName, null, 1);
    }

    @Override
    public void increment(String metricName, String requestDomainName) {
        increment(metricName, requestDomainName, 1);
    }

    @Override
    public void increment(String metricName, String requestDomainName, String principalDomainName) {
        increment(metricName, requestDomainName, principalDomainName, 1);
    }

    @Override
    public void increment(String metricName, String requestDomainName, int count) {
        increment(metricName, requestDomainName, null, count);
    }

    @Override
    public void increment(String metricName, String requestDomainName, String principalDomainName, int count) {
        // prometheus does not allow null labels
        requestDomainName = (this.isLabelRequestDomainNameEnable) ? Objects.toString(requestDomainName, "") : "";
        principalDomainName = (this.isLabelPrincipalDomainNameEnable) ? Objects.toString(principalDomainName, "") : "";

        metricName = this.normalizeCounterMetricName(metricName);
        Counter counter = (Counter) createOrGetCollector(metricName, Counter.build());
        counter.labels(requestDomainName, principalDomainName).inc(count);
    }

    @Override
    public Object startTiming(String metricName, String requestDomainName) {
        return startTiming(metricName, requestDomainName, null);
    }

    @Override
    public Object startTiming(String metricName, String requestDomainName, String principalDomainName) {
        // prometheus does not allow null labels
        requestDomainName = (this.isLabelRequestDomainNameEnable) ? Objects.toString(requestDomainName, "") : "";
        principalDomainName = (this.isLabelPrincipalDomainNameEnable) ? Objects.toString(principalDomainName, "") : "";

        metricName = this.normalizeTimerMetricName(metricName);
        Summary summary = (Summary) createOrGetCollector(metricName, Summary.build()
        // .quantile(0.5, 0.05)
        // .quantile(0.9, 0.01)
        );
        return summary.labels(requestDomainName, principalDomainName).startTimer();
    }

    @Override
    public void stopTiming(Object timerObj) {
        if (timerObj == null) {
            return;
        }
        Summary.Timer timer = (Summary.Timer) timerObj;
        timer.observeDuration();
    }

    @Override
    public void stopTiming(Object timerObj, String requestDomainName, String principalDomainName) {
        stopTiming(timerObj);
    }

    @Override
    public void flush() {
        if (this.exporter != null) {
            this.exporter.flush();
        }
    }

    @Override
    public void quit() {
        if (this.exporter != null) {
            this.exporter.flush();
            this.exporter.quit();
        }
    }

    /**
     * Create collector and register it to the registry.
     * This is needed since Athenz metric names are defined on runtime and we need the same collector object to record the data.
     * @param metricName Name of the metric
     * @param builder Prometheus Collector Builder
     */
    private Collector createOrGetCollector(String metricName, SimpleCollector.Builder<?, ?> builder) {
        String key = metricName;
        ConcurrentMap<String, Collector> map = this.namesToCollectors;
        Collector collector = map.get(key);

        // double checked locking
        if (collector == null) {
            synchronized (map) {
                if (!map.containsKey(key)) {
                    // create
                    builder = builder
                        .namespace(this.namespace)
                        .name(metricName)
                        .help(metricName)
                        .labelNames(REQUEST_DOMAIN_LABEL_NAME, PRINCIPAL_DOMAIN_LABEL_NAME);
                    collector = builder.register(this.registry);
                    // put
                    map.put(key, collector);
                } else {
                    // get
                    collector = map.get(key);
                }
            }
        };

        return collector;
    }

    /**
     * Create counter metric name that follows prometheus standard
     * @param metricName Name of the counter metric
     */
    private String normalizeCounterMetricName(String metricName) {
        return metricName + METRIC_NAME_DELIMITER + COUNTER_SUFFIX;
    }

    /**
     * Create timer metric name that follows prometheus standard
     * @param metricName Name of the timer metric
     */
    private String normalizeTimerMetricName(String metricName) {
        return metricName + METRIC_NAME_DELIMITER + TIMER_UNIT;
    }
}
