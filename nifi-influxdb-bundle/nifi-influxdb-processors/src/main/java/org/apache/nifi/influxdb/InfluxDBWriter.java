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
import org.apache.nifi.util.StopWatch;

import java.util.*;
import java.util.concurrent.TimeUnit;


@Tags({"InfluxDB", "metrics", "time-series"})
@CapabilityDescription("Writes measurements to an InfluxDB time-series using an InfluxDBService instance.")
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

/*  From flow-file content Not yet supported
    public static final PropertyDescriptor FROM_ATTRIBUTES = new PropertyDescriptor.Builder()
            .name("FROM_ATTRIBUTES").displayName("Get data from attributes")
            .description("Whether to get data from attributes of content of the flow file. See InfluxDB.com for format in the latter case")
            .required(true)
            .defaultValue("true")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
*/

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
        final ProcessorLog logger = getLogger();
        try {
            InfluxDBServiceInterface influxdb = processContext.getProperty(INFLUXDB_SERVICE).asControllerService(InfluxDBServiceInterface.class);
            Map<String, String> atts = original.getAttributes();
            String measurement = atts.get("influxdb.measurement");
            String value = atts.get("influxdb.value");
            if (measurement != null && value != null) {
                double dblValue = Double.valueOf(value);
                Map<String,String> tags = new HashMap<>();
                for (String key : atts.keySet()) {
                    if (key.startsWith("influxdb.tag.")) {
                        String tagName = key.substring("influxdb.tag.".length());
                        String tagValue = atts.get(key);
                        tags.put(tagName,tagValue);
                    }
                }
                influxdb.write(measurement,dblValue,tags);
            }
        } catch (Exception x) {
            logger.error("Error writing influxdb measurement",x);
        }

    }
}
