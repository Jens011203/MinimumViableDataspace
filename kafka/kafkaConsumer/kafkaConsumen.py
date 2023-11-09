from confluent_kafka import Consumer

def consume_topic(topic):
    consumer = Consumer({
        "bootstrap.servers": "kafka-consumer:9094",
        "group.id": "mygroup",
        "auto.offset.reset": "earliest",
    })

    consumer.subscribe([topic])
    count = 0
    with open("kafkaConsumedData.json", "w") as f:
        while True:
            msg = consumer.poll(1.0)

            if msg is None:
                continue

            if msg.error():
                raise Exception(msg.error())
            else:
                record = msg.value().decode('utf-8')  # Decode byte hardcoded to UTF-8
                f.write(record + '\n')  # Write JSON string to file
                count += 1
                if count % 10000 == 0:
                    # Trigger the sending of all messages to the brokers every 10,000 messages
                    f.flush()
        f.flush()

def main():
    consume_topic("consumption")  # Replace with your actual Kafka topic

if __name__ == "__main__":
    main()