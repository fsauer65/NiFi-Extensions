package org.apache.nifi.influxdb;

import org.apache.nifi.controller.ControllerService;


import java.util.List;
import java.util.Map;

public interface InfluxDBServiceInterface extends ControllerService {

    void write(String measurement, double value, Map<String,String> tags);
    void write(String measurement, double value, Map<String,String> tags, long timestamp);

    Results query(String query, boolean pretty);

    class Series {
        String name;
        Map<String,String> tags;
        List<String> columns;
        List<List<?>> values;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }

        public List<String> getColumns() {
            return columns;
        }

        public void setColumns(List<String> columns) {
            this.columns = columns;
        }

        public List<List<?>> getValues() {
            return values;
        }

        public void setValues(List<List<?>> values) {
            this.values = values;
        }
    }

    class ResultSeries {
        List<Series> series;
        String error;

        public List<Series> getSeries() {
            return series;
        }

        public void setSeries(List<Series> series) {
            this.series = series;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    class Results {
        List<ResultSeries> results;

        public List<ResultSeries> getResults() {
            return results;
        }

        public void setResults(List<ResultSeries> results) {
            this.results = results;
        }
    }
}
