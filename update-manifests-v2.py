import json
import sys
file_name = 'manifests-v2.json'


def build_microfrontend(args):
    return {
        'url': args[2],
        'appname': args[3],
        'namespace': args[4],
        'ssr': args[5].lower() == 'true'
    }


def add_microfrontend(microfrontends, args):
    id = args[1]
    microfrontend = build_microfrontend(args)
    microfrontends[id] = microfrontend

    return microfrontends


# read file
with open(file_name, 'r') as json_file:
    microfrontends = json.load(json_file)
    microfrontends = add_microfrontend(microfrontends, sys.argv)

    out_json = json.dumps(microfrontends)


# write file
with open(file_name,'w') as json_out_file:
    json_out_file.write(out_json)