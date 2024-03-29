package com.yahoo.athenz.common.metrics.impl.prometheus;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.SimpleCollector;
import io.prometheus.client.Summary;

public class PrometheusMetricTest {

    private String[] labelNames = {
        PrometheusMetric.REQUEST_DOMAIN_LABEL_NAME,
        PrometheusMetric.PRINCIPAL_DOMAIN_LABEL_NAME
    };

    @Test
    public void testConstructor() {
        CollectorRegistry registry = mock(CollectorRegistry.class);
        ConcurrentHashMap<String, Collector> namesToCollectors = new ConcurrentHashMap<>();
        PrometheusExporter exporter = mock(PrometheusExporter.class);
        String namespace = "constructor_test";
        boolean isLabelRequestDomainNameEnable = true;
        boolean isLabelPrincipalDomainNameEnable = true;

        BiFunction<Field, PrometheusMetric, Object> getFieldValue = (f, object) -> {
            try {
                f.setAccessible(true);
                return f.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        };

        PrometheusMetric metric_1 = new PrometheusMetric(registry, namesToCollectors, exporter, namespace);
        // assertions
        for (Field f : metric_1.getClass().getDeclaredFields()) {
            switch (f.getName()) {
            case "registry":
                Assert.assertSame(getFieldValue.apply(f, metric_1), registry);
                break;
            case "namesToCollectors":
                Assert.assertSame(getFieldValue.apply(f, metric_1), namesToCollectors);
                break;
            case "exporter":
                Assert.assertSame(getFieldValue.apply(f, metric_1), exporter);
                break;
            case "namespace":
                Assert.assertSame(getFieldValue.apply(f, metric_1), namespace);
                break;
            case "isLabelRequestDomainNameEnable":
                Assert.assertEquals(getFieldValue.apply(f, metric_1), false);
                break;
            case "isLabelPrincipalDomainNameEnable":
                Assert.assertEquals(getFieldValue.apply(f, metric_1), false);
                break;
            default:
                break;
            }
        }

        // different signature
        PrometheusMetric metric_2 = new PrometheusMetric(registry, namesToCollectors, exporter, namespace,
                isLabelRequestDomainNameEnable, isLabelPrincipalDomainNameEnable);
        // assertions
        for (Field f : metric_2.getClass().getDeclaredFields()) {
            switch (f.getName()) {
            case "registry":
                Assert.assertSame(getFieldValue.apply(f, metric_2), registry);
                break;
            case "namesToCollectors":
                Assert.assertSame(getFieldValue.apply(f, metric_2), namesToCollectors);
                break;
            case "exporter":
                Assert.assertSame(getFieldValue.apply(f, metric_2), exporter);
                break;
            case "namespace":
                Assert.assertSame(getFieldValue.apply(f, metric_2), namespace);
                break;
            case "isLabelRequestDomainNameEnable":
                Assert.assertEquals(getFieldValue.apply(f, metric_2), isLabelRequestDomainNameEnable);
                break;
            case "isLabelPrincipalDomainNameEnable":
                Assert.assertEquals(getFieldValue.apply(f, metric_2), isLabelPrincipalDomainNameEnable);
                break;
            default:
                break;
            }
        }
    }

    @Test
    public void testCreateOrGetCollector() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        CollectorRegistry registry = new CollectorRegistry();
        ConcurrentHashMap<String, Collector> namesToCollectors = new ConcurrentHashMap<>();
        PrometheusMetric metric = new PrometheusMetric(registry, namesToCollectors, null, "");
        Method createOrGetCollector = metric.getClass().getDeclaredMethod("createOrGetCollector", String.class, SimpleCollector.Builder.class);
        createOrGetCollector.setAccessible(true);

        // test create
        String metricName = "metric_test";
        Counter.Builder builder = Counter.build();
        double countValue = 110.110d;
        Counter counter = (Counter) createOrGetCollector.invoke(metric, metricName, builder);
        counter.labels("", "").inc(countValue);
        // assertions
        Assert.assertSame(counter, namesToCollectors.get(metricName));
        Assert.assertEquals(registry.getSampleValue(metricName, this.labelNames, new String[]{"", ""}), countValue);

        // test get
        Counter counter_2 = (Counter) createOrGetCollector.invoke(metric, metricName, builder);
        // assertions
        Assert.assertSame(counter_2, namesToCollectors.get(metricName));
        Assert.assertSame(counter_2, counter);
    }

