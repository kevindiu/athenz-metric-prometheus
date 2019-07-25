package com.yahoo.athenz.common.metrics;

import com.yahoo.athenz.common.metrics.Metric;
import com.yahoo.athenz.common.metrics.impl.prometheus.PrometheusMetricFactory;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("PrometheusMetric start");

        PrometheusMetricFactory pmf = new PrometheusMetricFactory();
        Metric pm = pmf.create();

        // counter
        pm.increment("request_no_label");
        pm.increment("request01", null, 5);
        pm.increment("request01", "domain01", 10);
        pm.increment("request01", "domain02", 20);

        // timer
        Object timer = pm.startTiming("timer_test", null);
        Thread.sleep(99L);
        pm.stopTiming(timer);

        Object timerD = pm.startTiming("timer_test_domain", "domain01");
        Thread.sleep(111L);
        pm.stopTiming(timerD);

        // flush
        System.out.println("before flush...");
        pm.flush();
        System.out.println("If you are using pull exporter, run 'curl localhost:8181/metrics' to verify");

        // quit
        System.out.println("wait 1 min, before quit...");
        Thread.sleep(1L * 1000 * 60);
        pm.quit();
    }
}
