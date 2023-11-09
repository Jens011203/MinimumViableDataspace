import json

def is_in_order(file_name):
    last_count = 0
    entries_false = 0
    decoder = json.JSONDecoder()

    with open(file_name, 'r') as f:
        data = json.load(f)
        for item in data:
            count = item["count"]
            if count < last_count:
                entries_false += 1
            last_count = count

    return entries_false

file_name = '/Users/I572661/Documents/Praxisphase/Praxisphase Innovation Energy/MinimumViableDataspace/testData/kafkaConsumedData.json'
print(is_in_order(file_name))
