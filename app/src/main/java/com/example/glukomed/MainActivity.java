package com.example.glukomed;

import static android.R.layout.simple_list_item_1;
import static com.example.glukomed.constants.BluetoothConstants.KAKOGOTO_CHERTA_17;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.glukomed.constants.BluetoothConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private BluetoothAdapter bluetoothAdapter;
    private ListView listViewPairedDevice;
    private FrameLayout butPanel;
    private ThreadConnectBTdevice myThreadConnectBTdevice;
    private ThreadConnected myThreadConnected;
    private UUID myUUID;
    private StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ToggleButton tb1 = (ToggleButton) findViewById(R.id.toggleButton1);
        ToggleButton tb2 = (ToggleButton) findViewById(R.id.toggleButton2);
        ToggleButton tb3 = (ToggleButton) findViewById(R.id.toggleButton3);
        ToggleButton tb4 = (ToggleButton) findViewById(R.id.toggleButton4);

        tb1.setOnCheckedChangeListener(this);
        tb2.setOnCheckedChangeListener(this);
        tb3.setOnCheckedChangeListener(this);
        tb4.setOnCheckedChangeListener(this);
        final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
        TextView textInfo = (TextView) findViewById(R.id.textInfo);
        listViewPairedDevice = (ListView) findViewById(R.id.list);
        butPanel = (FrameLayout) findViewById(R.id.panel);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "BLUETOOTH NOT support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        @SuppressLint("HardwareIds") String stInfo = bluetoothAdapter.getName() + " " + bluetoothAdapter.getAddress();
        textInfo.setText(String.format("Это устройство: %s", stInfo));
    }

    @Override
    protected void onStart() { // Запрос на включение Bluetooth
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableIntent, BluetoothConstants.REQUEST_ENABLE_BT);
        }
        setup();
    }


    private void setup() { // Создание списка сопряжённых Bluetooth-устройств
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (!pairedDevices.isEmpty()) { // Если есть сопряжённые устройства
            ArrayList<String> pairedDeviceArrayList = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) { // Добавляем сопряжённые устройства - Имя + MAC-адресс
                pairedDeviceArrayList.add(device.getName() + "\n" + device.getAddress());
            }
            ArrayAdapter<String> pairedDeviceAdapter = new ArrayAdapter<>(this, simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);
            // Клик по нужному устройству
            listViewPairedDevice.setOnItemClickListener((parent, view, position, id) -> { //тут пробел после скобки !!!!
                listViewPairedDevice.setVisibility(View.GONE); // После клика скрываем список
                String itemValue = (String) listViewPairedDevice.getItemAtPosition(position);
                String MAC = itemValue.substring(itemValue.length() - KAKOGOTO_CHERTA_17); // Вычленяем MAC-адрес
                BluetoothDevice device2 = bluetoothAdapter.getRemoteDevice(MAC);
                myThreadConnectBTdevice = new ThreadConnectBTdevice(device2);
                myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
            });
        }
    }

    @Override
    protected void onDestroy() { // Закрытие приложения
        super.onDestroy();
        if (myThreadConnectBTdevice != null) {
            myThreadConnectBTdevice.cancel(); // За if в одну строку в приличном обществе руки отрывают :)
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothConstants.REQUEST_ENABLE_BT) { // Если разрешили включить Bluetooth, тогда void setup()
            if (resultCode == Activity.RESULT_OK) {
                setup();
            } else { // Если не разрешили, тогда закрываем приложение
                Toast.makeText(this, "BlueTooth не включён", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        switch (compoundButton.getId()) { // Весь switch надо переписать с if - else if
            case R.id.toggleButton1:
                if (isChecked) {
                    if (myThreadConnected != null) {
                        byte[] bytesToSend = "a".getBytes();
                        myThreadConnected.write(bytesToSend);
                    }
                    Toast.makeText(MainActivity.this, "D10 ON", Toast.LENGTH_SHORT).show();
                } else {
                    if (myThreadConnected != null) {
                        byte[] bytesToSend = "A".getBytes();
                        myThreadConnected.write(bytesToSend);
                    }
                    Toast.makeText(MainActivity.this, "D10 OFF", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.toggleButton2:
                if (isChecked) {
                    if (myThreadConnected != null) {
                        byte[] bytesToSend = "b".getBytes();
                        myThreadConnected.write(bytesToSend);
                    }

                    Toast.makeText(MainActivity.this, "D11 ON", Toast.LENGTH_SHORT).show();
                } else {
                    if (myThreadConnected != null) {
                        byte[] bytesToSend = "B".getBytes();
                        myThreadConnected.write(bytesToSend);
                    }

                    Toast.makeText(MainActivity.this, "D11 OFF", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.toggleButton3:
                if (isChecked) {
                    if (myThreadConnected != null) {
                        byte[] bytesToSend = "c".getBytes();
                        myThreadConnected.write(bytesToSend);
                    }

                    Toast.makeText(MainActivity.this, "D12 ON", Toast.LENGTH_SHORT).show();
                } else {
                    if (myThreadConnected != null) {
                        byte[] bytesToSend = "C".getBytes();
                        myThreadConnected.write(bytesToSend);
                    }

                    Toast.makeText(MainActivity.this, "D12 OFF", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.toggleButton4:
                if (isChecked) {
                    if (myThreadConnected != null) {
                        byte[] bytesToSend = "d".getBytes();
                        myThreadConnected.write(bytesToSend);
                    }

                    Toast.makeText(MainActivity.this, "D13 ON", Toast.LENGTH_SHORT).show();
                } else {
                    if (myThreadConnected != null) {
                        byte[] bytesToSend = "D".getBytes();
                        myThreadConnected.write(bytesToSend);
                    }

                    Toast.makeText(MainActivity.this, "D13 OFF", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth

        private BluetoothSocket bluetoothSocket = null;
        private final Logger logger = Logger.getLogger("ThreadConnectBTdevice logger");

        private ThreadConnectBTdevice(BluetoothDevice device) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        }

        @Override
        public void run() { // Коннект
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                logger.warning(e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Нет коннекта, проверьте Bluetooth-устройство, с которым хотите соединиться!", Toast.LENGTH_LONG).show();
                    listViewPairedDevice.setVisibility(View.VISIBLE);
                });
                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    logger.warning(e1.getMessage());
                }
            }
            if (success) {  // Если законнектились, тогда открываем панель с кнопками и запускаем поток приёма и отправки данных
                runOnUiThread(() -> {
                    butPanel.setVisibility(View.VISIBLE); // открываем панель с кнопками
                });

                myThreadConnected = new ThreadConnected(bluetoothSocket);
                myThreadConnected.start(); // запуск потока приёма и отправки данных
            }
        }

        public void cancel() {
            Toast.makeText(getApplicationContext(), "Close - BluetoothSocket", Toast.LENGTH_LONG).show();
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        }

    } // END ThreadConnectBTdevice:

    private class ThreadConnected extends Thread {    // Поток - приём и отправка данных
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;
        private String sbprint;
        private final Logger logger = Logger.getLogger("ThreadConnected logger");

        public ThreadConnected(BluetoothSocket socket) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() { // Приём данных
            while (true) {
                try {
                    byte[] buffer = new byte[1];
                    int bytes = connectedInputStream.read(buffer);
                    String strIncom = new String(buffer, 0, bytes);
                    sb.append(strIncom); // собираем символы в строку
                    int endOfLineIndex = sb.indexOf("\r\n"); // определяем конец строки
                    if (endOfLineIndex > 0) {
                        sbprint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());
                        // Вывод данных
                        runOnUiThread(() -> {
                            switch (sbprint) {

                                case "D10 ON":
                                    Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_SHORT).show();
                                    break;

                                case "D10 OFF":
                                    Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_SHORT).show();
                                    break;

                                case "D11 ON":
                                    Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_SHORT).show();
                                    break;

                                case "D11 OFF":
                                    Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_SHORT).show();
                                    break;

                                case "D12 ON":
                                    Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_SHORT).show();
                                    break;

                                case "D12 OFF":
                                    Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_SHORT).show();
                                    break;

                                case "D13 ON":
                                    Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_SHORT).show();
                                    break;

                                case "D13 OFF":
                                    Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_SHORT).show();
                                    break;

                                default:
                                    break;
                            }
                        });
                    }
                } catch (IOException e) {
                    logger.warning(e.getMessage()); // Мы же не хотим потом гадать, что за исключение прокинулось
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        }

    }
}
