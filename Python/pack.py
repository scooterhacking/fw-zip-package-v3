import io
import zipfile
import hashlib
import json
import os
import argparse
from typing import List, Optional
from NinebotTEA.NinebotTEA import NinebotTEA

ALLOWED_ENC_FLAGS_ZIP3 = {'both', 'plain', 'encrypted'}
ALLOWED_TYP_FLAGS = {'DRV', 'BMS', 'BLE'}
MODEL_LENGTH_RANGE = (1, 10)


def validate_model(model: str):
    if not model:
        raise ValueError('Model must be specified')
    if not (MODEL_LENGTH_RANGE[0] <= len(model) <= MODEL_LENGTH_RANGE[1]):
        raise ValueError('Model must be between 1 and 10 characters long')
    if not model.isalnum():
        raise ValueError('Model must be alphanumerical')


def validate_boards(boards: List):
    if len(boards) == 0 or not isinstance(boards, list):
        raise ValueError('You must specify at least one compatible board, in a list!')


def validate_encryption_flag(enc: str):
    if enc not in ALLOWED_ENC_FLAGS_ZIP3:
        raise ValueError(f'Invalid encryption flag! Allowed flags: {ALLOWED_ENC_FLAGS_ZIP3}')


def validate_type_flag(type_flag: str):
    if type_flag not in ALLOWED_TYP_FLAGS:
        raise ValueError(f'Invalid type flag! Allowed flags: {ALLOWED_TYP_FLAGS}')


def make_zip_v3(data: bytes, name: str, type_flag: str, model: str, boards: List[str], 
                enforce_model: bool, enc: str, schema_version: int, params: Optional[str] = None) -> bytes:
    """
    Create a ZIP file version 3 with specific firmware details.

    :param data: The firmware data.
    :param name: The firmware display name.
    :param type_flag: The type flag.
    :param model: The model name.
    :param boards: List of compatible boards.
    :param enforce_model: Boolean to enforce model.
    :param enc: Encryption flag.
    :param schema_version: The schema version.
    :param params: Additional parameters.
    :return: The content of the ZIP file.
    """
    if schema_version != 1:
        raise ValueError('Unknown schema version')

    validate_model(model)
    validate_boards(boards)
    validate_encryption_flag(enc)
    validate_type_flag(type_flag)

    zip_buffer = io.BytesIO()
    with zipfile.ZipFile(zip_buffer, 'a', zipfile.ZIP_DEFLATED, False) as zip_file:
        info_json = {
            "schemaVersion": schema_version,
            "firmware": {
                "displayName": name,
                "model": model,
                "enforceModel": enforce_model,
                "type": type_flag,
                "compatible": boards,
                "encryption": enc,
                "md5": {}
            }
        }

        if enc in {"both", "plain"}:
            zip_file.writestr('FIRM.bin', data)
            md5 = hashlib.md5(data).hexdigest()
            info_json["firmware"]["md5"]["bin"] = md5

        if enc in {"both", "encrypted"}:
            encrypted_data = NinebotTEA().encrypt(data)
            zip_file.writestr('FIRM.bin.enc', encrypted_data)
            md5e = hashlib.md5(encrypted_data).hexdigest()
            info_json["firmware"]["md5"]["enc"] = md5e

        zip_file.writestr('info.json', json.dumps(info_json, indent=4))

        if params:
            zip_file.writestr('params.txt', params)

    zip_buffer.seek(0)
    content = zip_buffer.getvalue()
    return content

def main():
    parser = argparse.ArgumentParser(description="Generate a firmware ZIP file. Example: python pack.py \"1.6.13 (Compat).bin\" \"1.6.13 (Compat)\" max DRV max_DRV_STM32F103CxT6,max_DRV_AT32F415CxT7 True encrypted \"1.6.13 (Compat).zip\"")
    parser.add_argument("bin_file_path", help="Path to the firmware .bin file")
    parser.add_argument("name", help="Firmware display name")
    parser.add_argument("model", help="Model name (1-10 alphanumeric characters)")
    parser.add_argument("type_flag", choices=ALLOWED_TYP_FLAGS, help="Type flag")
    parser.add_argument("boards", help="List of compatible boards (comma-separated)")
    parser.add_argument("enforce_model", type=bool, help="Enforce model? (True/False)")
    parser.add_argument("enc", choices=ALLOWED_ENC_FLAGS_ZIP3, help="Encryption flag")
    parser.add_argument("output_zip_path", help="Path to save the output ZIP file")
    parser.add_argument("--params", default="", help="Additional parameters if any, for the params.txt file (optional)")
    parser.add_argument("--schema_version", type=int, default=1, help="Schema version (default 1)")

    args = parser.parse_args()

    if not os.path.exists(args.bin_file_path):
        print("File not found. Please check the path.")
        return

    with open(args.bin_file_path, 'rb') as file:
        data = file.read()

    try:
        zip_content = make_zip_v3(
            data, args.name, args.type_flag, args.model, args.boards.split(','),
            args.enforce_model, args.enc, args.schema_version, args.params
        )

        with open(args.output_zip_path, 'wb') as zip_file:
            zip_file.write(zip_content)

        print(f"ZIP file saved successfully to {args.output_zip_path}")

    except ValueError as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()
