package org.apache.nifi.influxdb;

import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;


@Tags({"example"})
@CapabilityDescription("Provide a description")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})

public class InfluxDBReader extends AbstractProcessor {

    public static final PropertyDescriptor INFLUXDB_SERVICE = new PropertyDescriptor.Builder()
            .name("INFLUXDB_SERVICE").displayName("InfluxDB Service")
            .description("Instance of an InfluxDB Service specifying the database configuration to use")
            .required(true)
            .identifiesControllerService(InfluxDBServiceInterface.class)
            .build();

    @Override
    public void onTrigger(ProcessContext processContext, ProcessSession processSession) throws ProcessException {

    }
}
