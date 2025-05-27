package com.fixedincome.marketdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FredApiResponse {
    
  @JsonProperty("realtime_start")
  private String realtimeStart;
  
  @JsonProperty("realtime_end")
  private String realtimeEnd;
  
  @JsonProperty("observation_start")
  private String observationStart;
  
  @JsonProperty("observation_end")
  private String observationEnd;
  
  private String units;
  
  @JsonProperty("output_type")
  private int outputType;
  
  @JsonProperty("file_type")
  private String fileType;
  
  @JsonProperty("order_by")
  private String orderBy;
  
  @JsonProperty("sort_order")
  private String sortOrder;
  
  private int count;
  
  private int offset;
  
  private int limit;
  
  private List<FredObservation> observations;
  
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class FredObservation {
      
    @JsonProperty("realtime_start")
    private String realtimeStart;
    
    @JsonProperty("realtime_end")
    private String realtimeEnd;
    
    private String date;
    
    private String value;
  }
}
