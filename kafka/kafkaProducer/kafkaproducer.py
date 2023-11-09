from confluent_kafka import Producer

# Kafka configuration
conf = {
  'bootstrap.servers': 'kafka-provider:9092',
  'queue.buffering.max.messages': 10000000,
  'delivery.timeout.ms': 3000000,
}

producer = Producer(conf)

count = 0
# Open file
with open('Filename.json', 'r') as f:
    for line in f:
        # Send JSON object to the topic with the comma at the end
        producer.produce("consumption", line.strip())
        # Trigger the sending of all messages to the brokers
        count += 1
        if count % 10000 == 0:
            # Trigger the sending of all messages to the brokers every 10,000 messages
            producer.flush()
producer.flush()