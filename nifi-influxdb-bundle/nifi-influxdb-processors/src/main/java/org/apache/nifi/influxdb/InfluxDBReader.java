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
import com.bazaarvoice.jolt.JsonUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.logging.ProcessorLog;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.stream.io.BufferedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Tags({"influxdb", "time-series", "query"})
@CapabilityDescription("Issues queries to an InfluxDB (v. 0.9) time-series database configured by InfluxDBService")
@SeeAlso({InfluxDBService.class, InfluxDBWriter.class})

public class InfluxDBReader extends AbstractProcessor {

    public static final PropertyDescriptor INFLUXDB_SERVICE = new PropertyDescriptor.Builder()
            .name("INFLUXDB_SERVICE").displayName("InfluxDB Service")
            .description("Instance of an InfluxDB Service specifying the database configuration to use")
            .required(true)
            .identifiesControllerService(InfluxDBServiceInterface.class)
            .build();

    public static final PropertyDescriptor QUERY = new PropertyDescriptor.Builder()
            .name("QUERY").displayName("InfluxDB Query")
            .description("Query to submit to InfluxDB when triggered")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor FILENAME = new PropertyDescriptor.Builder()
            .name("FILENAME").displayName("FIlename")
            .description("Resulting Flowfile name")
            .required(true)
            .defaultValue("InfluxDBQueryResult.json")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PRETTY = new PropertyDescriptor.Builder()
            .name("PRETTY").displayName("Pretty print results")
            .description("Whether to get query result data in pretty-printed json or an optimized unformatted single line")
            .required(true)
            .defaultValue("false")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Query results are sent here in json format.")
            .build();

    public static final Relationship FAILURE = new Relationship.Builder()
            .name("failure")
            .description("failures (exceptions) are sent here.")
            .build();

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;


    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(INFLUXDB_SERVICE);
        descriptors.add(QUERY);
        descriptors.add(FILENAME);
        descriptors.add(PRETTY);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS);
        relationships.add(FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public void onTrigger(ProcessContext processContext, ProcessSession processSession) throws ProcessException {
        final ProcessorLog logger = getLogger();
        try {
            final InfluxDBServiceInterface influxdb = processContext.getProperty(INFLUXDB_SERVICE).asControllerService(InfluxDBServiceInterface.class);
            final Boolean pretty = Boolean.valueOf(processContext.getProperty(PRETTY).getValue());
            InfluxDBServiceInterface.Results results = influxdb.query(processContext.getProperty(QUERY).getValue(), pretty);
            for (final InfluxDBServiceInterface.ResultSeries rs : results.results) {
                FlowFile out = processSession.create();
                out = processSession.putAttribute(out, CoreAttributes.FILENAME.key(), processContext.getProperty(FILENAME).getValue());
                // TODO the amount of boiler plate here is ludicrous ... how about some java 8 lambdas or better yet: a Scala API?
                // TODO the only line that actually matters is the one (1) line inside the try catch
                if (rs.error != null) {
                    // write error to failure
                    out = processSession.write(out, new StreamCallback() {
                        @Override
                        public void process(final InputStream rawIn, final OutputStream out) throws IOException {
                            try (final InputStream in = new BufferedInputStream(rawIn)) {
                                out.write(rs.error.getBytes());
                            } catch (final Exception e) {
                                throw new IOException(e);
                            }
                        }
                    });
                    processSession.transfer(out, FAILURE);
                } else {
                    // write query result to success
                    out = processSession.putAttribute(out, "content-type", "application/json");
                    out = processSession.write(out, new StreamCallback() {
                        @Override
                        public void process(final InputStream rawIn, final OutputStream out) throws IOException {
                            try {
                                if (pretty)
                                    out.write(JsonUtils.toPrettyJsonString(rs).getBytes());
                                else
                                    out.write(JsonUtils.toJsonString(rs).getBytes());
                            } catch (final Exception e) {
                                throw new IOException(e);
                            }
                        }
                    });
                    processSession.transfer(out, SUCCESS);
                }
            }
        } catch (Exception x) {
            throw new ProcessException("Error querying influxdb",x);
        }

    }
}
