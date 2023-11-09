import copy
import json

#open and read file
with open('countJson.json', 'r') as f:
    data = json.load(f)

duplicationCount = 1000000 

with open('countTo1000000.json', 'w') as f:

    f.write('[')

    #Erstellt die Kopien und schreibt sie direkt in die Datei
    for i in range(1, duplicationCount + 1):
            # Erstellt eine tiefe Kopie des Eintrags
            json_copy = copy.deepcopy(data)
             
            json_copy['count'] = i

            # Wandelt die Kopie in einen JSON-String um und schreibt sie in die Datei
            f.write(json.dumps(json_copy, separators=(',', ':')))

            # Fügt ein Komma hinzu, wenn es noch weitere Einträge gibt
            if i != duplicationCount - 1:
                f.write(',\n')
            
    f.write(']')



