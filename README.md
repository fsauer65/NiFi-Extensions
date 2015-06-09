# NiFi-JsonTransform
This repository contains the source for a json-json transformation processor for apache NiFi based on the jolt library. 
After building using maven from the root folder,  copy the .nar archive from nifi-jsontransform-nar/target to the lib folder of your NiFi installation. 

The TransformJson processor is configured entirely via the advanced UI; it has no simple properties. This UI is a copy of the jolt demo UI, which uses the awesome CodeMirror library to edit a sample json as well as the jolt spec. This UI allows you to experiment with your transformations before committing them to the processor config.
