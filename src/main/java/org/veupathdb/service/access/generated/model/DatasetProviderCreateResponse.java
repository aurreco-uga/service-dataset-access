package org.veupathdb.service.access.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(
    as = DatasetProviderCreateResponseImpl.class
)
public interface DatasetProviderCreateResponse {
  @JsonProperty("providerId")
  int getProviderId();

  @JsonProperty("providerId")
  void setProviderId(int providerId);
}