    @Test
    public void testIncrement() {
        CollectorRegistry registry = new CollectorRegistry();
        ConcurrentHashMap<String, Collector> namesToCollectors = new ConcurrentHashMap<>();
        String namespace = "metric_test";
        int count = 24;

        // 1. no labels (default)
        PrometheusMetric metric_1 = new PrometheusMetric(registry, namesToCollectors, null, namespace);
        String metricName_1 = "test_counter_1";
        String fullMetricName_1 = namespace + "_" + metricName_1 + "_" + PrometheusMetric.COUNTER_SUFFIX;
        String requestDomainName_1 = "request_domain_1";
        String principalDomainName_1 = "principal_domain_1";
        // assertions
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ "", "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ requestDomainName_1, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ "", principalDomainName_1 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ requestDomainName_1, principalDomainName_1 }));
        metric_1.increment(metricName_1);
        metric_1.increment(metricName_1, requestDomainName_1);
        metric_1.increment(metricName_1, null, principalDomainName_1);
        metric_1.increment(metricName_1, requestDomainName_1, principalDomainName_1);
        metric_1.increment(metricName_1, null, count);
        metric_1.increment(metricName_1, requestDomainName_1, count);
        metric_1.increment(metricName_1, null, principalDomainName_1, count);
        metric_1.increment(metricName_1, requestDomainName_1, principalDomainName_1, count);
        Assert.assertEquals(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ "", "" }), 4d + 24d * 4d, 0.1d);
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ requestDomainName_1, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ "", principalDomainName_1 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ requestDomainName_1, principalDomainName_1 }));

        // 2. only request domain
        PrometheusMetric metric_2 = new PrometheusMetric(registry, namesToCollectors, null, namespace, true, false);
        String metricName_2 = "test_counter_2";
        String fullMetricName_2 = namespace + "_" + metricName_2 + "_" + PrometheusMetric.COUNTER_SUFFIX;
        String requestDomainName_2 = "request_domain_2";
        String principalDomainName_2 = "principal_domain_2";
        // assertions
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ "", "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ requestDomainName_2, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ "", principalDomainName_2 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ requestDomainName_2, principalDomainName_2 }));
        metric_2.increment(metricName_2);
        metric_2.increment(metricName_2, requestDomainName_2);
        metric_2.increment(metricName_2, null, principalDomainName_2);
        metric_2.increment(metricName_2, requestDomainName_2, principalDomainName_2);
        metric_2.increment(metricName_2, null, count);
        metric_2.increment(metricName_2, requestDomainName_2, count);
        metric_2.increment(metricName_2, null, principalDomainName_2, count);
        metric_2.increment(metricName_2, requestDomainName_2, principalDomainName_2, count);
        Assert.assertEquals(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ "", "" }), 2d + 24d * 2d, 0.1d);
        Assert.assertEquals(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ requestDomainName_2, "" }), 2d + 24d * 2d, 0.1d);
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ "", principalDomainName_2 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ requestDomainName_2, principalDomainName_2 }));

        // 3. only principal domain
        PrometheusMetric metric_3 = new PrometheusMetric(registry, namesToCollectors, null, namespace, false, true);
        String metricName_3 = "test_counter_3";
        String fullMetricName_3 = namespace + "_" + metricName_3 + "_" + PrometheusMetric.COUNTER_SUFFIX;
        String requestDomainName_3 = "request_domain_3";
        String principalDomainName_3 = "principal_domain_3";
        // assertions
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ "", "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ requestDomainName_3, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ "", principalDomainName_3 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ requestDomainName_3, principalDomainName_3 }));
        metric_3.increment(metricName_3);
        metric_3.increment(metricName_3, requestDomainName_3);
        metric_3.increment(metricName_3, null, principalDomainName_3);
        metric_3.increment(metricName_3, requestDomainName_3, principalDomainName_3);
        metric_3.increment(metricName_3, null, count);
        metric_3.increment(metricName_3, requestDomainName_3, count);
        metric_3.increment(metricName_3, null, principalDomainName_3, count);
        metric_3.increment(metricName_3, requestDomainName_3, principalDomainName_3, count);
        Assert.assertEquals(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ "", "" }), 2d + 24d * 2d, 0.1d);
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ requestDomainName_3, "" }));
        Assert.assertEquals(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ "", principalDomainName_3 }), 2d + 24d * 2d, 0.1d);
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ requestDomainName_3, principalDomainName_3 }));

        // 4. enable both labels
        PrometheusMetric metric_4 = new PrometheusMetric(registry, namesToCollectors, null, namespace, true, true);
        String metricName_4 = "test_counter_4";
        String fullMetricName_4 = namespace + "_" + metricName_4 + "_" + PrometheusMetric.COUNTER_SUFFIX;
        String requestDomainName_4 = "request_domain_4";
        String principalDomainName_4 = "principal_domain_4";
        // assertions
        Assert.assertNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ "", "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ requestDomainName_4, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ "", principalDomainName_4 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ requestDomainName_4, principalDomainName_4 }));
        metric_4.increment(metricName_4);
        metric_4.increment(metricName_4, requestDomainName_4);
        metric_4.increment(metricName_4, null, principalDomainName_4);
        metric_4.increment(metricName_4, requestDomainName_4, principalDomainName_4);
        metric_4.increment(metricName_4, null, count);
        metric_4.increment(metricName_4, requestDomainName_4, count);
        metric_4.increment(metricName_4, null, principalDomainName_4, count);
        metric_4.increment(metricName_4, requestDomainName_4, principalDomainName_4, count);
        Assert.assertEquals(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ "", "" }), 1d + 24d, 0.1d);
        Assert.assertEquals(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ requestDomainName_4, "" }), 1d + 24d, 0.1d);
        Assert.assertEquals(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ "", principalDomainName_4 }), 1d + 24d, 0.1d);
        Assert.assertEquals(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ requestDomainName_4, principalDomainName_4 }), 1d + 24d, 0.1d);
    }

    @Test
    public void testStartTiming() {
        CollectorRegistry registry = new CollectorRegistry();
        ConcurrentHashMap<String, Collector> namesToCollectors = new ConcurrentHashMap<>();
        String namespace = "metric_test";

        // 1. no labels (default)
        PrometheusMetric metric_1 = new PrometheusMetric(registry, namesToCollectors, null, namespace);
        String metricName_1 = "test_timer_1";
        String fullMetricName_1 = namespace + "_" + metricName_1 + "_" + PrometheusMetric.TIMER_UNIT + "_sum";
        String requestDomainName_1 = "request_domain_1";
        String principalDomainName_1 = "principal_domain_1";
        // assertions
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ "", "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ requestDomainName_1, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ "", principalDomainName_1 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ requestDomainName_1, principalDomainName_1 }));
        metric_1.stopTiming(metric_1.startTiming(metricName_1, null));
        metric_1.stopTiming(metric_1.startTiming(metricName_1, requestDomainName_1));
        metric_1.stopTiming(metric_1.startTiming(metricName_1, null, principalDomainName_1));
        metric_1.stopTiming(metric_1.startTiming(metricName_1, requestDomainName_1, principalDomainName_1));
        Assert.assertNotNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ "", "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ requestDomainName_1, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ "", principalDomainName_1 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_1, this.labelNames, new String[]{ requestDomainName_1, principalDomainName_1 }));

        // 2. only request domain
        PrometheusMetric metric_2 = new PrometheusMetric(registry, namesToCollectors, null, namespace, true, false);
        String metricName_2 = "test_timer_2";
        String fullMetricName_2 = namespace + "_" + metricName_2 + "_" + PrometheusMetric.TIMER_UNIT + "_sum";
        String requestDomainName_2 = "request_domain_2";
        String principalDomainName_2 = "principal_domain_2";
        // assertions
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ "", "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ requestDomainName_2, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ "", principalDomainName_2 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ requestDomainName_2, principalDomainName_2 }));
        metric_2.stopTiming(metric_2.startTiming(metricName_2, null));
        metric_2.stopTiming(metric_2.startTiming(metricName_2, requestDomainName_2));
        metric_2.stopTiming(metric_2.startTiming(metricName_2, null, principalDomainName_2));
        metric_2.stopTiming(metric_2.startTiming(metricName_2, requestDomainName_2, principalDomainName_2));
        Assert.assertNotNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ "", "" }));
        Assert.assertNotNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ requestDomainName_2, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ "", principalDomainName_2 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_2, this.labelNames, new String[]{ requestDomainName_2, principalDomainName_2 }));

        // 3. only principal domain
        PrometheusMetric metric_3 = new PrometheusMetric(registry, namesToCollectors, null, namespace, false, true);
        String metricName_3 = "test_timer_3";
        String fullMetricName_3 = namespace + "_" + metricName_3 + "_" + PrometheusMetric.TIMER_UNIT + "_sum";
        String requestDomainName_3 = "request_domain_3";
        String principalDomainName_3 = "principal_domain_3";
        // assertions
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ "", "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ requestDomainName_3, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ "", principalDomainName_3 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ requestDomainName_3, principalDomainName_3 }));
        metric_3.stopTiming(metric_3.startTiming(metricName_3, null));
        metric_3.stopTiming(metric_3.startTiming(metricName_3, requestDomainName_3));
        metric_3.stopTiming(metric_3.startTiming(metricName_3, null, principalDomainName_3));
        metric_3.stopTiming(metric_3.startTiming(metricName_3, requestDomainName_3, principalDomainName_3));
        Assert.assertNotNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ "", "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ requestDomainName_3, "" }));
        Assert.assertNotNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ "", principalDomainName_3 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_3, this.labelNames, new String[]{ requestDomainName_3, principalDomainName_3 }));

        // 4. enable both labels
        PrometheusMetric metric_4 = new PrometheusMetric(registry, namesToCollectors, null, namespace, true, true);
        String metricName_4 = "test_timer_4";
        String fullMetricName_4 = namespace + "_" + metricName_4 + "_" + PrometheusMetric.TIMER_UNIT + "_sum";
        String requestDomainName_4 = "request_domain_4";
        String principalDomainName_4 = "principal_domain_4";
        // assertions
        Assert.assertNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ "", "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ requestDomainName_4, "" }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ "", principalDomainName_4 }));
        Assert.assertNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ requestDomainName_4, principalDomainName_4 }));
        metric_4.stopTiming(metric_4.startTiming(metricName_4, null));
        metric_4.stopTiming(metric_4.startTiming(metricName_4, requestDomainName_4));
        metric_4.stopTiming(metric_4.startTiming(metricName_4, null, principalDomainName_4));
        metric_4.stopTiming(metric_4.startTiming(metricName_4, requestDomainName_4, principalDomainName_4));
        Assert.assertNotNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ "", "" }));
        Assert.assertNotNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ requestDomainName_4, "" }));
        Assert.assertNotNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ "", principalDomainName_4 }));
        Assert.assertNotNull(registry.getSampleValue(fullMetricName_4, this.labelNames, new String[]{ requestDomainName_4, principalDomainName_4 }));
    }

    @Test
    public void testStopTiming() {
        Summary.Timer timer = mock(Summary.Timer.class);
        PrometheusMetric metric = new PrometheusMetric(null, null, null, "", false, false);

        metric.stopTiming(timer);
        // assertions
        verify(timer, times(1)).observeDuration();

        // different signature
        metric.stopTiming(timer, "request_domain", "principal_domain");
        // assertions
        verify(timer, times(2)).observeDuration();
    }
    @Test
    public void testStopTimingOnNull() {
        PrometheusMetric metric = new PrometheusMetric(null, null, null, "", false, false);
        metric.stopTiming(null);

        // no exceptions, and no actions
    }

    @Test
    public void testFlush() {
        PrometheusExporter exporter = mock(PrometheusExporter.class);

        PrometheusMetric metric = new PrometheusMetric(null, null, exporter, "", false, false);
        metric.flush();
        // assertions
        verify(exporter, times(1)).flush();

        // test null exporter
        metric = new PrometheusMetric(null, null, null, "", false, false);
        metric.flush();
        // no exceptions, and no actions
    }

    @Test
    public void testQuit() {
        PrometheusExporter exporter = mock(PrometheusExporter.class);

        PrometheusMetric metric = new PrometheusMetric(null, null, exporter, "", false, false);
        metric.quit();
        // assertions
        verify(exporter, times(1)).flush();
        verify(exporter, times(1)).quit();

        // test null exporter
        metric = new PrometheusMetric(null, null, null, "", false, false);
        metric.quit();
        // no exceptions, and no actions
    }

}
