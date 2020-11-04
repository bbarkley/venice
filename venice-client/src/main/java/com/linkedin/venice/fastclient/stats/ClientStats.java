package com.linkedin.venice.fastclient.stats;

import com.linkedin.venice.read.RequestType;
import com.linkedin.venice.stats.AbstractVeniceStats;
import com.linkedin.venice.stats.StatsUtils;
import com.linkedin.venice.stats.TehutiUtils;
import com.linkedin.venice.utils.concurrent.VeniceConcurrentHashMap;
import io.tehuti.metrics.MetricsRepository;
import io.tehuti.metrics.Sensor;
import io.tehuti.metrics.stats.Avg;
import io.tehuti.metrics.stats.Max;
import io.tehuti.metrics.stats.OccurrenceRate;
import io.tehuti.metrics.stats.Rate;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.apache.log4j.Logger;


public class ClientStats extends com.linkedin.venice.client.stats.ClientStats {
  private static final Logger LOGGER = Logger.getLogger(ClientStats.class);

  private final String storeName;

  private final Sensor noAvailableReplicaRequestCountSensor;
  private final Sensor dualReadFastClientSlowerRequestCountSensor;
  private final Sensor dualReadFastClientSlowerRequestRatioSensor;
  private final Sensor dualReadFastClientErrorThinClientSucceedRequestCountSensor;
  private final Sensor dualReadFastClientErrorThinClientSucceedRequestRatioSensor;
  private final Sensor dualReadThinClientFastClientLatencyDeltaSensor;

  private final Sensor leakedRequestCountSensor;

  // Routing stats
  private final Map<String, RouteStats> perRouteStats = new VeniceConcurrentHashMap<>();

  public static ClientStats getClientStats(MetricsRepository metricsRepository, String statsPrefix, String storeName,
      RequestType requestType) {
    String metricName = statsPrefix.isEmpty() ?  storeName : statsPrefix + "." + storeName;
    return new ClientStats(metricsRepository, metricName, requestType);
  }

  private ClientStats(MetricsRepository metricsRepository, String storeName, RequestType requestType) {
    super(metricsRepository, storeName, requestType);

    this.storeName = storeName;
    this.noAvailableReplicaRequestCountSensor = registerSensor("no_available_replica_request_count", new OccurrenceRate());

    Rate requestRate = getRequestRate();
    Rate fastClientSlowerRequestRate = new OccurrenceRate();
    this.dualReadFastClientSlowerRequestCountSensor = registerSensor("dual_read_fastclient_slower_request_count",
        fastClientSlowerRequestRate);
    this.dualReadFastClientSlowerRequestRatioSensor = registerSensor("dual_read_fastclient_slower_request_ratio",
        new TehutiUtils.SimpleRatioStat(fastClientSlowerRequestRate, requestRate));
    Rate fastClientErrorThinClientSucceedRequestRate = new OccurrenceRate();
    this.dualReadFastClientErrorThinClientSucceedRequestCountSensor = registerSensor(
        "dual_read_fastclient_error_thinclient_succeed_request_count", fastClientErrorThinClientSucceedRequestRate);
    this.dualReadFastClientErrorThinClientSucceedRequestRatioSensor = registerSensor(
        "", new TehutiUtils.SimpleRatioStat(fastClientErrorThinClientSucceedRequestRate, requestRate));
    this.dualReadThinClientFastClientLatencyDeltaSensor = registerSensorWithDetailedPercentiles(
        "dual_read_thinclient_fastclient_latency_delta", new Max(), new Avg());
    this.leakedRequestCountSensor = registerSensor("leaked_request_count", new OccurrenceRate());
  }

  public void recordNoAvailableReplicaRequest() {
    noAvailableReplicaRequestCountSensor.record();
  }

  public void recordFastClientSlowerRequest() {
    dualReadFastClientSlowerRequestCountSensor.record();
  }

  public void recordFastClientErrorThinClientSucceedRequest() {
    dualReadFastClientErrorThinClientSucceedRequestCountSensor.record();
  }

