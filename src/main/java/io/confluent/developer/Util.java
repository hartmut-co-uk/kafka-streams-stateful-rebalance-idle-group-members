package io.confluent.developer;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class Util {

    private final static Logger logger = LoggerFactory.getLogger(Util.class);

    public static void createTopics(final Properties props, List<String> topics) {
        try (final AdminClient client = AdminClient.create(props)) {
            logger.info("Creating topics: {}", topics);

            final int numPartitions = Integer.parseInt(props.getProperty("topics.num-partitions"));
            final short replicationFactor = Short.parseShort(props.getProperty("topics.replication-factor"));

            final List<NewTopic> newTopics = topics.stream()
                    .map(topic -> new NewTopic(topic, numPartitions, replicationFactor))
                    .collect(Collectors.toList());


            client.createTopics(newTopics).values().forEach((topic, future) -> {
                try {
                    future.get();
                } catch (Exception ex) {
                    logger.info(ex.toString());
                }
            });

            //logTopics(topics, client);
        }
    }

    private static void logTopics(List<String> topics, AdminClient client) throws InterruptedException, ExecutionException, TimeoutException {
        logger.info("Asking cluster for topic descriptions");
        client
                .describeTopics(topics)
                .all() // allTopicNames()
                .get(10, TimeUnit.SECONDS)
                .forEach((name, description) -> logger.info("Topic Description: {}", description.toString()));
    }
}