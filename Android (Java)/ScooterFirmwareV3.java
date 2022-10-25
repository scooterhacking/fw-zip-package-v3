package sh.cfw.utility.classes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ScooterFirmwareV3 {

    public enum ERROR {NONE, ZIP_ERROR, UNSUPPORTED_SCHEMA_VERSION, INVALID_JSON, INVALID_ENC_FLAG, INVALID_TYPE, MISSING_ENC, MISSING_BIN, INVALID_MD5, INVALID_MD5E, UNKNOWN}
    public enum WARNING {NONE, MODEL_MISMATCH, INCOMPATIBLE}

    private ERROR _error;
    private WARNING _warning;

    private byte[] _bin;
    private byte[] _enc;

    private String _enc_flag;
    private String _name;
    private String _model;
    private String _type;

    private String _md5;
    private String _md5e;

    private String _params;

    private boolean _enforceModel;

    private List<String> _compatible;

    private static final List<String> required_keys = Arrays.asList("displayName", "model", "enforceModel", "type", "compatible", "encryption", "md5");
    private static final List<String> accepted_enc = Arrays.asList("both", "plain", "encrypted");

    public ScooterFirmwareV3(ZipInputStream zipIs, String model, String ble_board, String drv_board, String bms_board) {
        _error = ERROR.NONE;
        _warning = WARNING.NONE;
        boolean jsonOK = false;
        String board = "undefined";
        try {
            if (zipIs == null) {
                _error = ERROR.ZIP_ERROR;
                return;
            }
            ZipEntry ze;
            while ((ze = zipIs.getNextEntry()) != null) {
                if (ze.getName().equals("info.json")) {
                    String data = getStringFromEntry(zipIs);
                    if (data == null) {
                        _error = ERROR.INVALID_JSON;
                        return;
                    }
                    JSONObject readObject = new JSONObject(data);
                    if (!readObject.has("firmware")) {
                        _error = ERROR.INVALID_JSON;
                        return;
                    }
                    if (!readObject.has("schemaVersion")) {
                        _error = ERROR.INVALID_JSON;
                        return;
                    }
                    int schema_version = readObject.getInt("schemaVersion");
                    if (schema_version != 1) {
                        _error = ERROR.UNSUPPORTED_SCHEMA_VERSION;
                        return;
                    }
                    readObject = readObject.getJSONObject("firmware");
                    for (String key : required_keys) {
                        if (!readObject.has(key)) {
                            _error = ERROR.INVALID_JSON;
                            return;
                        }
                    }
                    _name = readObject.getString("displayName");
                    _model = readObject.getString("model");
                    _enforceModel = readObject.getBoolean("enforceModel");
                    _type = readObject.getString("type");
                    switch (_type) {
                        case "BLE":
                            board = ble_board;
                            break;
                        case "DRV":
                            board = drv_board;
                            break;
                        case "BMS":
                            board = bms_board;
                            break;
                        default:
                            _error = ERROR.INVALID_TYPE;
                            return;
                    }
                    _enc_flag = readObject.getString("encryption");
                    if (!accepted_enc.contains(_enc_flag)) {
                        _error = ERROR.INVALID_ENC_FLAG;
                        return;
                    }
                    JSONObject md5 = readObject.getJSONObject("md5");
                    if (_enc_flag.equals("both") || _enc_flag.equals("enc")) {
                        if (md5.has("enc")) {
                            _md5e = md5.getString("enc");
                        } else {
                            _error = ERROR.INVALID_MD5E;
                            return;
                        }
                    }
                    if (_enc_flag.equals("both") || _enc_flag.equals("plain")) {
                        if (md5.has("bin")) {
                            _md5 = md5.getString("bin");
                        } else {
                            _error = ERROR.INVALID_MD5;
                            return;
                        }
                    }
                    JSONArray jsonArray = readObject.getJSONArray("compatible");
                    if (jsonArray.length() < 1) {
                        _error = ERROR.INVALID_JSON;
                        return;
                    }
                    _compatible = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        _compatible.add(jsonArray.getString(i));
                    }
                    jsonOK = true;
                }
                if (ze.getName().equals("FIRM.bin")) {
                    int size = (int) ze.getSize();
                    _bin = new byte[size];
                    int read = 0;
                    while (read < size)
                        read += zipIs.read(_bin, read, (size - read));
                }
                if (ze.getName().equals("FIRM.bin.enc")) {
                    int size = (int) ze.getSize();
                    _enc = new byte[size];
                    int read = 0;
                    while (read < size)
                        read += zipIs.read(_enc, read, (size - read));
                }
                if (ze.getName().equals("params.txt")) {
                    _params = getStringFromEntry(zipIs);
                }
            }
            if (!jsonOK) {
                _error = ERROR.INVALID_JSON;
                return;
            }
            if (_enc_flag.equals("both") || _enc_flag.equals("plain")) {
                if (_bin == null) {
                    _error = ERROR.MISSING_BIN;
                    return;
                }
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(_bin);
                byte[] digest = md.digest();
                String hash = bytesToHex(digest).toLowerCase();
                if (!hash.equals(_md5)) {
                    _error = ERROR.INVALID_MD5;
                    return;
                }
            }
            if (_enc_flag.equals("both") || _enc_flag.equals("enc")) {
                if (_enc == null) {
                    _error = ERROR.MISSING_ENC;
                    return;
                }
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(_enc);
                byte[] digest = md.digest();
                String hash = bytesToHex(digest).toLowerCase();
                if (!hash.equals(_md5e)) {
                    _error = ERROR.INVALID_MD5E;
                    return;
                }
            }
            if (_enforceModel && !_model.equals(model))
                _warning = WARNING.MODEL_MISMATCH;
            if (!_compatible.contains(board) || board.equals("undefined") || board.contains("GENERIC") || board.contains("UNKNOWN"))
                _warning = WARNING.INCOMPATIBLE;
        } catch (Exception e) {
            _error = ERROR.UNKNOWN;
        }
    }

    public ERROR getError() {
        return _error;
    }

    public WARNING getWarning() {
        return _warning;
    }

    public byte[] getBin() {
        return _bin;
    }

    public byte[] getEnc() {
        return _enc;
    }

    public String getEncFlag() {
        return _enc_flag;
    }

    public String getName() {
        return _name;
    }

    public String getModel() {
        return _model;
    }

    public String getType() {
        return _type;
    }

    public String getMD5() {
        return _md5;
    }

    public String getMD5E() {
        return _md5e;
    }

    public String getParams() {
        return _params;
    }

    public boolean getEnforceModel() {
        return _enforceModel;
    }

    public List<String> getCompatible() {
        return _compatible;
    }

    private String getStringFromEntry(ZipInputStream zipIs) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(zipIs));
            StringBuilder sb = new StringBuilder();
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    sb.append(line);
                    firstLine = false;
                } else {
                    sb.append("\n").append(line);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
    private final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
