# Prometheus Scraping Extension for AppDynamics

## Use Case
Prometheus is an open-source monitoring solution for collecting and aggregating metrics as time series data.
Programs designed to provide metrics to Prometheus (called "exporters") listen for HTTP requests and emit
metrics. Prometheus periodically connects to the endpoint and scrapes the metrics.

This extension will scrape one or more Prometheus endpoints and ingest selected metrics into AppDynamics.

## Prerequisites
1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
2. Download and install [Apache Maven](https://maven.apache.org/) which is configured with `Java 8` to build the extension artifact from source. You can check the java version used in maven using command `mvn -v` or `mvn --version`. If your maven is using some other java version then please download java 8 for your platform and set JAVA_HOME parameter before starting maven.
3. This extension creates a java client to the Prometheus server that needs to be monitored. So the Prometheus server should be reachable from the machine that has the extension installed.

## Installation
1. Clone the "prometheus-scraping-extension" repo using `git clone <repoUrl>` command.
2. Run 'mvn clean install' from "prometheus-scraping-extension".
3. Unzip the `PromScraper-<version>.zip` from `target` directory into the "<MachineAgent_Dir>/monitors" directory.
4. Edit the file config.yml located at <MachineAgent_Dir>/monitors/PromScraper referring to configurations section below.
5. Restart the Machine Agent.

## Recommendations
It is recommended that a single Prometheus monitoring extension be used to monitor multiple Prometheus servers belonging to a single cluster.

## Configuring the extension using config.yml
Configure the Prometheus monitoring extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/PrometheusMonitor/`

  1. Configure the "tier" under which the metrics need to be reported. This can be done by changing the value of `<TIER NAME OR TIER ID>` in
     metricPrefix: "Server|Component:`<TIER NAME OR TIER ID>`|Custom Metrics|Prometheus".

     For example,
     ```
     metricPrefix: "Server|Component:Extensions tier|Custom Metrics|Prometheus"
     ```
More details around metric prefix can be found [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695)

  2. Configure the Prometheus instances by specifying the name(required), host(required), port(required) of the Prometheus instance, password (only if authentication enabled),
     encryptedPassword(only if password encryption required).

     For example,
     ```
      #Add your list of Prometheus servers here.
      servers:
        - name: "Server1"
          host: "localhost"
          port: "6379"
          password: ""
          encryptedPassword: ""
        - name: "Server2"
          host: "localhost"
          port: "6380"
          password: ""
          encryptedPassword: ""
     ```

  3. Configure the encyptionKey for encryptionPasswords(only if password encryption required).

     For example,
     ```
     #Encryption key for Encrypted password.
     encryptionKey: "axcdde43535hdhdgfiniyy576"
     ```

  4. Configure the numberOfThreads(only if the number of Prometheus servers need to be monitored is greater than 7).

     For example,

     If number Prometheus servers that need to be monitored is 10, then number of threads required is 10 * 3 = 30
     ```
     numberOfThreads: 30
     ```

  5. Configure the metrics section.

     For configuring the metrics, the following properties can be used:

     |     Property      |   Default value |         Possible values         |                                              Description                                                                                                |
     | :---------------- | :-------------- | :------------------------------ | :------------------------------------------------------------------------------------------------------------- |
     | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
     | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)    |
     | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)   |
     | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)|
     | multiplier        | 1               | Any number                      | Value with which the metric needs to be multiplied.                                                            |
     | convert           | null            | Any key value map               | Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:0, DOWN:1  |
     | delta             | false           | true, false                     | If enabled, gives the delta values of metrics instead of actual values.                                        |

     For example,
     ```
     - total_connections_received:  #Total number of connections accepted by the server
         alias: "connectionsReceived"
         multiplier: 1
         aggregationType: "SUM"
         timeRollUpType: "CURRENT"
         clusterRollUpType: "INDIVIDUAL"
         delta: true
     - role:  #Role of Prometheus server(master or slave)
         convert:
           master: 1
           slave: 0
     ```
     **All these metric properties are optional, and the default value shown in the table is applied to the metric(if a property has not been specified) by default.**


## Credential Encryption

Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench

Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

## Troubleshooting

Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.

## Contributing

Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/tradel/prometheus-monitoring-extension).

## Version

|                          |           |
|--------------------------|-----------|
|Current version           |1.0.0      |
|Last Update               |02/29/2024 |
|Changes list              |[ChangeLog](https://github.com/tradel/prometheus-monitoring-extension/blob/main/CHANGELOG.md)|

