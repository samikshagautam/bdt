# Setup instructions

## Requirements
 * Docker
 * JAVA
 * Apache HBase
 * Apache Flume

## Steps
 * clone the repository `https://github.com/samikshagautam/bdt` on your local machine
 * `cd bdt-project`
 * Create a folder `./src/resources/data` folder for `zookeeper` data directory.
 * Start `zookeeper` and `kafka` through `docker` -> `docker-compose up`
 * Wait for `zookeeper` and `kafka` to start and wait until kafka has created a topic
 * Create a folder `./flume` in the project.
 * Create a folder `./hbase` in the project.
 * Create a folder `./src/main/resources/data` in the project.
 * Download `Apache flume` https://flume.apache.org/download.html and extract the contents of the `zip` in './flume' folder
 * Copy the file `./src/main/resources/flume-conf.properties` to `./flume/conf/flume-conf.properties`.
 * Copy the file `./src/main/resoureces/dataset.csv` to `./flume/dataset.csv`. This file will be used as the source for `flume` ingestion.
 * Run the following command to start `flume` -> `sh bin/flume-ng agent --conf conf --conf-file conf/flume-conf.properties -Dflume.root.logger=INFO,console --name a1 -Xmx2048m -Xms512m`. Make sure `kafka` is already running inside docker. `flume` will read the contents of `dataset.csv` and publish it on `kafka` and wait for any changes in the file.
 * Download `hbase 2.4.6` from https://hbase.apache.org/downloads.html
 * Extract the contents of the `zip` file in `./hbase` folder.
 * Open `hbase/conf/hbase-env.sh` and uncomment `JAVA_HOME` and replace it with your actual `$JAVA_HOME` value. It can be obtained by running the following command - `/usr/libexec/java_home`
 * Uncomment `HBASE_MANAGES_ZK` and set it to `false` as `zookeeper` is already managed inside docker.
 * Replace the contents of `hbase/conf/hbase-site.xml` with `./src/resources/hbase-site.xml`. Note: In the file `./src/resources/hbase-site.xml` replace `${projectRoot}` with the actual path to project.
 * `cd hbase`
 * Start `hbase` server -> `sh bin/start-hbase.sh`
 * Open the project in `intellij idea`. After the `IDE` has configured the project, open `./src/main/java/org/bdt/project/SparkStreaming.java` and execute its `main` function through the `IDE`.
 * The `script` in this file connects with `kafka` and `hbase` and starts a streaming context. It receives any notification from `kafka` on the subscribed topic (`NotificationTopic` for our project) and as soon as it receives any notification, it inserts the data into the `hbase` database.

## References
* https://medium.com/@shripatibhat/install-hbase-on-mac-in-5-minutes-8501b6936e9e
* https://sakai.cs.miu.edu/access/content/attachment/6cf737ae-6d6f-4af7-9984-3220b8c145fb/Assignments/cc0e4cff-d0b5-468c-a342-6c10d4f04929/MyFirstHbaseTable.java
* https://medium.com/@thomaspt748/how-to-create-spark-dataframe-on-hbase-table-e9c8db31bb30
* https://sparkbyexamples.com/spark/spark-read-write-using-hbase-spark-connector/
* 