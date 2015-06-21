package org.apache.nifi.influxdb;

import com.bazaarvoice.jolt.JsonUtils;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Tags({"InfluxDB", "metrics", "time-series"})
@CapabilityDescription("Manages connections to an InfluxDB (v. 0.9) time-series database")
@SeeAlso({InfluxDBReader.class, InfluxDBWriter.class})
public class InfluxDBService extends AbstractControllerService implements InfluxDBServiceInterface {

    enum Precision {
        Nanos   {String paramValue() {return "n";}},
        Micros  {String paramValue() {return "u";}},
        Millis  {String paramValue() {return "ms";}},
        Seconds {String paramValue() {return "s";}},
        Minutes {String paramValue() {return "m";}},
        Hours   {String paramValue() {return "h";}};

        abstract String paramValue();
    }

    private static final Logger logger = LoggerFactory.getLogger(InfluxDBService.class);

    public static final PropertyDescriptor INFLUXDB_HOST = new PropertyDescriptor.Builder()
            .name("INFLUXDB_HOST").displayName("InfluxDB Host")
            .description("Hostname of the InfluxDB database server")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor INFLUXDB_PORT = new PropertyDescriptor.Builder()
            .name("INFLUXDB_PORT").displayName("InfluxDB port")
            .description("Port of the InfluxDB database server")
            .required(true)
            .defaultValue("8086")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    public static final PropertyDescriptor CONNECTION_TIMEOUT = new PropertyDescriptor.Builder()
            .name("CONNECTION_TIMEOUT").displayName("Connection Timeout")
            .description("How long to wait when attempting to connect to InfluxDB before giving up")
            .required(true)
            .defaultValue("30 sec")
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
            .build();

    public static final PropertyDescriptor USERNAME = new PropertyDescriptor.Builder()
            .name("USERNAME").displayName("Username")
            .description("DB Username")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor PASSWORD = new PropertyDescriptor.Builder()
            .name("PASSWORD").displayName("Password")
            .description("DB Password")
            .required(true)
            .sensitive(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor DATABASE_NAME = new PropertyDescriptor.Builder()
            .name("DATABASE_NAME").displayName("Database Name")
            .description("Name of the time-series database")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor RETENTION_POLICY = new PropertyDescriptor.Builder()
            .name("RETENTION_POLICY").displayName("Retention Policy")
            .description("Name of an existing influxdb retention policy [Optional]")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PRECISION = new PropertyDescriptor.Builder()
            .name("PRECISION").displayName("Timestamp precision")
            .description("[optional] precision of timestamps. default is nanoseconds")
            .required(false)
            .allowableValues(Precision.values())
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private static final List<PropertyDescriptor> properties;

    static {
        List<PropertyDescriptor> props = new ArrayList<>();
        props.add(INFLUXDB_HOST);
        props.add(INFLUXDB_PORT);
        props.add(CONNECTION_TIMEOUT);
        props.add(USERNAME);
        props.add(PASSWORD);
        props.add(DATABASE_NAME);
        props.add(RETENTION_POLICY);
        props.add(PRECISION);
        properties = Collections.unmodifiableList(props);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @Override
    public String toString() {
        return "InfluxDBService[id=" + getIdentifier() + "]";
    }

    @OnEnabled
    public void init(final ConfigurationContext context) throws InitializationException {
        // test the connection
        try {
            Results measurements = query("SHOW MEASUREMENTS", false);
            logger.debug("Connect test returned " + measurements);
        } catch (Exception x) {
            throw new InitializationException("Could not connect to InfluxDB:", x);
        }
    }

    private String tagsToString(Map<String,String> tags) {
        StringBuilder b = new StringBuilder();
        if (tags == null || tags.isEmpty()) return "";
        boolean first = true;
        for (Map.Entry<String,String> next : tags.entrySet()) {
            if (!first)
                b.append(',');
            else
                first = false;
            b.append(next.getKey()).append('=').append(next.getValue());

        }
        return b.toString();
    }

    @Override
    public void write(String measurement, double value, Map<String, String> tags) {
        StringBuilder b = new StringBuilder();
        b.append(measurement).append(',').append(tagsToString(tags)).append(" value=").append(value);
        post(b.toString());
    }

    @Override
    public void write(String measurement, double value, Map<String, String> tags, long timestamp) {
        StringBuilder b = new StringBuilder();
        b.append(measurement).append(',').append(tagsToString(tags)).append(" value=").append(value);
        post(b.toString());
    }

    @Override
    public void write(List<String> batch) {

    }

    private Client createClient() {
        ClientConfig cc = new DefaultClientConfig();
        cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
        cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, getProperty(CONNECTION_TIMEOUT).asTimePeriod(TimeUnit.MILLISECONDS).intValue());
        Client c = Client.create(cc);

        HTTPBasicAuthFilter auth = new HTTPBasicAuthFilter(
                getProperty(USERNAME).getValue(),
                getProperty(PASSWORD).getValue());
        c.addFilter(auth);
        return c;
    }

    public Results query(String query, boolean pretty) {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("db", getProperty(DATABASE_NAME).getValue());
        if (pretty) {
            params.add("pretty", "true");
        }
        params.add("q", query);
        String result = createClient().resource(base().append("/query").toString()).queryParams(params).get(String.class);
        return JsonUtils.stringToType(result, Results.class);
    }

    private void post(String data) {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("db", getProperty(DATABASE_NAME).getValue());
        if (getProperty(RETENTION_POLICY).isSet()) {
            params.add("rp", getProperty(RETENTION_POLICY).getValue());
        }
        if (getProperty(PRECISION).isSet()) {
            String p = Precision.valueOf(getProperty(PRECISION).getValue()).paramValue();
            if (!"n".equals(p)) params.add("precision",p);
        }
        try {
            createClient().resource(base().append("/write").toString()).queryParams(params).post(data);
        } catch (UniformInterfaceException x) {
            logger.error("Error writing '" + data + "'", x.getResponse().getEntity(String.class));
            if (x.getResponse().getStatus()< 500)
                throw new IllegalArgumentException("Bad data:" + data, x);
            else
                throw new RuntimeException("Error writing data " + data, x);
        }
    }

    private StringBuffer base() {
        StringBuffer b = new StringBuffer("http://");
        b.append(getProperty(INFLUXDB_HOST).getValue())
                .append(":")
                .append(getProperty(INFLUXDB_PORT).getValue());
        return b;
    }
}
