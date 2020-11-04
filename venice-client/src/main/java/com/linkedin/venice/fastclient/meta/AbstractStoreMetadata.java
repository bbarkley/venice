package com.linkedin.venice.fastclient.meta;

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.venice.fastclient.ClientConfig;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public abstract class AbstractStoreMetadata implements StoreMetadata {
  private final InstanceHealthMonitor instanceHealthMonitor;
  private final ClientRoutingStrategy routingStrategy;

  public AbstractStoreMetadata(ClientConfig clientConfig) {
    this.instanceHealthMonitor = new InstanceHealthMonitor(clientConfig);
    this.routingStrategy = new LeastLoadedClientRoutingStrategy(this.instanceHealthMonitor);
  }

  @Override
  public List<String> getReplicas(long requestId, int version, int partitionId, int requiredReplicaCount) {
    List<String> replicas =  getReplicas(version, partitionId);

    return routingStrategy.getReplicas(requestId, replicas, requiredReplicaCount);
  }

  @Override
  public CompletableFuture<HttpStatus> sendRequestToInstance(String instance, int version, int partitionId) {
    return instanceHealthMonitor.sendRequestToInstance(instance);
  }

  @Override
  public InstanceHealthMonitor getInstanceHealthMonitor() {
    return instanceHealthMonitor;
  }
}
