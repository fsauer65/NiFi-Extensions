# NiFi Extensions

A number of generic NiFi extension Modules

## NiFi-JsonTransform
A json-json transformation processor for apache NiFi based on the jolt library. 
After building using maven from the root folder,  copy the .nar archive from nifi-jsontransform-nar/target to the lib folder of your NiFi installation. 

The TransformJson processor is configured entirely via the advanced UI; it has no simple properties. This UI is a copy of the jolt demo UI, which uses the awesome CodeMirror library to edit a sample json as well as the jolt spec. This UI allows you to experiment with your transformations before committing them to the processor config.

## NiFi-InfluxDB

An InfluxDB service, source and target. The InfluxDB source can be configured to issue queries to an InfluxDB time-series database made available via the InfluxDB service. The target writes date from either a JSON flow file or attributes. The InfluxDB service defines common configuration such as database hsot port and name as well as credentials.
