package com.yahoo.athenz.common.metrics.impl.prometheus;

public interface PrometheusExporter {

    /**
     * Flush any buffered metrics to destination.
     */
    public void flush();

    /**
     * Flush buffers and shutdown any tasks.
     */
    public void quit();

}
