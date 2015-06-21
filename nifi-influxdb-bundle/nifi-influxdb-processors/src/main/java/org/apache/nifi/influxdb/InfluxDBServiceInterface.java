package org.apache.nifi.influxdb;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.apache.nifi.controller.ControllerService;


import java.util.List;
import java.util.Map;

public interface InfluxDBServiceInterface extends ControllerService {

    void write(String measurement, double value, Map<String,String> tags);
    void write(String measurement, double value, Map<String,String> tags, long timestamp);

    void write(List<String> batch); // TODO assumed to have correct format - bad but speedy

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
