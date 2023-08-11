# importing the module
import json
import sys
json_file_name = 'manifests.json'

# Opening JSON file
with open(json_file_name, 'r') as json_file:
    data = json.load(json_file)
    data[sys.argv[1]] = sys.argv[2]
    out_json = json.dumps(data)
    json_file.close()

with open(json_file_name,'w') as json_out_file:
    json_out_file.write(out_json)