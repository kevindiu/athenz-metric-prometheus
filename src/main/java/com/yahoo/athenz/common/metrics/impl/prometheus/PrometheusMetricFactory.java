package com.yahoo.athenz.common.metrics.impl.prometheus;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.*;

import com.yahoo.athenz.common.metrics.Metric;
import com.yahoo.athenz.common.metrics.MetricFactory;
import com.yahoo.athenz.common.metrics.impl.NoOpMetric;

public class PrometheusMetricFactory implements MetricFactory {

    public static final String SYSTEM_PROP_PREFIX = "athenz.metrics.prometheus.";
    public static final String ENABLE_PROP = "enable";

    public static final String JVM_ENABLE_PROP = "jvm.enable";

    public static final String HTTP_SERVER_ENABLE_PROP = "http_server.enable";
    public static final String HTTP_SERVER_PORT_PROP = "http_server.port";

    /*
    public static final String PUSH_ENABLE_PROP = "pushgateway.enable";
    public static final String PUSH_ADDRESS_PROP = "pushgateway.address";
    public static final String PUSH_JOB_NAME_PROP = "pushgateway.job_name";
    public static final String PUSH_INSTANCE_NAME_PROP = "pushgateway.instance_name";
    public static final String PUSH_PERIOD_PROP = "pushgateway.period_second";
    */

    public static final String NAMESPACE_PROP = "namespace";
    public static final String LABEL_REQUEST_DOMAIN_NAME_ENABLE_PROP = "label.request_domain_name.enable";
    public static final String LABEL_PRINCIPAL_DOMAIN_NAME_ENABLE_PROP = "label.principal_domain_name.enable";

    /**
     * Get system property related to PrometheusMetric. Property name: ${prefix}.${key}
     * @param key key without prefix
     * @param def default value
     * @return system property value
     */
    public static String getProperty(String key, String def) {
        return System.getProperty(SYSTEM_PROP_PREFIX + key, def);
    }

    @Override
    public Metric create() {

        boolean isEnable = Boolean.valueOf(getProperty(ENABLE_PROP, "true"));
        if (!isEnable) {
            return new NoOpMetric();
        }

        // metric registry, should have 1-to-1 relationship with ConcurrentHashMap namesToCollectors for collector lookup
        CollectorRegistry registry = new CollectorRegistry();
        ConcurrentHashMap<String, Collector> namesToCollectors = new ConcurrentHashMap<>();

        // register JVM metrics
        if (Boolean.valueOf(getProperty(JVM_ENABLE_PROP, "false"))) {
            // for version = 0.6.1
            // DefaultExports.register(registry);

            // for version <= 0.6.0
            new StandardExports().register(registry);
            new MemoryPoolsExports().register(registry);
            new MemoryAllocationExports().register(registry);
            new BufferPoolsExports().register(registry);
            new GarbageCollectorExports().register(registry);
            new ThreadExports().register(registry);
            new ClassLoadingExports().register(registry);
            new VersionInfoExports().register(registry);
        }

        // exporter
        PrometheusExporter exporter = null;
        if (Boolean.valueOf(getProperty(HTTP_SERVER_ENABLE_PROP, "true"))) {
            // HTTP server for pulling
            int pullingPort = Integer.valueOf(getProperty(HTTP_SERVER_PORT_PROP, "8181"));
            try {
                exporter = new PrometheusPullServer(pullingPort, registry);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        /*
        if (Boolean.valueOf(getProperty(PUSH_ENABLE_PROP, "false"))) {
            String hostname = "localhost";
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ex) {}
            String pushGatewayAddress = getProperty(PUSH_ADDRESS_PROP, "localhost:9091");
            String jobName = getProperty(PUSH_JOB_NAME_PROP, "athenz_server");
            String instanceName = getProperty(PUSH_INSTANCE_NAME_PROP, hostname);
            int interval = Integer.valueOf(getProperty(PUSH_PERIOD_PROP, "10"));

            exporter = new PrometheusPushExporter(pushGatewayAddress, registry, jobName, instanceName, interval);
        }
        */

        // prometheus metric class
        String namespace = getProperty(NAMESPACE_PROP, "athenz_server");
        boolean isLabelRequestDomainNameEnable = Boolean.valueOf(getProperty(LABEL_REQUEST_DOMAIN_NAME_ENABLE_PROP, "false"));
        boolean isLabelPrincipalDomainNameEnable = Boolean.valueOf(getProperty(LABEL_PRINCIPAL_DOMAIN_NAME_ENABLE_PROP, "false"));
        return new PrometheusMetric(registry, namesToCollectors, exporter, namespace, isLabelRequestDomainNameEnable, isLabelPrincipalDomainNameEnable);

    }

}
