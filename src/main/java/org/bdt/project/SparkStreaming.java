package org.bdt.project;

import kafka.serializer.StringDecoder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import scala.Tuple2;

import java.io.IOException;
import java.util.*;

public class SparkStreaming {
    public static SparkContext sc;

    public static void main(String[] args) throws InterruptedException, IOException {
        String brokers = "localhost:9094";
        String topics = "NotificationTopic";
        String fullTableName = "bdtProject";
        String colFamily = "sentimentRecord";

        Configuration conf = HBaseConfiguration.create();
        conf.set("zookeeper.znode.parent", "/hbase");
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set("hbase.master", "localhost:16000");

        SparkConf sparkConf = new SparkConf();
        sparkConf.setMaster("local[2]");
        sparkConf.setAppName("SparkStreaming");

        JavaStreamingContext jssc = new JavaStreamingContext(
                sparkConf,
                Durations.seconds(10));

        Set<String> topicsSet = new HashSet<>(
                Arrays.asList(topics.split(",")));
        Map<String, String> kafkaParams = new HashMap<>();
        kafkaParams.put("metadata.broker.list", brokers);

        JavaPairInputDStream<String, String> messages =
                KafkaUtils.createDirectStream(
                        jssc,
                        String.class,
                        String.class,
                        StringDecoder.class,
                        StringDecoder.class,
                        kafkaParams,
                        topicsSet
                );

        JavaDStream<String> lines = messages.map((Function<Tuple2<String, String>, String>) Tuple2::_2);

        JavaPairDStream<String, Integer> streamedRecords = lines
                .mapToPair(
                        (PairFunction<String, String, Integer>) s ->
                                new Tuple2<>(s, 1)).reduceByKey(
                        (Function2<Integer, Integer, Integer>)
                                Integer::sum);
        sc = jssc.sparkContext().sc();

        try (Connection connection = ConnectionFactory.createConnection(conf)) {
            TableName tableName = TableName.valueOf(fullTableName);
            TableDescriptor tableDescriptor = TableDescriptorBuilder.newBuilder(tableName)
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(colFamily.getBytes()).build())
                    .build();

            Admin admin = connection.getAdmin();
            if (!admin.tableExists(tableDescriptor.getTableName())) {
                admin.createTable(tableDescriptor);
            }

            Table table = connection.getTable(tableDescriptor.getTableName());
            List<Put> insert = new ArrayList<>();
            streamedRecords.foreachRDD((v1, v2) -> {
                v1.foreach((x) -> {
                    String str = x._1;
                    String[] cells = str.split(",");
                    int type = Integer.parseInt(cells[0].replaceAll("[^\\d]", ""));
                    long tweetId = Long.parseLong(cells[1].replaceAll("[^\\d]", ""));
                    String date = cells[2];
                    String tweet = cells[5];

                    Put put = new Put(Bytes.toBytes("r1"));
                    put.addColumn(Bytes.toBytes(colFamily), Bytes.toBytes("sentiment"), Bytes.toBytes(type));
                    put.addColumn(Bytes.toBytes(colFamily), Bytes.toBytes("tweetId"), Bytes.toBytes(tweetId));
                    put.addColumn(Bytes.toBytes(colFamily), Bytes.toBytes("date"), Bytes.toBytes(date));
                    put.addColumn(Bytes.toBytes(colFamily), Bytes.toBytes("tweet"), Bytes.toBytes(tweet));

                    insert.add(put);

                    String addedRow = "(sentiment=" + type + ", tweetId=" + tweetId + ", date=" + date + ", tweet=" + tweet + ")";
                    System.out.println(addedRow);
                });
            });
            streamedRecords.print();
            table.put(insert);
            table.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        jssc.start();
        jssc.awaitTermination();
    }
}
