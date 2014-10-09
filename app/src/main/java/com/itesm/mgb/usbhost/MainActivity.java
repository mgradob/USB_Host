package com.itesm.mgb.usbhost;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.ProlificSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tw.com.prolific.driver.pl2303.PL2303Driver;


public class MainActivity extends Activity {

    TextView mainTV;
    Button sendBTN;
    EditText cmdET;

    PL2303Driver driver;
    private UsbManager mUsbManger;

    private BroadcastReceiver mUsBroadcastReceiver;
    private PendingIntent mPermissionIntent;

    private static final String ACTION_USB_PERMISSION = "com.itesm.mgb.usbhost.USB_PERMISSION";

    private PL2303Driver.BaudRate mBaudrate = PL2303Driver.BaudRate.B9600;
    private PL2303Driver.DataBits mDataBits = PL2303Driver.DataBits.D8;
    private PL2303Driver.Parity mParity = PL2303Driver.Parity.NONE;
    private PL2303Driver.StopBits mStopBits = PL2303Driver.StopBits.S1;
    private PL2303Driver.FlowControl mFlowControl = PL2303Driver.FlowControl.OFF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainTV = (TextView) findViewById(R.id.usbDevTV);
        sendBTN = (Button) findViewById(R.id.sendBTN);
        cmdET = (EditText) findViewById(R.id.textET);

        mUsbManger = (UsbManager) getSystemService(Context.USB_SERVICE);

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsBroadcastReceiver, filter);

        HashMap<String, UsbDevice> deviceList = mUsbManger.getDeviceList();
        for (HashMap.Entry<String, UsbDevice> entry : deviceList.entrySet()){
            UsbDevice aDevice = entry.getValue();
            if(!mUsbManger.hasPermission(aDevice)){
                mUsbManger.requestPermission(aDevice, mPermissionIntent);
            }
        }

        /*
        driver = new PL2303Driver((UsbManager) getSystemService(Context.USB_SERVICE), this, ACTION_USB_PERMISSION);
        Toast.makeText(this, "Driver: " + driver, Toast.LENGTH_SHORT).show();

        if(!driver.PL2303USBFeatureSupported()){
            Toast.makeText(this, "No support for USB host API", Toast.LENGTH_SHORT).show();
            driver = null;
        }

        if (driver != null) {
            if(driver.PL2303Device_IsHasPermission()){
                Toast.makeText(this, "OK Permission", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(this, "NO Permission", Toast.LENGTH_SHORT).show();

            if(driver.PL2303Device_IsSupportChip()){
                Toast.makeText(this, "Chip Supported", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(this, "Chip Not Supported", Toast.LENGTH_SHORT).show();
        }*/



        sendBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openConnection();
            }
        });
    }

    /*
    void openConnection(){
        if (driver == null) return;

        if (!driver.isConnected()) {
            mBaudrate = PL2303Driver.BaudRate.B9600;

            try {
                int res = driver.setup(mBaudrate, mDataBits, mStopBits, mParity, mFlowControl);
                Toast.makeText(this, "Connection, result: " + res, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Didn't setup", Toast.LENGTH_SHORT).show();
                return;
            }

            if (driver.InitByBaudRate(mBaudrate)) {
                Toast.makeText(this, "Something happened", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
            }
        } else return;

        String data = mainTV.getText().toString();
        int result = driver.write(data.getBytes());

        Toast.makeText(this, "Wrote: " + result, Toast.LENGTH_SHORT).show();

        byte[] in_buf = new byte[1000];
        StringBuffer stringBuffer = new StringBuffer();
        int len;

        len = driver.read(in_buf);

        if (len > 0){
            for (int j=0; j<len; j++){
                stringBuffer.append((char) in_buf[j]&0x000000FF);
            }
            mainTV.setText(stringBuffer.toString());
            Toast.makeText(this, "Len: " + len, Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(this, "0 length or negative", Toast.LENGTH_SHORT).show();

        driver.end();
    }*/


    private void openConnection(){
        Toast.makeText(this, "Opening connection", Toast.LENGTH_SHORT).show();

        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManger);
        List<UsbSerialDriver> proberDrivers;
        UsbSerialDriver driver;

        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "Probing for specific device", Toast.LENGTH_SHORT).show();
            mainTV.append("No devices attached :(");

            ProbeTable customTable = new ProbeTable();
            customTable.addProduct(1659, 8963, ProlificSerialDriver.class);

            UsbSerialProber prober = new UsbSerialProber(customTable);
            proberDrivers = prober.findAllDrivers(mUsbManger);

            if (proberDrivers.isEmpty()) return;

            driver = proberDrivers.get(0);

            String id;
            id = "DeviceID: " + driver.getDevice().getDeviceId();
            id += "\nDeviceVendorID: " + driver.getDevice().getVendorId();
            id += "\nDeviceProductID: " + driver.getDevice().getProductId();

            Toast.makeText(this, "Driver: " + driver.getDevice().getDeviceName() + "\n" + id, Toast.LENGTH_SHORT).show();
        } else {
            driver = availableDrivers.get(1);

            String id;
            id = "DeviceID: " + driver.getDevice().getDeviceId();
            id += "\nDeviceVendorID: " + driver.getDevice().getVendorId();
            id += "\nDeviceProductID: " + driver.getDevice().getProductId();

            Toast.makeText(this, "Driver: " + driver.getDevice().getDeviceName() + "\n" + id, Toast.LENGTH_SHORT).show();
        }

        UsbDeviceConnection connection = mUsbManger.openDevice(driver.getDevice());

        if (connection == null) {
            Toast.makeText(this, "Can't open a connection to the device", Toast.LENGTH_LONG).show();
            return;
        }

        List<UsbSerialPort> ports = driver.getPorts();

        UsbSerialPort port = ports.get(0);
        try {
            port.open(connection);
            port.setParameters(9600, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            byte writeBuffer[] = cmdET.getText().toString().getBytes();
            byte readBuffer[] = new byte[100];

            int ack = port.write(writeBuffer, 1500);
            Toast.makeText(this, String.format("Wrote %d bytes to uC", ack) , Toast.LENGTH_SHORT).show();

            ack = port.read(readBuffer, 1500);
            Toast.makeText(this, String.format("Read %d bytes to uC", ack) , Toast.LENGTH_SHORT).show();
            String reading = new String(readBuffer);
            mainTV.append("Received: " + reading);
        } catch (IOException e) {
            Log.e("Error", "Can't open port");
        } finally {
            try {
                port.close();
            } catch (IOException e) {
                Log.e("Error", "Can't close port");
            }
        }
    }
}