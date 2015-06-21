package org.apache.nifi.influxdb;

import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ProcessorLog;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.stream.io.BufferedInputStream;

import java.io.*;
import java.util.*;


@Tags({"InfluxDB", "metrics", "time-series"})
@CapabilityDescription("Writes measurements to an InfluxDB (v. 0.9) time-series using an InfluxDBService instance.")
@SeeAlso({InfluxDBService.class})
@ReadsAttributes({
        @ReadsAttribute(attribute="influxdb.measurement", description="Name of the measurement (time-series)"),
        @ReadsAttribute(attribute="influxdb.value", description="Value of the measurement"),
        @ReadsAttribute(attribute="influxdb.tag.<tagname>", description="Additional tags to write with the measurement"),
        @ReadsAttribute(attribute="influxdb.timestamp", description="Optional timestamp for the measurement")
})

public class InfluxDBWriter extends AbstractProcessor {

    public static final PropertyDescriptor INFLUXDB_SERVICE = new PropertyDescriptor.Builder()
            .name("INFLUXDB_SERVICE").displayName("InfluxDB Service")
            .description("Instance of an InfluxDB Service specifying the database configuration to use")
            .required(true)
            .identifiesControllerService(InfluxDBServiceInterface.class)
            .build();

    public static final PropertyDescriptor FROM_ATTRIBUTES = new PropertyDescriptor.Builder()
            .name("FROM_ATTRIBUTES").displayName("Get data from attributes")
            .description("Whether to get data from attributes of content of the flow file. See InfluxDB.com for format in the latter case")
            .required(true)
            .defaultValue("true")
            .allowableValues("true","false")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();

    public static final PropertyDescriptor BATCH_SIZE = new PropertyDescriptor.Builder()
            .name("BATCH_SIZE").displayName("Measurement Batch Size")
            .description("Only applicable to Flowfile content processing; how many measurements to send in a single call to InfluxDB")
            .required(false)
            .defaultValue("1")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    public static final Relationship ORIGINAL = new Relationship.Builder()
            .name("original")
            .description("Original flow file is simply forwarded here when writing succeeded.")
            .build();

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;


    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(INFLUXDB_SERVICE);
        descriptors.add(FROM_ATTRIBUTES);
        descriptors.add(BATCH_SIZE);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(ORIGINAL);
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

        final FlowFile original = processSession.get();
        if ( original == null ) {
            return;
        }
        processSession.transfer(original, ORIGINAL);
        boolean fromAttributes = Boolean.valueOf(processContext.getProperty(FROM_ATTRIBUTES).getValue());
        InfluxDBServiceInterface influxdb = processContext.getProperty(INFLUXDB_SERVICE).asControllerService(InfluxDBServiceInterface.class);
        if (fromAttributes)
            processAttributes(influxdb, original.getAttributes());
        else {
            int batchSize = Integer.valueOf(processContext.getProperty(BATCH_SIZE).getValue());
            processFlowfileContent(influxdb, processSession, original, batchSize);
        }
    }

    /**
     * Write a single measurement taken from flowfile attributes, see documentation (annotations) for attribute names
     * @param influxdb
     * @param atts
     */
    private void processAttributes(final InfluxDBServiceInterface influxdb, final Map<String, String> atts) {
        final ProcessorLog logger = getLogger();
        try {
            String measurement = atts.get("influxdb.measurement");
            String value = atts.get("influxdb.value");
            if (measurement != null && value != null) {
                double dblValue = Double.valueOf(value);
                Map<String, String> tags = new HashMap<>();
                for (String key : atts.keySet()) {
                    if (key.startsWith("influxdb.tag.")) {
                        String tagName = key.substring("influxdb.tag.".length());
                        String tagValue = atts.get(key);
                        tags.put(tagName, tagValue);
                    }
                }
                influxdb.write(measurement, dblValue, tags);
            }
        } catch (Exception x) {
            logger.error("Error writing influxdb measurement", x);
        }
    }

    /**
     * Write potentially many measurements taken from the flowfile content, where each line has this format:
     * measurement_name, {tag_name=tag_value,}* measurement_value=someValue [timestamp]
     * @param influxdb
     * @param in
     */
    private void processFlowfileContent(final InfluxDBServiceInterface influxdb, final ProcessSession processSession,
                                        final FlowFile in, final int batchSize) {
        FlowFile out = processSession.write(in, new StreamCallback() {
            @Override
            public void process(final InputStream rawIn, final OutputStream out) throws IOException {
                try (final InputStream in = new BufferedInputStream(rawIn)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line = null;
                    List<String> buffer = new ArrayList<>();
                    while ((line = reader.readLine()) != null) {
                        out.write(line.getBytes());
                        buffer.add(line);
                        if (buffer.size() == batchSize) {
                            influxdb.write(buffer);
                            buffer.clear();
                        }
                    }
                    // write last partial batch
                    if (buffer.size() > 0) {
                        influxdb.write(buffer);
                    }
                } catch (final Exception e) {
                    throw new IOException(e);
                }
            }
        });
        processSession.transfer(out, ORIGINAL);
    }
}