  public void recordThinClientFastClientLatencyDelta(double latencyDelta) {
    dualReadThinClientFastClientLatencyDeltaSensor.record(latencyDelta);
  }

  private RouteStats getRouteStats(String instanceUrl) {
    return perRouteStats.computeIfAbsent(instanceUrl, k -> {
      String instanceName = instanceUrl;
      try {
        URL url = new URL(instanceUrl);
        instanceName = url.getHost()  + "_" + url.getPort();
      } catch (MalformedURLException e) {
        LOGGER.error("Invalid instance url: " + instanceUrl);
      }
      return new RouteStats(getMetricsRepository(), storeName, instanceName);
    });
  }

  public void recordRequest(String instance) {
    getRouteStats(instance).recordRequest();
  }
  public void recordResponseWaitingTime(String instance, double latency) {
    getRouteStats(instance).recordResponseWaitingTime(latency);
  }
  public void recordHealthyRequest(String instance) {
    getRouteStats(instance).recordHealthyRequest();
  }
  public void recordQuotaExceededRequest(String instance) {
    getRouteStats(instance).recordQuotaExceededRequest();
  }
  public void recordInternalServerErrorRequest(String instance) {
    getRouteStats(instance).recordInternalServerErrorRequest();
  }
  public void recordServiceUnavailableRequest(String instance) {
    getRouteStats(instance).recordServiceUnavailableRequest();
  }
  public void recordLeakedRequest(String instance) {
    leakedRequestCountSensor.record();
    getRouteStats(instance).recordLeakedRequest();
  }
  public void recordOtherErrorRequest(String instance) {
    getRouteStats(instance).recordOtherErrorRequest();
  }

  /**
   * Per-route request metrics.
   */
  private static class RouteStats extends AbstractVeniceStats {
    private final Sensor requestCountSensor;
    private final Sensor responseWaitingTimeSensor;
    private final Sensor healthyRequestCountSensor;
    private final Sensor quotaExceededRequestCountSensor;
    private final Sensor internalServerErrorRequestCountSensor;
    private final Sensor serviceUnavailableRequestCountSensor;
    private final Sensor leakedRequestCountSensor;
    private final Sensor otherErrorRequestCountSensor;

    public RouteStats(MetricsRepository metricsRepository, String storeName, String instanceName) {
      super(metricsRepository, storeName + "." + StatsUtils.convertHostnameToMetricName(instanceName));
      this.requestCountSensor = registerSensor("request_count", new OccurrenceRate());
      this.responseWaitingTimeSensor = registerSensor("response_waiting_time", TehutiUtils.getPercentileStat(getName(), "response_waiting_time"));
      this.healthyRequestCountSensor = registerSensor("healthy_request_count", new OccurrenceRate());
      this.quotaExceededRequestCountSensor = registerSensor("quota_exceeded_request_count", new OccurrenceRate());
      this.internalServerErrorRequestCountSensor = registerSensor("internal_server_error_request_count", new OccurrenceRate());
      this.serviceUnavailableRequestCountSensor = registerSensor("service_unavailable_request_count", new OccurrenceRate());
      this.leakedRequestCountSensor = registerSensor("leaked_request_count", new OccurrenceRate());
      this.otherErrorRequestCountSensor = registerSensor("other_error_request_count", new OccurrenceRate());
    }

    public void recordRequest() {
      requestCountSensor.record();
    }
    public void recordResponseWaitingTime(double latency) {
      responseWaitingTimeSensor.record(latency);
    }
    public void recordHealthyRequest() {
      healthyRequestCountSensor.record();
    }
    public void recordQuotaExceededRequest() {
      quotaExceededRequestCountSensor.record();
    }
    public void recordInternalServerErrorRequest() {
      internalServerErrorRequestCountSensor.record();
    }
    public void recordServiceUnavailableRequest() {
      serviceUnavailableRequestCountSensor.record();
    }
    public void recordLeakedRequest() {
      leakedRequestCountSensor.record();
    }
    public void recordOtherErrorRequest() {
      otherErrorRequestCountSensor.record();
    }
  }
}
