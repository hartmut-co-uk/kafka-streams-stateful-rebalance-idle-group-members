package io.confluent.developer;


import com.github.javafaker.Faker;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.lang.System.getenv;

public class KafkaProducerApplication {

    private final Logger logger = LoggerFactory.getLogger(KafkaProducerApplication.class);

    private final Producer<String, String> producer;
    private final String outTopic;
    private final int batchSize;
    private final int batchScheduleMs;

    private final Random random = new Random();

    private boolean closed;

    public KafkaProducerApplication(final Producer<String, String> producer,
                                    final String topic,
                                    final int batchSize,
                                    final int batchScheduleMs
    ) {
        this.closed = false;
        this.producer = producer;
        this.outTopic = topic;
        this.batchSize = batchSize;
        this.batchScheduleMs = batchScheduleMs;
    }

    public Future<RecordMetadata> produceRandom() {
        Faker faker = new Faker();
        final ProducerRecord<String, String> producerRecord = new ProducerRecord<>(
                this.outTopic,
                String.valueOf(random.nextInt(12 * 10)),
                faker.chuckNorris().fact());
        return producer.send(producerRecord);
    }

    private void start() {
        logger.info("Kafka Random Data Producer App Started");
        while (!closed) {
            try {
                logger.info("produce new batch of random string messages...");
                for (int i = 0; i < batchSize; i++) {
                    produceRandom().get();
                }
                Thread.sleep(batchScheduleMs);
            } catch (Exception ex) {
                logger.error(ex.toString());
            }
        }
    }

    public void shutdown() {
        closed = true;
        producer.close(Duration.ofSeconds(5));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("This program takes two arguments: the path to a configuration file, topic name to produce test data to.");
        }

        Properties props = new Properties();
        try (InputStream inputStream = Files.newInputStream(Paths.get(args[0]))) {
            props.load(inputStream);
        }

        final String clientId = getenv().get("CLIENT_ID");
        final int batchSize = Integer.parseInt(getenv().get("BATCH_SIZE"));
        final int batchScheduleMs = Integer.parseInt(getenv().get("BATCH_SCHEDULE_MS"));

        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);

        final String topic = args[1];

        final Producer<String, String> producer = new KafkaProducer<>(props);
        final KafkaProducerApplication producerApp = new KafkaProducerApplication(producer, topic, batchSize, batchScheduleMs);

        Util.createTopics(props, Arrays.asList(topic));

        try {
            producerApp.start();
        } finally {
            producerApp.shutdown();
        }
    }
}