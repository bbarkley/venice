package com.linkedin.venice.controllerapi;

import com.linkedin.venice.meta.Version;


@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.codehaus.jackson.annotate.JsonIgnoreProperties(ignoreUnknown = true)
public class RepushInfo {
  private String kafkaBrokerUrl;
  private Version version;

  public static RepushInfo createRepushInfo(Version version, String kafkaBrokerUrl) {
    RepushInfo repushInfo = new RepushInfo();
    repushInfo.setVersion(version);
    repushInfo.setKafkaBrokerUrl(kafkaBrokerUrl);
    return repushInfo;
  }

  public void setVersion(Version version) {
    this.version = version;
  }

  public void setKafkaBrokerUrl(String kafkaBrokerUrl) {
    this.kafkaBrokerUrl = kafkaBrokerUrl;
  }

  public String getKafkaBrokerUrl() {
    return this.kafkaBrokerUrl;
  }

  public Version getVersion() {
    return version;
  }
}
