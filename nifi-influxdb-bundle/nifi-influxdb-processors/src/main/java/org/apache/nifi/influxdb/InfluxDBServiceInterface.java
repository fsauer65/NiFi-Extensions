package org.apache.nifi.influxdb;

import org.apache.nifi.controller.ControllerService;

import java.util.Map;

public interface InfluxDBServiceInterface extends ControllerService {

    void write(String measurement, double value, Map<String,String> tags);
    void write(String measurement, double value, Map<String,String> tags, long timestamp);

}
