# NiFi-JsonTransform
This repository contains the source for a json-json transformation processor for apache NiFi based on the jolt library. 
After building copy the .nar archive from the target folder in the nifi-jsontransform-nar module to the lib folder of your NiFi installation. 
The processor (TransformJson) is configured via the advanced UI. This UI is bascially a copy of the jolt demo UI, using the CodeMirror library
to edit a sample json as well as the jolt spec. This UI allows you to experiment with your transformations before committing them to the processor config.
