package com.lsx.system.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.List;

@FeignClient(name = "property-service", path = "/api")
public interface PropertyServiceClient {

    @GetMapping("/complaint/stats")
    Map<String, Object> getComplaintStats();

    @GetMapping("/complaint/stats/type")
    List<Map<String, Object>> getComplaintTypeStats();

    @GetMapping("/repair/stats")
    Map<String, Object> getRepairStats();
    
    @GetMapping("/repair/stats/trend")
    List<Map<String, Object>> getRepairTrend();

    @GetMapping("/visitor/stats")
    Map<String, Object> getVisitorStats();
}
