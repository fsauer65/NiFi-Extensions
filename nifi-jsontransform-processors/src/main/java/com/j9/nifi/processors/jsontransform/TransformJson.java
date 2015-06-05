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
package com.j9.nifi.processors.jsontransform;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.expression.AttributeExpression;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ProcessorLog;
import org.apache.nifi.processor.*;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.stream.io.BufferedInputStream;
import org.apache.nifi.util.StopWatch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Tags({"json", "transform", "transformation", "jolt"})
@CapabilityDescription("Performs Json to Json transformations using Jolt specifications.")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class TransformJson extends AbstractProcessor {


    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Transformed Json will be sent here")
            .build();

    public static final Relationship FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Errors about failed transformations will be sent here")
            .build();

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS);
        relationships.add(FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {
        return new PropertyDescriptor.Builder()
                .name(propertyDescriptorName)
                .expressionLanguageSupported(true)
                .addValidator(StandardValidators.createAttributeExpressionLanguageValidator(AttributeExpression.ResultType.STRING, true))
                .required(false)
                .dynamic(true)
                .build();
    }

    @Override
    public Collection<ValidationResult> customValidate(ValidationContext validationContext) {
        Collection result = super.customValidate(validationContext);
        String configData = validationContext.getAnnotationData();
        if (configData == null || configData.length()==0) {
            ValidationResult emptyConfig = new ValidationResult.Builder()
                    .explanation("No transformation specified. Click advanced button to fix.")
                    .valid(false).build();
            result.add(emptyConfig);
            getLogger().warn("No transformation specified");
        } else {
            try {
                EvaluationContextEntity config = JsonUtils.stringToType(configData, EvaluationContextEntity.class);
                JsonUtils.stringToType(config.getJoltTransform(), Object.class);
            } catch (Exception x) {
                ValidationResult badConfig = new ValidationResult.Builder()
                        .explanation("Invalid transformation")
                        .input(configData)
                        .valid(false).build();
                result.add(badConfig);
                getLogger().warn("Invalid transformation");
            }
        }
        return result;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile original = session.get();
		if ( original == null ) {
			return;
		}

        final ProcessorLog logger = getLogger();
        final StopWatch stopWatch = new StopWatch(true);
        try {
            FlowFile transformed = session.write(original, new StreamCallback() {
                @Override
                public void process(final InputStream rawIn, final OutputStream out) throws IOException {
                    try (final InputStream in = new BufferedInputStream(rawIn)) {
                        // TODO this can be optimized by not doing this every time - while respecting changes
                        String configData = context.getAnnotationData();
                        EvaluationContextEntity config = JsonUtils.stringToType(configData, EvaluationContextEntity.class);
                        Chainr chain = Chainr.fromSpec(JsonUtils.stringToType(config.getJoltTransform(), Object.class));
                        // TODO end
                        Object input = JsonUtils.streamToType(in, Object.class);
                        Object transformed = chain.transform(input);
                        out.write(JsonUtils.toJsonString(transformed).getBytes());
                    } catch (final Exception e) {
                        throw new IOException(e);
                    }
                }
            });
            session.transfer(transformed, SUCCESS);
            session.getProvenanceReporter().modifyContent(transformed, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            logger.info("Transformed {}", new Object[]{original});
        } catch (ProcessException e) {
            logger.error("Unable to transform {} due to {}", new Object[]{original, e});
            session.transfer(original, FAILURE);
        }
    }

}
