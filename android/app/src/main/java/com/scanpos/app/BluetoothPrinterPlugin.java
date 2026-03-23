package com.scanpos.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

@CapacitorPlugin(name = "BluetoothPrinter")
public class BluetoothPrinterPlugin extends Plugin {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @PluginMethod
    public void getBluetoothDevices(PluginCall call) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) { call.reject("Bluetooth not supported"); return; }
        Set<BluetoothDevice> paired = adapter.getBondedDevices();
        JSONArray arr = new JSONArray();
        for (BluetoothDevice d : paired) {
            JSObject obj = new JSObject();
            obj.put("name", d.getName());
            obj.put("address", d.getAddress());
            arr.put(obj);
        }
        JSObject ret = new JSObject();
        ret.put("devices", arr);
        call.resolve(ret);
    }

    @PluginMethod
    public void printText(PluginCall call) {
        String address = call.getString("address");
        String text = call.getString("text");
        if (address == null || text == null) { call.reject("address and text required"); return; }
        new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = adapter.getRemoteDevice(address);
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
                OutputStream os = socket.getOutputStream();
                os.write(text.getBytes("UTF-8"));
                os.flush();
                socket.close();
                call.resolve();
            } catch (Exception e) {
                try { if (socket != null) socket.close(); } catch (Exception ignored) {}
                call.reject(e.getMessage());
            }
        }).start();
    }
}
