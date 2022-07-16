## kafka-streams-stateful-rebalance-idle-group-members

TODO: describe problem

### stack

* Java 11
* kafka-streams 2.8.1
* topics: 1
* stores: 1 (KTable)

### howto reproduce

    docker exec -it broker kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-streams-101 --describe

    docker logs -f app-1 2>&1 |grep -A 4 "per-consumer assignment"
    docker-compose logs --tail 0 --follow 2>&1 |grep -A 4 "per-consumer assignment"
    docker-compose logs --tail 0 --follow 2>&1 |grep -A 4 "Handle new assignment"