# Test Summary

## NoOps V.S. Prometheus (Athenz endpoint)
- [Using NoOpMetric](./jmeter/no_ops/summary.csv)
- [Using PrometheusMetric](./jmeter/prometheus/summary.csv)

### Conclusion
- Throughput: (499-510)/510 * 100% = `-2.16%`
- **not much performance impact on existing API**

## without domain V.S. with 2000 domain (prometheus endpoint)
- [label disabled](./jmeter/metric-no-label/summary.csv)
- [label enabled, with 2000 domain as label](./jmeter/metric-2000-domain/summary.csv)

### Conclusion
- Throughput: (4-44)/44 * 100% = `-90.9%`
- **should not enable metric label for Athenz domain**
