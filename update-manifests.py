# importing the module
import json
import sys
json_file_name = 'manifests.json'
json_file_name_v2 = 'manifests-v2.json'

# Opening JSON file
with open(json_file_name, 'r') as json_file:
    data = json.load(json_file)
    data[sys.argv[1]] = sys.argv[2]
    out_json = json.dumps(data)
    json_file.close()

with open(json_file_name,'w') as json_out_file:
    json_out_file.write(out_json)

# Opening JSON file
with open(json_file_name_v2, 'r') as json_file_v2:
    data_v2 = json.load(json_file_v2)
    data_v2[sys.argv[1]] = {
        'url': sys.argv[2],
        'appname': '',
        'namespace': '',
        'fallback': '',
        'ssr': False
    }
    out_json_v2 = json.dumps(data_v2)
    json_file_v2.close()

with open(json_file_name_v2, 'w') as json_out_file_v2:
    json_out_file_v2.write(out_json_v2)