package com.feiniu.lifeai.ble;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.feiniu.lifeai.comm.BaseApplication;
import com.feiniu.lifeai.comm.Constant;
import com.feiniu.lifeai.utils.SpUtils;
import com.feiniu.lifeai.view.ConnBleOperListener;
import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.veepoo.protocol.listener.base.IABleConnectStatusListener;
import com.veepoo.protocol.listener.base.IConnectResponse;
import com.veepoo.protocol.listener.base.INotifyResponse;


/**
 * Created by Admin
 * Date 2019/11/4
 */
public class BleConnStatusService extends Service {

    private static final String TAG = "BleConnStatusService";

    private IBinder iBinder = new B31Loader();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothClient bluetoothClient;

    private BleConnDataOperate bleConnDataOperate;

    private AudioManager audioManager;


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0x01:  //开启自动重连
                    handler.removeMessages(0x01);
                    String savedMac = (String) SpUtils.getParam(BaseApplication.getBaseApplication(), Constant.CONN_BLE_MAC,"");
                    Log.e(TAG,"--------saveMac="+savedMac);
                    if(TextUtils.isEmpty(savedMac)) {
                        return;
                    }
                    autoConnBle(true);
                    break;
                case 1001:  //自动搜索设备返回
                    handler.removeMessages(1001);
                    String savedMacs = (String) SpUtils.getParam(BaseApplication.getBaseApplication(), Constant.CONN_BLE_MAC,"");
                    if(TextUtils.isEmpty(savedMacs))
                        return;
                    SearchResult searchResult = (SearchResult) msg.obj;
                    if(searchResult == null || searchResult.getAddress() == null)
                        return;
                    Log.e(TAG,"--------自动重连搜索设备="+searchResult.getAddress());
                    if(savedMacs.equals(searchResult.getAddress().trim())){
                        if (bluetoothClient != null) {
                            bluetoothClient.stopSearch();
                        }
                        String pwdStr = (String) SpUtils.getParam(BaseApplication.getBaseApplication(), Constant.DEVICE_PWD_KEY,"0000");
                        connBleB31Device(searchResult.getName(),searchResult.getAddress(),pwdStr);
                    }

