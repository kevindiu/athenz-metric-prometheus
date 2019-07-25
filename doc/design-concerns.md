# Design concerns

1. metric name format
    1. `{namespace}_{metric}_{unit}`
        1. namespace = set by system properties
        1. metric hard coded inside Athenz
        1. unit = `total` or `seconds`
    1. reference: [Metric and label naming | Prometheus](https://prometheus.io/docs/practices/naming/#metric-names)
1. labels for `requestDomainName` and `principalDomainName`
    1. disable by default
    1. [Instrumentation#Use labels | Prometheus](https://prometheus.io/docs/practices/instrumentation/#use-labels)
    1. [Instrumentation#Do not overuse labels | Prometheus](https://prometheus.io/docs/practices/instrumentation/#do-not-overuse-labels)
        1. not a suggested way in Prometheus
1. Prometheus pull as default
    1. require same network (Prometheus server, Athenz server)
    1. the suggested deployment for Prometheus
    1. open firewall port for Grafana for query

## NOT doing
1. make metrics method calls async
    - current implement do not have much performance impact
1. support Prometheus and Athenz in different network
    1. (**anti-pattern**) ~~use push gateway~~
        - (implementation not finished yet) need to delete the previously cached metrics on service restart (`instance` label)
        - push gateway as single point of failure
            - solution
                1. push to multiple push gateways
                1. each host/server has its own push gateway
        - reference
            - [Prometheus and anti-pattern pushgateway timeouts &#8211; Craftware](https://www.craftware.info/2017/01/26/prometheus-and-anti-pattern-pushgateway-timeouts/)
            - [Common pitfalls when using the Pushgateway &#8211; Robust Perception | Prometheus Monitoring Experts](https://www.robustperception.io/common-pitfalls-when-using-the-pushgateway)
            - [Prometheus Pushgateway - kubedex.com](https://kubedex.com/resource/prometheus-pushgateway/)
    1. use proxy
        1. [RobustPerception/PushProx](https://github.com/RobustPerception/PushProx)
        1. [pambrose/prometheus-proxy](https://github.com/pambrose/prometheus-proxy)
        1. [jacksontj/promxy](https://github.com/jacksontj/promxy)
    1. remote storage
        1. [Configuration | Prometheus](https://prometheus.io/docs/prometheus/latest/configuration/configuration/#remote_write)
        1. [cortexproject/cortex](https://github.com/cortexproject/cortex)
        1. [improbable-eng/thanos](https://github.com/improbable-eng/thanos)
1. metrics with correct timestamp (on server high load)
    - need new library/implementation of the exporter
        1. idea
            1. write metric with timestamp to file as cache (by some interval)
            1. provide `/metric` endpoint to export the metric in file to Prometheus
    - reference
        1. [Exposition formats | Prometheus](https://prometheus.io/docs/instrumenting/exposition_formats/#comments-help-text-and-type-information)
        1. [Consider allowing setting a timestamp for ConstMetrics. · Issue #187 · Prometheus/client_golang](https://github.com/prometheus/client_golang/issues/187)
        1. [Treat custom textfile metric timestamps as errors by juliusv · Pull Request #769 · Prometheus/node_exporter](https://github.com/prometheus/node_exporter/pull/769)
        1. [prometheus/client_golang](https://github.com/prometheus/client_golang/blob/dae2ffdedce8449c6f0aaf64acf8ae782fd1ba07/prometheus/examples_test.go#L688-L719)
1. update `simpleclient_hotspot` to `0.6.1` (not released yet) to simplify the code
1. add Prometheus handler to jetty server to extract info.
    - require code changes in Athenz server

### extra

1. [Prometheus/prombench](https://github.com/prometheus/prombench)
1. Prometheus HA deployment
    1. have 2 or more Prometheus servers to pull the metrics (primary, secondary)
    1. switch data source in grafana
    1. articles
        - [Scaling out with Prometheus](https://www.perimeterx.com/blog/scaling-out-with-Prometheus/)
        - [explaining a  HA + scalable setup? · Issue #1500 · Prometheus/prometheus](https://github.com/prometheus/prometheus/issues/1500)
        - [Federation | Prometheus](https://prometheus.io/docs/prometheus/latest/federation/)

### reference

- Metric Impl
    - [NoOpMetric](https://github.com/yahoo/athenz/tree/master/libs/java/server_common/src/main/java/com/yahoo/athenz/common/metrics/impl)
- prometheus use case
    - [Overview | Prometheus](https://prometheus.io/docs/introduction/overview/#when-does-it-not-fit)
    - [Writing exporters | Prometheus](https://prometheus.io/docs/instrumenting/writing_exporters/#scheduling)
    - [Writing exporters | Prometheus](https://prometheus.io/docs/instrumenting/writing_exporters/#pushes)
    - [Google グループ](https://groups.google.com/forum/#!topic/prometheus-developers/sLpOnfId13o)
    - [Prometheus以及如果实现跨环境监控](http://ylzheng.com/2017/05/13/prometheus-01/)
    - [prometheus/pushgateway](https://github.com/prometheus/pushgateway#about-timestamps)
- prometheus counter
    - [How does a Prometheus Counter work? &#8211; Robust Perception | Prometheus Monitoring Experts](https://www.robustperception.io/how-does-a-prometheus-counter-work)
- read later
    - [Prometheus Counters and how to deal with them](https://www.innoq.com/en/blog/prometheus-counters/)
    - [Logs and Metrics - Cindy Sridharan - Medium](https://medium.com/@copyconstruct/logs-and-metrics-6d34d3026e38)
