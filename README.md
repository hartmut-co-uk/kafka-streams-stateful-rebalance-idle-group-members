## kafka-streams-stateful-rebalance-idle-group-members

TODO: describe problem

### stack

* Java 11
* kafka-streams 2.8.1
* topics: 2
* stores: 3 (2x KTable + 1x join)

### howto reproduce

    docker exec -it broker kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-streams-101 --describe

    docker logs -f app-1 2>&1 |grep -A 4 "per-consumer assignment"
    docker-compose logs --tail 0 -t --follow 2>&1 |grep -A 4 "per-consumer assignment"
    docker-compose logs --tail 0 -t --follow 2>&1 |grep -A 4 "Handle new assignment"
    docker-compose logs --tail 0 -t --follow 2>&1 |grep -A 6 "Assigned tasks"
    docker-compose logs --tail 0 -t --follow 2>&1 |grep "Restoration took"    

### Theories
* what threads do standby run on? 
  * use more threads and warmup replica?  