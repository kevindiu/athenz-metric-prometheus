<a id="markdown-athenz-metric-for-prometheus" name="athenz-metric-for-prometheus"></a>
# Athenz metric for Prometheus
Athenz Yahoo Server metrics interface implementation for Prometheus

<!-- TOC -->

- [Athenz metric for Prometheus](#athenz-metric-for-prometheus)
    - [Build](#build)
    - [Test coverage](#test-coverage)
    - [Integrate with Athenz](#integrate-with-athenz)

<!-- /TOC -->

<a id="markdown-build" name="build"></a>
## Build
```bash
mvn clean package
ls ./target/athenz_metrics_prometheus-*.jar
```

<a id="markdown-test-coverage" name="test-coverage"></a>
## Test coverage
```bash
mvn clover:instrument clover:aggregate clover:clover clover:check
open ./target/site/clover/index.html
```

<a id="markdown-integrate-with-athenz" name="integrate-with-athenz"></a>
## Integrate with Athenz
1. add `athenz_metrics_prometheus-*.jar` in Athenz server's classpath
1. overwrite existing system property
    ```properties
    # ZMS server
    athenz.zms.metric_factory_class=com.yahoo.athenz.common.metrics.impl.prometheus.PrometheusMetricFactory

    # ZTS server
    athenz.zts.metric_factory_class=com.yahoo.athenz.common.metrics.impl.prometheus.PrometheusMetricFactory
    ```
1. add system property for `PrometheusMetric`
    ```properties
    # enable PrometheusMetric class
    athenz.metrics.prometheus.enable=true
    # export JVM metrics
    athenz.metrics.prometheus.jvm.enable=true
    # the Prometheus /metrics endpoint
    athenz.metrics.prometheus.http_server.enable=true
    athenz.metrics.prometheus.http_server.port=8181
    # Prometheus metric prefix
    athenz.metrics.prometheus.namespace=athenz_zms
    # for dev. env. ONLY, record Athenz domain data as label
    athenz.metrics.prometheus.label.request_domain_name.enable=false
    athenz.metrics.prometheus.label.principal_domain_name.enable=false
    ```
1. verify setup: `curl localhost:8181/metrics`
1. add job in your Prometheus server
    ```yaml
    scrape_configs:
        - job_name: 'athenz-server'
            scrape_interval: 10s
            honor_labels: true
            static_configs:
                - targets: ['athenz.server.domain:8181']
    ```
