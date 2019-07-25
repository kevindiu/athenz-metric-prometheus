package com.yahoo.athenz.common.metrics.impl.prometheus;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;

/**
 * This class is not complete. Need to have a way to delete metric in push gateway when the instance is down.
 */
public class PrometheusPushExporter implements PrometheusExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusPushExporter.class.getName());

    private String pushGatewayAddress;
    private CollectorRegistry registry;
    private String jobName;
    private Map<String, String> groupingKey;
    private ScheduledExecutorService scheduledExecutorService;

    private Gauge pushDuration;
    private Gauge pushLastSuccess;

    public PrometheusPushExporter(String pushGatewayAddress, CollectorRegistry registry, String jobName,
            String instanceName, int pushPeriod) {
        this.pushGatewayAddress = pushGatewayAddress;
        this.registry = initRegistry(registry);
        this.jobName = jobName;
        this.groupingKey = Collections.singletonMap("instance", instanceName);

        // auto push task
        final PrometheusPushExporter self = this;
        TimerTask pushTask = new TimerTask() {
            public void run() {
                self.flush();
            }
        };
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(pushTask, pushPeriod, pushPeriod, TimeUnit.SECONDS);
    }

    private CollectorRegistry initRegistry(CollectorRegistry registry) {
        // register pull job metric collectors
        this.pushDuration = Gauge.build().name("push_duration_seconds").help("Duration of metric push in seconds.")
                .register(registry);
        this.pushLastSuccess = Gauge.build().name("push_last_success")
                .help("Last time metric push succeeded, in unix time.").register(registry);
        /*
         * } catch (IllegalArgumentException ex) { // no methods to check collector
         * already exists in registry, // skip exception if collector already
         * registered. }
         */

        return registry;
    }

    @Override
    public void flush() {
        Gauge.Timer durationTimer = this.pushDuration.startTimer();
        try {
            PushGateway pushGateway = new PushGateway(this.pushGatewayAddress);
            // pushGateway.setConnectionFactory(new
            // BasicAuthHttpConnectionFactory("my_user", "my_password"));
            // pushGateway.setConnectionFactory(new MyHttpConnectionFactory());
            pushGateway.pushAdd(this.registry, this.jobName, this.groupingKey);

            pushLastSuccess.setToCurrentTime();
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Cannot push metric to prometheus push gateway", e);
            }
        } finally {
            durationTimer.setDuration();
        }
    }

    @Override
    public void quit() {
        // unregister
        this.registry.unregister(this.pushDuration);
        this.registry.unregister(this.pushLastSuccess);
        // shutdown
        this.scheduledExecutorService.shutdown();
    }

}
