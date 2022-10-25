import zipfile
import hashlib
import json

ALLOWED_ENC_FLAGS_ZIP3 = {'both', 'plain', 'encrypted'}
ALLOWED_TYP_FLAGS = {'DRV', 'BMS', 'BLE'}

def make_zip_v3(data, name, type, model, boards, enforce_model, enc, schema_version, params):
	if (schema_version != 1):
		raise ValueError('Unknown schema version')
	if not model:
		raise ValueError('model must be specified')
	if (len(model) <= 0 or len(model) > 10):
		raise ValueError('model must be between 1 and 10 characters long')
	if not model.isalnum():
		raise ValueError('model must be alphanumerical')
	if (len(boards) <= 0 or not isinstance(boards, list)):
		raise ValueError('You must specify at least one compatible board, in a list!')
	if enc not in ALLOWED_ENC_FLAGS_ZIP3:
		raise ValueError('Invalid encryption flag! Allowed flags: ' + str(ALLOWED_ENC_FLAGS_ZIP3))
	if type not in ALLOWED_TYP_FLAGS:
		raise ValueError('Invalid type flag! Allowed flags: ' + str(ALLOWED_TYP_FLAGS))

	zip_buffer = io.BytesIO()
	zip_file = zipfile.ZipFile(zip_buffer, 'a', zipfile.ZIP_DEFLATED, False)

	info_json = {}
	info_json["schemaVersion"] = schema_version
	info_json["firmware"] = {}
	info_json["firmware"]["displayName"] = name
	info_json["firmware"]["model"] = model
	info_json["firmware"]["enforceModel"] = enforce_model
	info_json["firmware"]["type"] = type
	info_json["firmware"]["compatible"] = boards
	info_json["firmware"]["encryption"] = enc
	info_json["firmware"]["md5"] = {}

	if (enc == "both" or enc == "plain"):
		zip_file.writestr('FIRM.bin', data)
		md5 = hashlib.md5()
		md5.update(data)
		info_json["firmware"]["md5"]["bin"] = md5.hexdigest()

	if (enc == "both" or enc == "encrypted"):
		data = encrypt(data)
		zip_file.writestr('FIRM.bin.enc', data)
		md5e = hashlib.md5()
		md5e.update(data)
		info_json["firmware"]["md5"]["enc"] = md5e.hexdigest()


	zip_file.writestr('info.json', json.dumps(info_json, indent=4).encode())

	if params is not None:
		zip_file.writestr('params.txt', params.encode())

	zip_file.close()
	zip_buffer.seek(0)
	content = zip_buffer.getvalue()
	zip_buffer.close()
	return (content)