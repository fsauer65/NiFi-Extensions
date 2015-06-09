package org.apache.nifi.processors.jsontransform.api;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import org.apache.nifi.processors.jsontransform.model.EvaluationContextEntity;
import org.apache.nifi.web.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("transformations")
public class TransformationResource {

    private static final Logger logger = LoggerFactory.getLogger(TransformationResource.class);

    @Context
    private ServletContext servletContext;

    @Context
    private HttpServletRequest request;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/evaluation-context")
    public Response getEvaluationContext(@QueryParam("processorId") final String processorId) {

        // get the web context
        final NiFiWebConfigurationContext nifiWebContext = (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");

        // build the web context config
        final NiFiWebRequestContext contextConfig = getRequestContext(processorId);

        final ComponentDetails processorDetails;

        try {
            // load the processor configuration
            processorDetails = nifiWebContext.getComponentDetails(contextConfig);
        } catch (final InvalidRevisionException ire) {
            throw new WebApplicationException(invalidRevision(ire.getMessage()));
        } catch (final Exception e) {
            final String message = String.format("Unable to get TransformJson[id=%s] transformation spec: %s", contextConfig.getId(), e);
            logger.error(message, e);
            throw new WebApplicationException(error(message));
        }
        // create the response entity
        String sampleAndSpec = processorDetails.getAnnotationData();
        EvaluationContextEntity responseEntity = null;
        if (sampleAndSpec != null && sampleAndSpec.length()>0) {
            responseEntity = JsonUtils.stringToType(sampleAndSpec, EvaluationContextEntity.class);
        } else {
            responseEntity = new EvaluationContextEntity();
            responseEntity.setProcessorId(processorId);
            // set some sample input and transform just to get a first time user going with this...
            responseEntity.setSampleInput("{\n" +
                    "\t\"ratings\": {\n" +
                    "\t\t\"primary\": 5,\n" +
                    "\t\t\"quality\": 4\n" +
                    "\t}\n" +
                    "}");
            responseEntity.setJoltTransform("[\n" +
                    "\t{\n" +
                    "\t\t\"operation\": \"shift\",\n" +
                    "\t\t\"spec\": {\n" +
                    "\t\t\t\"ratings\": {\n" +
                    "\t\t\t\t\"*\": {\n" +
                    "\t\t\t\t\t\"$\": \"Ratings[#2].Name\",\n" +
                    "\t\t\t\t\t\"@\": \"Ratings[#2].Value\"\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t}\n" +
                    "\t\t}\n" +
                    "\t}\n" +
                    "]");
        }
        // generate the response
        final Response.ResponseBuilder response = Response.ok(responseEntity);
        return noCache(response).build();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/evaluation-context")
    public Response updateEvaluationContext(
            @Context final UriInfo uriInfo,
            final EvaluationContextEntity requestEntity) {

        // get the web context
        final NiFiWebConfigurationContext configurationContext = (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");

        // ensure the evaluation context has been specified
        if (requestEntity == null) {
            throw new WebApplicationException(badRequest("The evaluation context must be specified."));
        }

         // build the web context config
        final NiFiWebConfigurationRequestContext requestContext = getConfigurationRequestContext(
                requestEntity.getProcessorId(), requestEntity.getRevision(), requestEntity.getClientId());

        saveTransformConfig(requestContext, requestEntity);

        // create the response entity
        final EvaluationContextEntity responseEntity = new EvaluationContextEntity();
        responseEntity.setClientId(requestEntity.getClientId());
        responseEntity.setRevision(requestEntity.getRevision());
        responseEntity.setProcessorId(requestEntity.getProcessorId());
        responseEntity.setJoltTransform(requestEntity.getJoltTransform());
        responseEntity.setSampleInput(requestEntity.getSampleInput());
        // generate the response
        final Response.ResponseBuilder response = Response.ok(responseEntity);
        return noCache(response).build();
    }

    private void saveTransformConfig(final NiFiWebConfigurationRequestContext requestContext, final EvaluationContextEntity config) {
        // serialize the criteria
        final String annotationData = JsonUtils.toPrettyJsonString(config);

        // get the web context
        final NiFiWebConfigurationContext configurationContext = (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");

        try {
            // save the annotation data
            configurationContext.setAnnotationData(requestContext, annotationData);
        } catch (final InvalidRevisionException ire) {
            throw new WebApplicationException(invalidRevision(ire.getMessage()));
        } catch (final Exception e) {
            final String message = String.format("Unable to save TransformJson[id=%s] configuration: %s", requestContext.getId(), e);
            logger.error(message, e);
            throw new WebApplicationException(error(message));
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_PLAIN})
    @Path("/transform")
    public Response transform (@Context final UriInfo uriInfo,
                               @FormParam("input") final String sample,
                               @FormParam("spec") final String spec) {
        Chainr chain = Chainr.fromSpec(JsonUtils.stringToType(spec, Object.class));
        Object input = JsonUtils.stringToType(sample, Object.class);
        try {
            Object transformed = chain.transform(input);
            // generate the response
            String json = JsonUtils.toPrettyJsonString(transformed);
            final Response.ResponseBuilder response = Response.ok(json);
            return noCache(response).build();
        } catch (Exception x) {
            logger.error("Failed jolt transform", x);
            return error(x.getMessage());
        }
    }

    private Response.ResponseBuilder noCache(Response.ResponseBuilder response) {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setPrivate(true);
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        return response.cacheControl(cacheControl);
    }

    private NiFiWebRequestContext getRequestContext(final String processorId) {
        return new HttpServletRequestContext(UiExtensionType.ProcessorConfiguration, request) {
            @Override
            public String getId() {
                return processorId;
            }
        };
    }

    private NiFiWebConfigurationRequestContext getConfigurationRequestContext(final String processorId, final Long revision, final String clientId) {
        return new HttpServletConfigurationRequestContext(UiExtensionType.ProcessorConfiguration, request) {
            @Override
            public String getId() {
                return processorId;
            }

            @Override
            public Revision getRevision() {
                return new Revision(revision, clientId);
            }
        };
    }

    private Response badRequest(final String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(message).type("text/plain").build();
    }

    private Response invalidRevision(final String message) {
        return Response.status(Response.Status.CONFLICT).entity(message).type("text/plain").build();
    }

    private Response error(final String message) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(message).type("text/plain").build();
    }
}
