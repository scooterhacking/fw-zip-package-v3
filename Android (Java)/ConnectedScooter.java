package sh.cfw.utility.models;

import android.bluetooth.BluetoothDevice;

import sh.cfw.utility.SharedData;

public class ConnectedScooter {
    private BluetoothDevice _bluetoothDevice;

    private int _chipType;

    private String _deviceAddress;
    private String _deviceName;
    private String _model;
    private String _variant;
    private String _humanFriendlyModel;
    private String _uid;
    private String _automationShortcut;

    private String _drv_board;
    private String _drv_mcu;
    private String _ble_board;
    private String _bms_board;

    private boolean _useCrypto;
    private boolean _isXiaomi;

    public ConnectedScooter(BluetoothDevice bluetoothDevice, String address, String name, String model, String variant, String humanFriendlyModel,
                            boolean isXiaomi, boolean useCrypto) {
        _bluetoothDevice = bluetoothDevice;
        _deviceAddress = address;
        _deviceName = name;
        _model = model;
        _variant = variant;
        _humanFriendlyModel = humanFriendlyModel;
        _isXiaomi = isXiaomi;
        _useCrypto = useCrypto;
        _drv_board = "undefined";
        _ble_board = "undefined";
        _bms_board = "undefined";
        _drv_mcu = "n/a";
        if (_isXiaomi) {
            if (model.equals("m365")) {
                _ble_board = "mi_BLE_LEGACY";
            }
            else if (model.equals("pro"))
                _ble_board = "mi_BLE_NRF51822QFAA";
            else
                _ble_board = "mi_BLE_NRF51822QFAC";
        } else {
            if (model.equals("esx") || model.equals("e"))
                _ble_board = "nb_BLE_ROUND";
            else if (model.equals("max") || model.equals("f"))
                _ble_board = "nb_BLE_NRF51822QFAA";
            else
                _ble_board = _model + "_BLE";
        }
        SharedData.getMutableScooterData().getDrvMcu().postValue(_drv_mcu);
    }

    // Only used for Scootbatt.
    public ConnectedScooter(String uid) {
        _uid = uid;
    }

    public ConnectedScooter(String name, String model, String variant, String humanFriendlyModel, boolean isXiaomi, boolean useCrypto) {
        _deviceName = name;
        _model = model;
        _variant = variant;
        _humanFriendlyModel = humanFriendlyModel;
        _isXiaomi = isXiaomi;
        _useCrypto = useCrypto;
        _drv_board = "undefined";
        _ble_board = "undefined";
        _bms_board = "undefined";
        _drv_mcu = "n/a";
        SharedData.getMutableScooterData().getDrvMcu().postValue(_drv_mcu);
    }

    private void detectController() {
        if (_isXiaomi) {
            switch (_chipType) {
                case 0:
                case 3:
                    _drv_board = "mi_DRV_STM32F103CxT6";
                    _drv_mcu = "STM32F";
                    break;
                case 1:
                    _drv_board = "mi_DRV_GD32E103CxT6";
                    _drv_mcu = "GD32E";
                    break;
                case 2:
                    _drv_board = "mi_DRV_GD32F103CxT6";
                    _drv_mcu = "GD32F";
                    break;
                default:
                    _drv_board = "mi_DRV_UNKNOWN";
                    _drv_mcu = "n/a";
                    break;
            }
        } else if (getModel().equals("d18") || getModel().equals("d28") || getModel().equals("d38")){
            switch (_chipType) {
                case 0:
                    _drv_board = "d_DRV_STM32F103CxT6";
                    _drv_mcu = "STM32F";
                    break;
                case 1:
                    _drv_board = "d_DRV_AT32F415CxT7";
                    _drv_mcu = "AT32F";
                    break;
                default:
                    _drv_board = "d_DRV_UNKNOWN";
                    _drv_mcu = "n/a";
                    break;
            }
        } else {
            switch (_chipType) {
                case 0:
                    _drv_board = _model + "_DRV_STM32F103CxT6";
                    _drv_mcu = "STM32F";
                    break;
                case 1:
                    _drv_board = _model + "_DRV_AT32F415CxT7";
                    _drv_mcu = "AT32F";
                    break;
                default:
                    _drv_board = _model + "_DRV_UNKNOWN";
                    _drv_mcu = "n/a";
                    break;
            }
        }
        SharedData.getMutableScooterData().getDrvMcu().postValue(_drv_mcu);
    }

    private void detectBMS(int bmsVer)
    {
        if (_model.equals("f") || _model.equals("t15")) {
            _bms_board = "undefined";
            return;
        }
        else if (_isXiaomi)
            _bms_board = "mi_BMS_";
        else if (_model.equals("esx") || _model.equals("e"))
            _bms_board = "esx_e_BMS_";
        else if (_model.equals("max") || _model.equals("g2") || _model.equals("g65"))
            _bms_board = "max_BMS_";
        else
            _bms_board = _model + "_BMS_";
        if (bmsVer < 0x1000)
            _bms_board += "ST8";
        else
            _bms_board += "STM32";
    }

    public void setUid(String uid) {
        _uid = uid;
    }

    public void setUseCrypto(boolean use) {
        _useCrypto = use;
    }

    public String getUid() {
        return _uid;
    }

    public boolean hasExternalBms() {
        return (_model.equals("esx") || _model.equals("e"));
    }

    public boolean useCrypto() {
        return _useCrypto;
    }

    public boolean isXiaomi() {
        return _isXiaomi;
    }

    public int getChipType() {
        return _chipType;
    }

    public void setChipType(int chipType) { // data read from DRV reg 0x46
        _chipType = chipType;
        detectController();
    }

    public void setBMSType(int bmsVer) {
        detectBMS(bmsVer);
    }
    public BluetoothDevice getBluetoothDevice() {
        return _bluetoothDevice;
    }

    public String getScooterAddress() {
        return _deviceAddress;
    }

    public String getName() {
        return _deviceName;
    }

    public String getModel() {
        return _model;
    }

    public String getFriendlyModel() {
        return _humanFriendlyModel;
    }

    public String getAutomationShortcut() {
        return _automationShortcut;
    }

    public void setAutomationShortcut(String shortcut) {
        _automationShortcut = shortcut;
    }

    public String getDRVBoard() {
        return _drv_board;
    }

    public String getBLEBoard() {
        return _ble_board;
    }

    public String getBMSBoard() {
        return _bms_board;
    }
}
