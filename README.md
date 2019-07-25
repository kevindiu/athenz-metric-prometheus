## Run
```bash
mvn package exec:java -Dexec.mainClass="com.yahoo.athenz.common.metrics.Main"
```

## sample output (with default values)
```bash
$ curl localhost:8181/metrics
# HELP athenz_server_request_no_label_total request_no_label_total
# TYPE athenz_server_request_no_label_total counter
athenz_server_request_no_label_total{domain="",principal="",} 1.0
# HELP athenz_server_request01_total request01_total
# TYPE athenz_server_request01_total counter
athenz_server_request01_total{domain="",principal="",} 35.0
# HELP athenz_server_timer_test_domain_seconds timer_test_domain_seconds
# TYPE athenz_server_timer_test_domain_seconds summary
athenz_server_timer_test_domain_seconds_count{domain="",principal="",} 1.0
athenz_server_timer_test_domain_seconds_sum{domain="",principal="",} 0.113545231
# HELP athenz_server_timer_test_seconds timer_test_seconds
# TYPE athenz_server_timer_test_seconds summary
athenz_server_timer_test_seconds_count{domain="",principal="",} 1.0
athenz_server_timer_test_seconds_sum{domain="",principal="",} 0.101996235
```
