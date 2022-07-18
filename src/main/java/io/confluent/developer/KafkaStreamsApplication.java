package io.confluent.developer;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static java.lang.System.getenv;

public class KafkaStreamsApplication {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamsApplication.class);

    static void runKafkaStreams(final KafkaStreams streams) {
        final CountDownLatch latch = new CountDownLatch(1);
        streams.setStateListener((newState, oldState) -> {
            if (oldState == KafkaStreams.State.RUNNING && newState != KafkaStreams.State.RUNNING) {
                latch.countDown();
            }
        });

        streams.start();

        try {
            latch.await();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        logger.info("Streams Closed");
    }

    static Topology buildTopology(String inputTopic1, String inputTopic2, String outputTopic) {
        Serde<String> stringSerde = Serdes.String();

        StreamsBuilder builder = new StreamsBuilder();

        KTable<String, String> table1 = builder
                .stream(inputTopic1, Consumed.with(stringSerde, stringSerde))
                .peek((k, v) -> logger.info("on {} observed event: {}", inputTopic1, v))
                .toTable(
                        Materialized.<String, String>as(Stores.inMemoryKeyValueStore(inputTopic1 + "-store"))
                                .withKeySerde(stringSerde)
                                .withValueSerde(stringSerde));

        KTable<String, String> table2 = builder
                .stream(inputTopic2, Consumed.with(stringSerde, stringSerde))
                .peek((k, v) -> logger.info("on {} observed event: {}", inputTopic2, v))
                .toTable(
                        Materialized.<String, String>as(Stores.inMemoryKeyValueStore(inputTopic2 + "-store"))
                                .withKeySerde(stringSerde)
                                .withValueSerde(stringSerde));

        table1.join(table2, (s1, s2) -> s1 + " && " + s2,
                        Materialized.<String, String>as(Stores.inMemoryKeyValueStore("joined-topics-store"))
                                .withKeySerde(stringSerde)
                                .withValueSerde(stringSerde))
                .toStream()
                .peek((k, v) -> logger.info("joined on {} resulting in: {}", inputTopic2, v))
                .to(outputTopic);

        return builder.build();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("This program takes one argument: the path to a configuration file.");
        }

        Properties props = new Properties();
        try (InputStream inputStream = new FileInputStream(args[0])) {
            props.load(inputStream);
        }
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, getenv().getOrDefault("NUM_STREAM_THREADS", "1"));
        props.put(StreamsConfig.MAX_WARMUP_REPLICAS_CONFIG, getenv().getOrDefault("MAX_WARMUP_REPLICAS", "2"));
        props.put(StreamsConfig.MAX_TASK_IDLE_MS_CONFIG, getenv().getOrDefault("MAX_TASK_IDLE_MS", "30000"));
        props.put(StreamsConfig.ACCEPTABLE_RECOVERY_LAG_CONFIG, getenv().getOrDefault("ACCEPTABLE_RECOVERY_LAG", "10000"));
        props.put(StreamsConfig.PROBING_REBALANCE_INTERVAL_MS_CONFIG, getenv().getOrDefault("PROBING_REBALANCE_INTERVAL_MS", "60000"));

        final String inputTopic1 = props.getProperty("input.topic1.name");
        final String inputTopic2 = props.getProperty("input.topic2.name");
        final String outputTopic = props.getProperty("output.topic1.name");

        Util.createTopics(props, Arrays.asList(inputTopic1, inputTopic2, outputTopic));

        // Ramdomizer only used to produce sample data for this application, not typical usage
        KafkaStreams kafkaStreams = new KafkaStreams(
                buildTopology(inputTopic1, inputTopic2, outputTopic),
                props);

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));

        logger.info("Kafka Streams 101 App Started");
        runKafkaStreams(kafkaStreams);
    }
}