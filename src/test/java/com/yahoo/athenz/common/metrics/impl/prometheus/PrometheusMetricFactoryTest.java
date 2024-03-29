package com.yahoo.athenz.common.metrics.impl.prometheus;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.prometheus.client.CollectorRegistry;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.yahoo.athenz.common.metrics.Metric;
import com.yahoo.athenz.common.metrics.impl.NoOpMetric;

public class PrometheusMetricFactoryTest {

    private static String setProperty(String key, String value) {
        return System.setProperty(PrometheusMetricFactory.SYSTEM_PROP_PREFIX + key, value);
    }

    private static String clearProperty(String key) {
        return System.clearProperty(PrometheusMetricFactory.SYSTEM_PROP_PREFIX + key);
    }

    @Test
    public void testGetProperty() {
        String expected = "false";

        setProperty(PrometheusMetricFactory.ENABLE_PROP, expected);
        String prop = PrometheusMetricFactory.getProperty(PrometheusMetricFactory.ENABLE_PROP, "true");
        clearProperty(PrometheusMetricFactory.ENABLE_PROP);

        // assertions
        Assert.assertEquals(prop, expected);
    }

    @Test
    public void testCreateMetricDisable() {
        Class<?> expected = NoOpMetric.class;

        setProperty(PrometheusMetricFactory.ENABLE_PROP, "false");
        Metric metric = new PrometheusMetricFactory().create();
        clearProperty(PrometheusMetricFactory.ENABLE_PROP);

        // assertions
        Assert.assertEquals(metric.getClass(), expected);
    }

    @Test
    public void testCreateJvmMetricEnable()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        setProperty(PrometheusMetricFactory.JVM_ENABLE_PROP, "true");
        setProperty(PrometheusMetricFactory.HTTP_SERVER_ENABLE_PROP, "false");
        PrometheusMetric metric = (PrometheusMetric) new PrometheusMetricFactory().create();
        clearProperty(PrometheusMetricFactory.JVM_ENABLE_PROP);
        clearProperty(PrometheusMetricFactory.HTTP_SERVER_ENABLE_PROP);

        Field registryField = metric.getClass().getDeclaredField("registry");
        registryField.setAccessible(true);
        CollectorRegistry registry = (CollectorRegistry) registryField.get(metric);

        // assertions
        Assert.assertNotNull(registry.getSampleValue("process_cpu_seconds_total"));
    }

    @Test(expectedExceptions = { RuntimeException.class, BindException.class }, expectedExceptionsMessageRegExp = ".* Address already in use.*")
    public void testCreateErrorUsedPort() throws IOException {
        int port = 18181;
        try (Socket socket = new Socket()) {
            socket.bind(new InetSocketAddress(port));

            setProperty(PrometheusMetricFactory.HTTP_SERVER_ENABLE_PROP, "true");
            setProperty(PrometheusMetricFactory.HTTP_SERVER_PORT_PROP, String.valueOf(port));
            new PrometheusMetricFactory().create();
            clearProperty(PrometheusMetricFactory.HTTP_SERVER_ENABLE_PROP);
            clearProperty(PrometheusMetricFactory.HTTP_SERVER_PORT_PROP);
        }
    }

    @Test
    public void testCreate() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Class<PrometheusPullServer> expectedExporterClass = PrometheusPullServer.class;
        String expectedNamespace = "expected_athenz_server";
        boolean expectedIsLabelRequestDomainNameEnable = true;
        boolean expectedIsLabelPrincipalDomainNameEnable = true;

        PrometheusMetric metric = null;
        try {
            setProperty(PrometheusMetricFactory.HTTP_SERVER_ENABLE_PROP, "true");
            setProperty(PrometheusMetricFactory.NAMESPACE_PROP, expectedNamespace);
            setProperty(PrometheusMetricFactory.LABEL_REQUEST_DOMAIN_NAME_ENABLE_PROP, String.valueOf(expectedIsLabelRequestDomainNameEnable));
            setProperty(PrometheusMetricFactory.LABEL_PRINCIPAL_DOMAIN_NAME_ENABLE_PROP, String.valueOf(expectedIsLabelPrincipalDomainNameEnable));
            metric = (PrometheusMetric) new PrometheusMetricFactory().create();
            clearProperty(PrometheusMetricFactory.HTTP_SERVER_ENABLE_PROP);
            clearProperty(PrometheusMetricFactory.NAMESPACE_PROP);
            clearProperty(PrometheusMetricFactory.LABEL_REQUEST_DOMAIN_NAME_ENABLE_PROP);
            clearProperty(PrometheusMetricFactory.LABEL_PRINCIPAL_DOMAIN_NAME_ENABLE_PROP);

            // assertions
            Field exporterField = metric.getClass().getDeclaredField("exporter");
            exporterField.setAccessible(true);
            PrometheusExporter exporter = (PrometheusExporter) exporterField.get(metric);
            Assert.assertEquals(exporter.getClass(), expectedExporterClass);

            Field namespaceField = metric.getClass().getDeclaredField("namespace");
            namespaceField.setAccessible(true);
            String namespace = (String) namespaceField.get(metric);
            Assert.assertEquals(namespace, expectedNamespace);

            Field isLabelRequestDomainNameEnableField = metric.getClass().getDeclaredField("isLabelRequestDomainNameEnable");
            isLabelRequestDomainNameEnableField.setAccessible(true);
            boolean isLabelRequestDomainNameEnable = (Boolean) isLabelRequestDomainNameEnableField.get(metric);
            Assert.assertEquals(isLabelRequestDomainNameEnable, expectedIsLabelRequestDomainNameEnable);

            Field isLabelPrincipalDomainNameEnableField = metric.getClass().getDeclaredField("isLabelPrincipalDomainNameEnable");
            isLabelPrincipalDomainNameEnableField.setAccessible(true);
            boolean isLabelPrincipalDomainNameEnable = (Boolean) isLabelPrincipalDomainNameEnableField.get(metric);
            Assert.assertEquals(isLabelPrincipalDomainNameEnable, expectedIsLabelPrincipalDomainNameEnable);
        } finally {
            // cleanup
            if (metric != null) {
                metric.quit();
            }
        }
    }

}
