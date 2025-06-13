import json
import sys
file_name = 'manifests-v2.json'


def build_entry(args):
    return {
        'url': args[2],
        'appname': args[3],
        'namespace': args[4],
        'fallback': args[5],
        'ssr': args[6].lower() == 'true'
    }


def add_entry(manifest, args):
    id = args[1]
    entry = build_entry(args)
    manifest[id] = entry

    return manifest


# read file
with open(file_name, 'r') as json_file:
    manifest = json.load(json_file)
    manifest = add_entry(manifest, sys.argv)

    out_json = json.dumps(manifest)


# write file
with open(file_name,'w') as json_out_file:
    json_out_file.write(out_json)