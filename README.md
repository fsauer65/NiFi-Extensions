# [NiFi](http://nifi.incubator.apache.org) Extensions

A number of generic [NiFi](http://nifi.incubator.apache.org) extension Modules

## NiFi-JsonTransform
A json-json transformation processor for apache [NiFi](http://nifi.incubator.apache.org) based on the [jolt](http://bazaarvoice.github.io/jolt/) library. 
After building using maven from the root folder,  copy the .nar archive from nifi-jsontransform-nar/target to the lib folder of your [NiFi](http://nifi.incubator.apache.org) installation. 

The TransformJson processor is configured entirely via the advanced UI; it has no simple properties. This UI is a copy of the jolt demo UI, which uses the awesome CodeMirror library to edit a sample json as well as the jolt spec. This UI allows you to experiment with your transformations before committing them to the processor config.

## NiFi-InfluxDB

An [InfluxDB](https://influxdb.com) service, source and target. The [InfluxDB](https://influxdb.com) source can be configured to issue queries to an [InfluxDB](https://influxdb.com) time-series database made available via the [InfluxDB](https://influxdb.com) service. The target writes date from either a JSON flow file or attributes. The [InfluxDB](https://influxdb.com) service defines common configuration such as database host, port and name as well as credentials. This is based and dependent on the recently released 0.9 version of [InfluxDB](https://influxdb.com). See processor usage from the NiFi UI for more details. To install, build this bundle using maven and drop the nar archive from nifi-influxdb-nar/target into the lib folder of your [NiFi](http://nifi.incubator.apache.org) installation.
