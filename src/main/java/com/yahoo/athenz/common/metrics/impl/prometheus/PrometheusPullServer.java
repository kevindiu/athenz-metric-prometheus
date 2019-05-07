package com.yahoo.athenz.common.metrics.impl.prometheus;

import java.io.IOException;
import java.net.InetSocketAddress;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;

public class PrometheusPullServer implements PrometheusExporter {

    private HTTPServer server;

    public PrometheusPullServer(int pullingPort, CollectorRegistry registry) throws IOException {
        boolean isDaemon = true;
        this.server = new HTTPServer(new InetSocketAddress(pullingPort), registry, isDaemon);
    }

    @Override
    public void flush() {
        // should response to pull request from prometheus only, no action on flush
    }

    @Override
    public void quit() {
        this.server.stop();
    }

}