                    break;
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleConnDataOperate.NEXT_MUSIC_ACTION);
        intentFilter.addAction(BleConnDataOperate.PREVIOUS_MUSIC_ACTION);
        intentFilter.addAction(BleConnDataOperate.PREVIOUS_MUSIC_ACTION);
        registerReceiver(broadcastReceiver,intentFilter);
        initBle();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    }


    private void initBle() {
        bluetoothClient = new BluetoothClient(BaseApplication.getBaseApplication());
        BluetoothManager bluetoothManager = (BluetoothManager) BaseApplication.getBaseApplication().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    /**
     * 连接设备
     * @param bName 蓝牙名字
     * @param bMac  蓝牙Mac地址
     */
    public void connBleB31Device(final String bName, final String bMac, final String pwd){
        Log.e(TAG,"-------conn---bMac="+bMac+"--=name="+bName);
        BaseApplication.getVPOperateManager().registerConnectStatusListener(bMac,bleConnectStatusListener);
        BaseApplication.getVPOperateManager().connectDevice(bMac, new IConnectResponse() {
            @Override
            public void connectState(int i, BleGattProfile bleGattProfile, boolean b) {
                if (i == Code.REQUEST_SUCCESS) {  //连接成功过
                    if (bluetoothClient != null) {
                        bluetoothClient.stopSearch();
                    }
                }
            }
        }, new INotifyResponse() {  //设置通知成功，可以交换数据了
            @Override
            public void notifyState(final int i) {
                Log.e(TAG,"------设置通知="+i);

                if(i == Code.REQUEST_SUCCESS){  //设置通知成功
                    if(bleConnDataOperate == null)
                        bleConnDataOperate = BleConnDataOperate.getBleConnDataOperate();
                    bleConnDataOperate.setConnBleOperListener(new ConnBleOperListener() {
                        @Override
                        public void onBleConnSuccess() {
                            Log.e(TAG,"-------连接成功了------");
                            BaseApplication.getBaseApplication().setBleMac(bMac);
                            SpUtils.setParam(BaseApplication.getBaseApplication(), Constant.CONN_BLE_MAC,bMac);
                            SpUtils.setParam(BaseApplication.getBaseApplication(), Constant.CONN_BLE_NAME,bName);
                            BleConnStatus.CONNDEVICENAME = bName;
                            Intent intent = new Intent();
                            intent.setAction(Constant.DEVICE_CONNECT_ACTION);
                            sendBroadcast(intent);

                        }

                        @Override
                        public void onBleConnErrorPwd() {   //密码验证失败，提示再次输入密码
                            Intent intent = new Intent();
                            intent.setAction(Constant.DEVICE_INPUT_PWD_CODE);
                            intent.putExtra("bleName",bName);
                            intent.putExtra("bleMac",bMac);
                            sendBroadcast(intent);
                        }
                    });
                    bleConnDataOperate.connDeviceOperate(pwd);

                }
            }
        });


    }

    //验证密码再次连接
    public void continueConnBle(String pwd, ConnBleOperListener connBleOperListener){
       // BaseApplication.getVPOperateManager().registerConnectStatusListener(bMac,bleConnectStatusListener);
        bleConnDataOperate.continueConnBle(pwd,connBleOperListener);
    }


    //断开
    public void disBleConn(){
        BleConnDataOperate.getBleConnDataOperate().disBleConn();
    }

    //停止扫描
    public void stopScan(){
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            return;
        if(bluetoothClient == null)
            return;
        bluetoothClient.stopSearch();

    }


    //自动重连
    public void autoConnBle(boolean isAuto){
        if(!isAuto)
            return;
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            return;
        SearchRequest request = (new SearchRequest.Builder())
                .searchBluetoothLeDevice(Integer.MAX_VALUE,2)
                .build();
        bluetoothClient.search(request, new SearchResponse() {
            @Override
            public void onSearchStarted() {

            }

            @Override
            public void onDeviceFounded(SearchResult searchResult) {
                if(searchResult == null || searchResult.getAddress() == null)
                    return;
                Message message = handler.obtainMessage();
                message.what = 1001;
                message.obj = searchResult;
                handler.sendMessage(message);
            }

            @Override
            public void onSearchStopped() {

            }

            @Override
            public void onSearchCanceled() {

            }
        });

    }



    //监听连接状态回调
    private IABleConnectStatusListener bleConnectStatusListener = new IABleConnectStatusListener(){

        @Override
        public void onConnectStatusChanged(String s, int status) {
            switch (status){
                case Constants.STATUS_CONNECTED:    //连接成功

                    break;
                case Constants.STATUS_DISCONNECTED:     //连接断开
                    BleConnStatus.CONNDEVICENAME = null;
                    Intent intent = new Intent();
                    intent.setAction(Constant.DEVICE_DISANCE_ACTION);
                    sendBroadcast(intent);
                    handler.sendEmptyMessage(0x01);
                    break;
            }
        }
    };



    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }


    public class B31Loader extends Binder{
        public BleConnStatusService getBleService(){
            return BleConnStatusService.this;
        }
    }


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == null)
                return;
            Log.e(TAG,"------action="+action);
            if(action.equals(BleConnDataOperate.NEXT_MUSIC_ACTION)){    //下一首
                nextMusic();
            }

            if(action.equals(BleConnDataOperate.PREVIOUS_MUSIC_ACTION)){    //上一首
                previousMusic();
            }

            if(action.equals(BleConnDataOperate.PLAYORSTOP_MUSIC_ACTION)){  //播放或暂停
                if(audioManager == null)
                    return;
                if(audioManager.isMusicActive()){
                    pauseMusic();
                }else{
                    playMusic();
                }
            }
        }
    };




    //下一首音乐
    private void nextMusic(){
        try {
            if(audioManager != null){
                long eventTime2 = SystemClock.uptimeMillis() - 1;
                KeyEvent downEvent2 = new KeyEvent(eventTime2,eventTime2,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_NEXT, 0);
                audioManager.dispatchMediaKeyEvent(downEvent2);

                Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent upEvent = new KeyEvent(eventTime2, eventTime2, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
                upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                sendOrderedBroadcast(upIntent, null);
                audioManager.dispatchMediaKeyEvent(upEvent);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    //上一首音乐
    private void previousMusic(){
        try {
            if(audioManager != null){
                long eventTime2 = SystemClock.uptimeMillis() - 1;
                KeyEvent downEvent2 = new KeyEvent(eventTime2,eventTime2,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
                audioManager.dispatchMediaKeyEvent(downEvent2);

                Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent upEvent = new KeyEvent(eventTime2, eventTime2, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
                upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                sendOrderedBroadcast(upIntent, null);
                audioManager.dispatchMediaKeyEvent(upEvent);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    //暂停音乐
    private void pauseMusic(){

        try {
            if(audioManager != null){
                long eventTime2 = SystemClock.uptimeMillis() - 1;
                KeyEvent downEvent2 = new KeyEvent(eventTime2,eventTime2,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                audioManager.dispatchMediaKeyEvent(downEvent2);

                Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent upEvent = new KeyEvent(eventTime2, eventTime2, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                sendOrderedBroadcast(upIntent, null);
                audioManager.dispatchMediaKeyEvent(upEvent);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    //播放音乐
    private void playMusic(){
        try {
            if(audioManager != null){
                long eventTime2 = SystemClock.uptimeMillis() - 1;
                KeyEvent downEvent2 = new KeyEvent(eventTime2,eventTime2,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY, 0);
                audioManager.dispatchMediaKeyEvent(downEvent2);

                Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent upEvent = new KeyEvent(eventTime2, eventTime2, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
                upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                sendOrderedBroadcast(upIntent, null);
                audioManager.dispatchMediaKeyEvent(upEvent);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if(broadcastReceiver != null)
                unregisterReceiver(broadcastReceiver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
