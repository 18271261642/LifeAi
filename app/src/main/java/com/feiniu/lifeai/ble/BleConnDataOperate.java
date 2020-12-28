package com.feiniu.lifeai.ble;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.feiniu.lifeai.bean.BraceCommB31Db;
import com.feiniu.lifeai.bean.BraceCommDbInstance;
import com.feiniu.lifeai.bean.BraceHalfBpBean;
import com.feiniu.lifeai.bean.BraceHalfHeartBean;
import com.feiniu.lifeai.bean.BraceHalfHourSportBean;
import com.feiniu.lifeai.bean.BracePrecisionSleepBean;
import com.feiniu.lifeai.bean.BraceSleepBean;
import com.feiniu.lifeai.bean.CusVPTimeData;
import com.feiniu.lifeai.bean.SportBasicData;
import com.feiniu.lifeai.comm.BaseApplication;
import com.feiniu.lifeai.comm.Constant;
import com.feiniu.lifeai.spo2andhrv.bpoxy.uploadSpo2.AnalysisSpo2DataTask;
import com.feiniu.lifeai.spo2andhrv.bpoxy.uploadSpo2.ReadSpo2AndHrvAsyTask;
import com.feiniu.lifeai.utils.BraceUtils;
import com.feiniu.lifeai.utils.SpUtils;
import com.feiniu.lifeai.view.ConnBleOperListener;
import com.feiniu.lifeai.view.OnCurrentCountStepsListener;
import com.feiniu.lifeai.view.OnDataCompleteListener;
import com.google.gson.Gson;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.data.IAlarm2DataListListener;
import com.veepoo.protocol.listener.data.IAllHealthDataListener;
import com.veepoo.protocol.listener.data.IBatteryDataListener;
import com.veepoo.protocol.listener.data.ICustomSettingDataListener;
import com.veepoo.protocol.listener.data.IDeviceControlPhoneModelState;
import com.veepoo.protocol.listener.data.IDeviceFuctionDataListener;
import com.veepoo.protocol.listener.data.ILanguageDataListener;
import com.veepoo.protocol.listener.data.IOriginData3Listener;
import com.veepoo.protocol.listener.data.IPersonInfoDataListener;
import com.veepoo.protocol.listener.data.IPwdDataListener;
import com.veepoo.protocol.listener.data.ISleepDataListener;
import com.veepoo.protocol.listener.data.ISocialMsgDataListener;
import com.veepoo.protocol.listener.data.ISportDataListener;
import com.veepoo.protocol.model.datas.AlarmData2;
import com.veepoo.protocol.model.datas.BatteryData;
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData;
import com.veepoo.protocol.model.datas.FunctionSocailMsgData;
import com.veepoo.protocol.model.datas.HRVOriginData;
import com.veepoo.protocol.model.datas.HalfHourBpData;
import com.veepoo.protocol.model.datas.HalfHourRateData;
import com.veepoo.protocol.model.datas.HalfHourSportData;
import com.veepoo.protocol.model.datas.LanguageData;
import com.veepoo.protocol.model.datas.OriginData;
import com.veepoo.protocol.model.datas.OriginData3;
import com.veepoo.protocol.model.datas.OriginHalfHourData;
import com.veepoo.protocol.model.datas.PersonInfoData;
import com.veepoo.protocol.model.datas.PwdData;
import com.veepoo.protocol.model.datas.SleepData;
import com.veepoo.protocol.model.datas.SleepPrecisionData;
import com.veepoo.protocol.model.datas.Spo2hOriginData;
import com.veepoo.protocol.model.datas.SportData;
import com.veepoo.protocol.model.datas.TimeData;
import com.veepoo.protocol.model.enums.EFunctionStatus;
import com.veepoo.protocol.model.enums.ELanguage;
import com.veepoo.protocol.model.enums.EOprateStauts;
import com.veepoo.protocol.model.enums.EPwdStatus;
import com.veepoo.protocol.model.enums.ESex;
import com.veepoo.protocol.model.settings.CustomSettingData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * 数据操作
 * Created by Admin
 * Date 2019/11/4
 */
public class BleConnDataOperate {

    private static final String TAG = "BleConnDataOperate";

    //下一首音乐
    public static final String NEXT_MUSIC_ACTION = "com.brace.android.music_next";
    //上一首音乐
    public static final String PREVIOUS_MUSIC_ACTION = "com.brace.android.music_previous";
    //播放或者暂停
    public static final String PLAYORSTOP_MUSIC_ACTION = "com.brace.android.music_play_stop";


    private static volatile BleConnDataOperate bleConnDataOperate;

    //连接成功
    private ConnBleOperListener connBleOperListener;
    //当前设备总步数
    private OnCurrentCountStepsListener currentCountStepsListener;

    private Gson gson = new Gson();

    //设备的版本协议
    //第一代为0，不支持精准睡眠；支持精准睡眠的为3
    private int deviceVersion = 0;

    //是否支持血氧和HRV功能
    private boolean isSupportSpo2 = false;
    //是否支持精准睡眠
    private boolean isSleepPrecisionData = false;

    private OnDataCompleteListener onDataCompleteListener;


    //普通睡眠处理map
    private Map<String, BraceSleepBean> sleepMap = new HashMap<>();
    //精准睡眠的map
    private ConcurrentMap<String, BracePrecisionSleepBean> precisionSleepMap = new ConcurrentHashMap<>();


    //处理spo2数据
    private AnalysisSpo2DataTask analysisSpo2DataTask;
    private ReadSpo2AndHrvAsyTask readSpo2AndHrvAsyTask;


    //保存spo2的task
    private B31SaveSpo2AsyncTask b31SaveSpo2AsyncTask;
    //保存hrv的task
    private B31sSaveHrvAsyncTask b31sSaveHrvAsyncTask;


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            handler.removeMessages(0x01);
            Log.e(TAG, "-------睡眠map=" + sleepMap.size());
            //普通睡眠
            if (sleepMap != null && !sleepMap.isEmpty()) {
                for (Map.Entry<String, BraceSleepBean> mp : sleepMap.entrySet()) {
                    //保存详细数据 ，保存详细数据时日期会往后+ 一天
                    BraceCommB31Db db = new BraceCommB31Db();
                    db.setAddressStr(BaseApplication.getBaseApplication().getBleMac());
                    db.setRtcStr(BraceUtils.obtainAroundDate(mp.getValue().getDate(), false));
                    db.setType(Constant.DB_TYPE_GENERAL_SLEEP);
                    db.setDataSourceStr(gson.toJson(mp.getValue()));
                    db.setIsUploadStatus(0);
                    BraceCommDbInstance.getBraceCommDbInstance().saveDataSoreceData(db);
                }
            }


            //精准睡眠
            if (precisionSleepMap != null && !precisionSleepMap.isEmpty()) {
                Log.e(TAG, "------精准睡眠的map=" + precisionSleepMap.size());
                for (Map.Entry<String, BracePrecisionSleepBean> mmp : precisionSleepMap.entrySet()) {
                    //保存详细数据 ，保存详细数据时日期会往后+ 一天
                    Log.e(TAG, "------保存精准睡眠=" + mmp.toString() + "--=" + mmp.getValue().getSleepLine());
                    BraceCommB31Db db = new BraceCommB31Db();
                    db.setAddressStr(BaseApplication.getBaseApplication().getBleMac());
                    db.setRtcStr(mmp.getKey());
                    db.setType(Constant.DB_TYPE_PRECISION_SLEEP);
                    db.setDataSourceStr(gson.toJson(mmp.getValue()));
                    db.setIsUploadStatus(0);
                    BraceCommDbInstance.getBraceCommDbInstance().saveDataSoreceData(db);
                }
            }


            new ReadAllDataAsync().execute();
        }
    };




    private BleConnDataOperate() {

    }

    public static BleConnDataOperate getBleConnDataOperate(){
        if(bleConnDataOperate == null){
            synchronized (BleConnDataOperate.class){
                if(bleConnDataOperate == null){
                    bleConnDataOperate = new BleConnDataOperate();
                }
            }
        }
        return bleConnDataOperate;
    }


    private VPOperateManager getVpManager(){
        return BaseApplication.getVPOperateManager();
    }


    /**
     * 设置用户的基本信息，在连接设备前设置
     * @param sex 性别 0-男;1-女
     * @param height 身高 cm 整形
     * @param weight 体重 kg 整形
     * @param age 年龄 整形
     */
    public void setBasicMsgData(int sex,int height,int weight,int age){
        Context mContext = BaseApplication.getBaseApplication();
        SpUtils.setParam(mContext, Constant.USER_SEX,sex);
        SpUtils.setParam(mContext, Constant.USER_HEIGHT,height);
        SpUtils.setParam(mContext, Constant.USER_WEIGHT,weight);
        SpUtils.setParam(mContext, Constant.USER_AGE,age);
    }



    //验证密码操作，验证密码成功可表示连接成功，可进行数据的读取
    public void connDeviceOperate(final String pwd){
        getVpManager().confirmDevicePwd(iBleWriteResponse, new IPwdDataListener() {
            @Override
            public void onPwdDataChange(PwdData pwdData) {      //验证密码返回
                if(pwdData.getmStatus() == EPwdStatus.CHECK_AND_TIME_SUCCESS){  //密码验证成功
                    SpUtils.setParam(BaseApplication.getBaseApplication(), Constant.DEVICE_PWD_KEY,pwd);
                    SpUtils.setParam(BaseApplication.getBaseApplication(), Constant.DEVICE__VERSION_NUMBER,pwdData.getDeviceNumber()+"-"+pwdData.getDeviceVersion());

                    if(connBleOperListener != null)
                        connBleOperListener.onBleConnSuccess();
                }


                if(pwdData.getmStatus() == EPwdStatus.CHECK_FAIL){  //密码验证失败
                    if(connBleOperListener != null)
                        connBleOperListener.onBleConnErrorPwd();
                }
            }
        }, new IDeviceFuctionDataListener() {   //设备支持的功能
            @Override
            public void onFunctionSupportDataChange(FunctionDeviceSupportData functionDeviceSupportData) {
                Log.e(TAG,"-----connDeviceOperate="+functionDeviceSupportData.toString());
                getCommData(functionDeviceSupportData);
            }
        }, new ISocialMsgDataListener() {   //消息提醒开关状态
            @Override
            public void onSocialMsgSupportDataChange(FunctionSocailMsgData functionSocailMsgData) {

            }
        }, new ICustomSettingDataListener() {   //自定义的开关
            @Override
            public void OnSettingDataChange(CustomSettingData customSettingData) {

            }
        },pwd,true);
    }


    public void continueConnBle(final String pwd, final ConnBleOperListener connBleOperListener){
        getVpManager().confirmDevicePwd(iBleWriteResponse, new IPwdDataListener() {
            @Override
            public void onPwdDataChange(PwdData pwdData) {      //验证密码返回

                Log.e(TAG,"-------pwd="+pwdData.toString());
                SpUtils.setParam(BaseApplication.getBaseApplication(), Constant.DEVICE_PWD_KEY,pwd);
                SpUtils.setParam(BaseApplication.getBaseApplication(), Constant.DEVICE__VERSION_NUMBER,pwdData.getDeviceNumber()+"-"+pwdData.getDeviceVersion());

                if(pwdData.getmStatus() == EPwdStatus.CHECK_AND_TIME_SUCCESS){  //密码验证成功
                    if(connBleOperListener != null)
                        connBleOperListener.onBleConnSuccess();
                }


                if(pwdData.getmStatus() == EPwdStatus.CHECK_FAIL){  //密码验证失败
                    if(connBleOperListener != null)
                        connBleOperListener.onBleConnErrorPwd();
                }
            }
        }, new IDeviceFuctionDataListener() {   //设备支持的功能
            @Override
            public void onFunctionSupportDataChange(FunctionDeviceSupportData functionDeviceSupportData) {
                Log.e(TAG,"-----continueConnBle="+functionDeviceSupportData.toString());


                getCommData(functionDeviceSupportData);

            }
        }, new ISocialMsgDataListener() {   //消息提醒开关状态
            @Override
            public void onSocialMsgSupportDataChange(FunctionSocailMsgData functionSocailMsgData) {

            }
        }, new ICustomSettingDataListener() {   //自定义的开关
            @Override
            public void OnSettingDataChange(CustomSettingData customSettingData) {

            }
        },pwd,true);
    }

    //公共数据
    private void getCommData(FunctionDeviceSupportData functionDeviceSupportData){

        Log.e(TAG,"-------公共数据="+functionDeviceSupportData.toString());

        Context context = BaseApplication.getBaseApplication();
        deviceVersion = functionDeviceSupportData.getOriginProtcolVersion();
        int deviceStyleCoount = functionDeviceSupportData.getScreenstyle();
        SpUtils.setParam(context, Constant.SP_DEVICE_STYLE_COUNT,deviceStyleCoount);
        SpUtils.setParam(context, Constant.DEVICE_VERSION_KEY,deviceVersion);
        //是否支持血氧
        isSupportSpo2 = functionDeviceSupportData.getSpo2H() == EFunctionStatus.SUPPORT;
        SpUtils.setParam(context,Constant.IS_SUPPORT_SPO2,isSupportSpo2);

        //是否支持精准睡眠
        isSleepPrecisionData = functionDeviceSupportData.getPrecisionSleep() == EFunctionStatus.SUPPORT;
        //是否支持倒计时
        SpUtils.setParam(context,Constant.IS_SUPPORT_COUNT_DOWM,functionDeviceSupportData.getCountDown() == EFunctionStatus.SUPPORT);
        //是否支持血压
        SpUtils.setParam(context,Constant.IS_SUPPORT_BP,functionDeviceSupportData.getBp() == EFunctionStatus.SUPPORT);
        //是否支持疲劳度检测
        SpUtils.setParam(context,Constant.IS_SUPPORT_FATIGUE,functionDeviceSupportData.getFatigue() == EFunctionStatus.SUPPORT);
        //是否支持呼吸率检测
        SpUtils.setParam(context,Constant.IS_SUPPORT_HEART_CHECK,functionDeviceSupportData.getBeathFunction() == EFunctionStatus.SUPPORT);
        //是否支持摇一摇拍照
        SpUtils.setParam(context,Constant.IS_SUPPORT_CAMERA,functionDeviceSupportData.getCamera() == EFunctionStatus.SUPPORT);
        //是否支持亮屏时长
        SpUtils.setParam(context,Constant.IS_SUPPORT_SCREEN_LIGHT_TIME,functionDeviceSupportData.getScreenLightTime() == EFunctionStatus.SUPPORT);
    }


    public void  disBleConn(){
        getVpManager().disconnectWatch(iBleWriteResponse);
    }



    //同步用户信息，此方法必须在 setBasicMsgData 方法之后
    public void syncUserInfoData(){
        Context mContext = BaseApplication.getBaseApplication();
        int sexCode = (int) SpUtils.getParam(mContext, Constant.USER_SEX,0);
        int height = (int) SpUtils.getParam(mContext, Constant.USER_HEIGHT,175);
        int weight = (int) SpUtils.getParam(mContext, Constant.USER_WEIGHT,60);
        int age = (int) SpUtils.getParam(mContext, Constant.USER_AGE,25);

        int goal = (int) SpUtils.getParam(mContext, Constant.DEVICE_SPORT_GOAL, 0);



        ELanguage eLanguage;
        //同步手环的语言，中文或英文
        String locals = Locale.getDefault().getLanguage();
        if(!TextUtils.isEmpty(locals) && locals.equals("zh")){
            eLanguage = ELanguage.CHINA;
        }else{
            eLanguage = ELanguage.ENGLISH;
        }
        getVpManager().settingDeviceLanguage(iBleWriteResponse, new ILanguageDataListener() {
            @Override
            public void onLanguageDataChange(LanguageData languageData) {

            }
        }, eLanguage);


        //同步用户信息
        getVpManager().syncPersonInfo(iBleWriteResponse, new IPersonInfoDataListener() {
            @Override
            public void OnPersoninfoDataChange(EOprateStauts eOprateStauts) {
                getDeviceBasicData();
            }
        }, new PersonInfoData(sexCode == 0 ? ESex.MAN : ESex.WOMEN,height,weight,age,goal));
    }




    //验证秘密成功后同步用户信息,建议连接成功后同步一次用户信息
    /**
     *
     * @param eSexCode  性别
     * @param userHeight    身高
     * @param userWeight    体重
     * @param userAge   年龄
     * @param goalStep  目标步数
     */
    public void syncUserInfoData(int eSexCode, int userHeight, int userWeight, int userAge, int goalStep){

        ELanguage eLanguage;
        //同步手环的语言，中文或英文
        String locals = Locale.getDefault().getLanguage();
        if(!TextUtils.isEmpty(locals) && locals.equals("zh")){
            eLanguage = ELanguage.CHINA;
        }else{
            eLanguage = ELanguage.ENGLISH;
        }
        getVpManager().settingDeviceLanguage(iBleWriteResponse, new ILanguageDataListener() {
            @Override
            public void onLanguageDataChange(LanguageData languageData) {

            }
        }, eLanguage);


        //同步用户信息
        getVpManager().syncPersonInfo(iBleWriteResponse, new IPersonInfoDataListener() {
            @Override
            public void OnPersoninfoDataChange(EOprateStauts eOprateStauts) {
                getDeviceBasicData();
            }
        }, new PersonInfoData(eSexCode == 0 ? ESex.MAN : ESex.WOMEN,userHeight,userWeight,userAge,goalStep));

        setMusicStatusListener();

    }



    private void setMusicStatusListener(){
        Log.e(TAG,"------设置音乐控制------");
        getVpManager().settingDeviceControlPhone(new IDeviceControlPhoneModelState() {
            @Override
            public void knocknotify(int i) {

            }

            @Override
            public void nextMusic() {   //下一首音乐
                Log.e(TAG,"-------下一首音乐-----");
                sendAction(NEXT_MUSIC_ACTION);
            }

            @Override
            public void previousMusic() {   //上一首音乐
                Log.e(TAG,"-------上一首音乐-----");
                sendAction(PREVIOUS_MUSIC_ACTION);
            }

            @Override
            public void pauseAndPlayMusic() {   //播放或暂停音乐
                Log.e(TAG,"-------暂停音乐-----");
//                sendAction(PLAYORSTOP_MUSIC_ACTION);
            }

            @Override
            public void pauseMusic() {
                Log.e(TAG,"-------pauseMusic-----");
            }

            @Override
            public void playMusic() {
                Log.e(TAG,"-------playMusic-----");
                sendAction(PLAYORSTOP_MUSIC_ACTION);
            }

            @Override
            public void voiceUp() {
                Log.e(TAG,"-------voiceUp-----");
            }

            @Override
            public void voiceDown() {
                Log.e(TAG,"-------voiceDown-----");
            }

            @Override
            public void rejectPhone() {

            }

            @Override
            public void cliencePhone() {

            }

            @Override
            public void inPttModel() {

            }

            @Override
            public void outPttModel() {

            }

            @Override
            public void sos() {

            }
        });
    }




    //获取计步
    public void getDeviceBasicData(){
        //获取当天的汇总步数
        getVpManager().readSportStep(iBleWriteResponse, new ISportDataListener() {
            @Override
            public void onSportDataChange(SportData sportData) {
                Log.e(TAG,"-----sport="+sportData.toString());
                SportBasicData sportBasicData = new SportBasicData(sportData.getStep(),sportData.getKcal(),sportData.getDis());
                BraceCommB31Db braceCommB31Db = new BraceCommB31Db();
                braceCommB31Db.setRtcStr(BraceUtils.getCurrentDate());
                braceCommB31Db.setDataSourceStr(gson.toJson(sportBasicData));
                braceCommB31Db.setAddressStr(BaseApplication.getBaseApplication().getBleMac());
                braceCommB31Db.setType(Constant.DB_TYPE_SPTES);
                braceCommB31Db.setIsUploadStatus(0);

                BraceCommDbInstance.getBraceCommDbInstance().saveDataSoreceData(braceCommB31Db);

                if(currentCountStepsListener != null)
                    currentCountStepsListener.currentCountSteps(new SportBasicData(sportData.getStep(),sportData.getKcal(),sportData.getDis()));
            }
        });

        //电量
        getVpManager().readBattery(iBleWriteResponse, new IBatteryDataListener() {
            @Override
            public void onDataChange(BatteryData batteryData) {

            }
        });

        //读取闹钟
        getVpManager().readAlarm2(iBleWriteResponse, new IAlarm2DataListListener() {
            @Override
            public void onAlarmDataChangeListListener(AlarmData2 alarmData2) {

            }
        });

    }


    /**
     * 读取所有的健康数据，先读取睡眠，睡眠读取完成后再读取心率、步数等
     * @param isToday 是否只读当天的数据
     */
    public void readAllDeviceData(final boolean isToday){
        if (sleepMap != null)
            sleepMap.clear();
        if(precisionSleepMap != null)
            precisionSleepMap.clear();
        int vpOrignVersion = (int) SpUtils.getParam(BaseApplication.getBaseApplication(), Constant.DEVICE_VERSION_KEY,0);

        if(vpOrignVersion == 0){    //旧版
            getVpManager().readAllHealthData(iAllHealthDataListener,isToday ? 2 : 3);
        }else{  //新版协议
            readOnlySleep(isToday);
        }
    }



    //只读取睡眠
    private void readOnlySleep(boolean day){
        if (sleepMap != null)
            sleepMap.clear();
        if(precisionSleepMap != null)
            precisionSleepMap.clear();
        //单单读取睡眠的
        getVpManager().readSleepData(iBleWriteResponse, new ISleepDataListener() {
            @Override
            public void onSleepDataChange(SleepData sleepData) {
                if (sleepData == null)
                    return;
                //Log.e(TAG, "-----22----睡眠原始返回数据=" + sleepData.toString());
                //saveSleepData(sleepData);
                if(sleepData instanceof SleepPrecisionData && isSleepPrecisionData){
                    SleepPrecisionData sleepPrecisionData = (SleepPrecisionData) sleepData;
                    //Log.e(TAG,"----22--精准睡眠="+sleepPrecisionData.toString()+"--表示形式="+sleepPrecisionData.getSleepLine());
                    savePrecisionData(sleepPrecisionData);
                }else{
                    // 睡眠数据返回,会有多条数据
                    saveGeneralSleep(sleepData);
                }

            }

            @Override
            public void onSleepProgress(float v) {
                //Log.e(TAG,"---------onSleepProgress="+v);
            }

            @Override
            public void onSleepProgressDetail(String s, int i) {
                //Log.e(TAG,"-------onSleepProgressDetail="+s+"--i="+i);
            }

            @Override
            public void onReadSleepComplete() {
                handler.sendEmptyMessageDelayed(1001,3 * 1000);
            }
        }, day ? 2 : 3);
    }




    /**
     * 保存精准睡眠
     * @param sleepPrecisionData 精准睡眠
     */
    private void savePrecisionData(SleepPrecisionData sleepPrecisionData) {
        if(sleepPrecisionData == null)
            return;
        Log.e(TAG,"--------精准睡眠原始数据="+sleepPrecisionData.toString()+"="+sleepPrecisionData.getSleepLine());
        TimeData downTimeData = sleepPrecisionData.getSleepDown();
        CusVPTimeData donwTime = new CusVPTimeData(downTimeData.getYear(),downTimeData.getMonth(),
                downTimeData.getDay(),downTimeData.getHour(),downTimeData.getMinute(),
                downTimeData.getSecond(),downTimeData.getWeekDay());

        TimeData upTimeData = sleepPrecisionData.getSleepUp();
        CusVPTimeData upTime = new CusVPTimeData(upTimeData.getYear(),upTimeData.getMonth(),
                upTimeData.getDay(),upTimeData.getHour(),upTimeData.getMinute(),
                upTimeData.getSecond(),upTimeData.getWeekDay());

        BracePrecisionSleepBean cSleep = new BracePrecisionSleepBean();

        cSleep.setDate(sleepPrecisionData.getDate());
        cSleep.setSleepDown(donwTime);
        cSleep.setSleepUp(upTime);
        cSleep.setAllSleepTime(sleepPrecisionData.getAllSleepTime());
        cSleep.setLowSleepTime(sleepPrecisionData.getLowSleepTime());
        cSleep.setDeepSleepTime(sleepPrecisionData.getDeepSleepTime());
        cSleep.setWakeCount(sleepPrecisionData.getWakeCount());
        cSleep.setSleepQulity(sleepPrecisionData.getSleepQulity());


        cSleep.setSleepTag(sleepPrecisionData.getSleepTag());
        cSleep.setGetUpScore(sleepPrecisionData.getGetUpScore());
        cSleep.setDeepScore(sleepPrecisionData.getDeepScore());
        cSleep.setSleepEfficiencyScore(sleepPrecisionData.getSleepEfficiencyScore());
        cSleep.setFallAsleepScore(sleepPrecisionData.getFallAsleepScore());
        cSleep.setSleepTimeScore(sleepPrecisionData.getSleepTimeScore());
        cSleep.setExitSleepMode(sleepPrecisionData.getExitSleepMode());
        cSleep.setDeepAndLightMode(sleepPrecisionData.getDeepAndLightMode());
        cSleep.setOtherDuration(sleepPrecisionData.getOtherDuration());
        cSleep.setFirstDeepDuration(sleepPrecisionData.getFirstDeepDuration());
        cSleep.setGetUpDuration(sleepPrecisionData.getGetUpDuration());
        cSleep.setGetUpToDeepAve(sleepPrecisionData.getGetUpToDeepAve());
        cSleep.setOnePointDuration(sleepPrecisionData.getOnePointDuration());
        cSleep.setAccurateType(sleepPrecisionData.getAccurateType());
        cSleep.setInsomniaTag(sleepPrecisionData.getInsomniaTag());
        cSleep.setInsomniaScore(sleepPrecisionData.getInsomniaScore());
        cSleep.setInsomniaTimes(sleepPrecisionData.getInsomniaTimes());
        cSleep.setInsomniaLength(sleepPrecisionData.getInsomniaLength());
        cSleep.setInsomniaBeanList(sleepPrecisionData.getInsomniaBeanList());
        cSleep.setStartInsomniaTime(sleepPrecisionData.getStartInsomniaTime());
        cSleep.setStopInsomniaTime(sleepPrecisionData.getStopInsomniaTime());
        cSleep.setInsomniaDuration(sleepPrecisionData.getInsomniaDuration());
        cSleep.setSleepSourceStr(sleepPrecisionData.getSleepSourceStr());
        cSleep.setLaster(sleepPrecisionData.getLaster());
        cSleep.setNext(sleepPrecisionData.getNext());
        cSleep.setSleepLine(sleepPrecisionData.getSleepLine());



        String dateStr = sleepPrecisionData.getDate();
        Log.e(TAG,"---------dateStr="+dateStr);
        if(precisionSleepMap.get(dateStr) == null){
            precisionSleepMap.put(dateStr,cSleep);
        }else{
            //同一天的
            BracePrecisionSleepBean tmpCusVpSleep = precisionSleepMap.get(dateStr);
            if(tmpCusVpSleep == null)
                return;
            //入睡时间的分钟
            long tmpSleepDownTime = BraceUtils.formatDateToLong(tmpCusVpSleep.getSleepDown().getDateAndClockForSleepSecond());
            //同天分段的两段睡眠两个入睡时间是不相同的
            long cSleepDownTime = BraceUtils.formatDateToLong(cSleep.getSleepDown().getDateAndClockForSleepSecond());

            if(tmpSleepDownTime != cSleepDownTime){ //组合分段的睡眠数据
                BracePrecisionSleepBean resultSleepData = new BracePrecisionSleepBean();
                resultSleepData.setDate(dateStr);
                //判断哪一个是最后的时间
                //入睡时间
                resultSleepData.setSleepDown(tmpSleepDownTime>cSleepDownTime?cSleep.getSleepDown():tmpCusVpSleep.getSleepDown());
                //起床时间
                resultSleepData.setSleepUp(tmpSleepDownTime>cSleepDownTime?tmpCusVpSleep.getSleepUp():cSleep.getSleepUp());
                //所有睡眠时间
                resultSleepData.setAllSleepTime(tmpSleepDownTime>cSleepDownTime?tmpCusVpSleep.getAllSleepTime():cSleep.getAllSleepTime());
                //浅睡时间
                resultSleepData.setLowSleepTime(tmpSleepDownTime>cSleepDownTime?tmpCusVpSleep.getLowSleepTime():cSleep.getLowSleepTime());
                //深睡
                resultSleepData.setDeepSleepTime(tmpSleepDownTime>cSleepDownTime?tmpCusVpSleep.getDeepSleepTime():cSleep.getDeepSleepTime());
                //苏醒次数
                resultSleepData.setWakeCount(tmpSleepDownTime>cSleepDownTime?tmpCusVpSleep.getWakeCount():cSleep.getWakeCount());
                //睡眠质量
                resultSleepData.setSleepQulity(tmpCusVpSleep.getSleepQulity()>=cSleep.getSleepQulity()?tmpCusVpSleep.getSleepQulity():cSleep.getSleepQulity());


                resultSleepData.setSleepTag(tmpCusVpSleep.getSleepTag());
                //起夜得分
                resultSleepData.setGetUpScore(tmpCusVpSleep.getGetUpScore()>=cSleep.getGetUpScore()?tmpCusVpSleep.getGetUpScore():cSleep.getGetUpScore());
                //深睡夜得分
                resultSleepData.setDeepScore(tmpCusVpSleep.getDeepScore()>=cSleep.getDeepScore()?tmpCusVpSleep.getDeepScore():cSleep.getDeepScore());
                //睡眠效率得分
                resultSleepData.setSleepEfficiencyScore(tmpCusVpSleep.getSleepEfficiencyScore()>=cSleep.getSleepEfficiencyScore()?tmpCusVpSleep.getSleepEfficiencyScore():cSleep.getSleepEfficiencyScore());
                //睡眠时长得分
                resultSleepData.setSleepTimeScore(tmpCusVpSleep.getSleepTimeScore()>=cSleep.getSleepTimeScore()?tmpCusVpSleep.getSleepTimeScore():cSleep.getSleepTimeScore());
                //设置退出睡眠模式
                resultSleepData.setExitSleepMode(tmpCusVpSleep.getExitSleepMode()>=cSleep.getExitSleepMode()?tmpCusVpSleep.getExitSleepMode():cSleep.getExitSleepMode());
                //入睡效率得分
                resultSleepData.setFallAsleepScore(tmpCusVpSleep.getFallAsleepScore()>=cSleep.getFallAsleepScore()?tmpCusVpSleep.getFallAsleepScore():cSleep.getFallAsleepScore());
                //获得深浅睡模式
                resultSleepData.setDeepAndLightMode(tmpCusVpSleep.getDeepAndLightMode());
                //其它睡眠时长
                resultSleepData.setOtherDuration(tmpCusVpSleep.getOtherDuration()>=cSleep.getOtherDuration()?tmpCusVpSleep.getOtherDuration():cSleep.getOtherDuration());
                resultSleepData.setGetUpToDeepAve(tmpCusVpSleep.getGetUpToDeepAve());
                resultSleepData.setOnePointDuration(tmpCusVpSleep.getOnePointDuration());


                resultSleepData.setAccurateType(tmpCusVpSleep.getAccurateType());
                resultSleepData.setInsomniaTag(tmpCusVpSleep.getInsomniaTag());
                resultSleepData.setInsomniaScore(tmpCusVpSleep.getInsomniaScore());
                resultSleepData.setInsomniaTimes(tmpCusVpSleep.getInsomniaTimes());
                resultSleepData.setInsomniaLength(tmpCusVpSleep.getInsomniaLength());
                resultSleepData.setInsomniaBeanList(tmpCusVpSleep.getInsomniaBeanList());
                resultSleepData.setStartInsomniaTime(tmpCusVpSleep.getStartInsomniaTime());
                resultSleepData.setStopInsomniaTime(tmpCusVpSleep.getStopInsomniaTime());
                resultSleepData.setInsomniaDuration(tmpCusVpSleep.getInsomniaDuration());
                resultSleepData.setSleepSourceStr(tmpCusVpSleep.getSleepSourceStr());
                resultSleepData.setLaster(tmpCusVpSleep.getLaster());
                resultSleepData.setNext(tmpCusVpSleep.getNext());

                String sleepLinStr1 = tmpCusVpSleep.getSleepLine();
                String sleepLinStr2 = cSleep.getSleepLine();
                resultSleepData.setSleepLine(tmpSleepDownTime>cSleepDownTime?(sleepLinStr2+sleepLinStr1):(sleepLinStr1+sleepLinStr2));
                precisionSleepMap.put(dateStr,resultSleepData);

            }

        }


    }

    //保存普通睡眠
    private void saveGeneralSleep(SleepData sleepData) {
        if(sleepData == null)
            return;

        /***
         * 睡眠数据
         *
         * @param cali_flag     睡眠定标值，目前这个值没有什么用
         * @param sleepQulity   睡眠质量
         * @param wakeCount     睡眠中起床的次数
         * @param deepSleepTime 深睡时长
         * @param lowSleepTime  浅睡时长
         * @param allSleepTime  睡眠总时长
         * @param sleepLine      获取睡眠曲线，主要用于更具象化的UI来显示睡眠状态（可参考我司APP,360应用市场搜索Hband），如果您睡眠界面对UI没有特殊要求，可不理会,睡眠曲线分为普通睡眠和精准睡眠，普通睡眠是一组由0,1,2组成的字符串，每一个字符代表时长为5分钟，其中0表示浅睡，1表示深睡，2表示苏醒,比如“201112”，长度为6，表示睡眠阶段共30分钟，头尾各苏醒5分钟，中间浅睡5分钟，深睡15分钟;若是精准睡眠，睡眠曲线是一组由0,1,2，3,4组成的字符串，每一个字符代表时长为1分钟，其中0表示深睡，1表示浅睡，2表示快速眼动,3表示失眠,4表示苏醒。
         * @param sleepDown     入睡时间
         * @param sleepUp       起床时间
         */
        TimeData downTimeData = sleepData.getSleepDown();
        CusVPTimeData donwTime = new CusVPTimeData(downTimeData.getYear(),downTimeData.getMonth(),
                downTimeData.getDay(),downTimeData.getHour(),downTimeData.getMinute(),
                downTimeData.getSecond(),downTimeData.getWeekDay());

        TimeData upTimeData = sleepData.getSleepUp();
        CusVPTimeData upTime = new CusVPTimeData(upTimeData.getYear(),upTimeData.getMonth(),
                upTimeData.getDay(),upTimeData.getHour(),upTimeData.getMinute(),
                upTimeData.getSecond(),upTimeData.getWeekDay());

        BraceSleepBean deviceSstr = new BraceSleepBean(sleepData.getDate(),sleepData.getCali_flag(),sleepData.getSleepQulity(),
                sleepData.getWakeCount(),sleepData.getDeepSleepTime(),sleepData.getLowSleepTime(),sleepData.getAllSleepTime(),
                sleepData.getSleepLine(),donwTime,upTime);

        if (sleepMap.get(deviceSstr.getDate()) == null) {
            sleepMap.put(deviceSstr.getDate(), deviceSstr);
        } else {
            BraceSleepBean tempSleepData = sleepMap.get(deviceSstr.getDate());    //已经存在的数据
            Log.e(TAG, "------tempSleepData=" + tempSleepData.toString());
            if (tempSleepData.getDate().equals(deviceSstr.getDate())) {  //同一天的
                if (!tempSleepData.getSleepLine().equals(deviceSstr.getSleepLine())) {
                    //map 中已经保存的
                    //Log.e(TAG,"--------tempSleepData="+tempSleepData.toString());
                    // SleepData resultSlee = new SleepData();
                    BraceSleepBean resultSlee = new BraceSleepBean();


                    resultSlee.setDate(deviceSstr.getDate());    //日期
                    resultSlee.setCali_flag(0);
                    //睡眠质量，取最大值
                    resultSlee.setSleepQulity(tempSleepData.getSleepQulity() >= deviceSstr.getSleepQulity() ? tempSleepData.getSleepQulity() : deviceSstr.getSleepQulity());
                    //睡醒次数
                    resultSlee.setWakeCount(tempSleepData.getWakeCount() + deviceSstr.getWakeCount() + 1);
                    //深睡时间
                    resultSlee.setDeepSleepTime(tempSleepData.getDeepSleepTime() + deviceSstr.getDeepSleepTime());
                    //浅睡时间
                    resultSlee.setLowSleepTime(tempSleepData.getLowSleepTime() + deviceSstr.getLowSleepTime());
                    //入睡时间 比较时间大小
                    String time1 = tempSleepData.getSleepDown().getDateAndClockForSleepSecond();
                    String time2 = sleepData.getSleepDown().getDateAndClockForSleepSecond();
                    CusVPTimeData sleepDownTime = BraceUtils.comPariDateDetail(time2, time1) ? deviceSstr.getSleepDown() : tempSleepData.getSleepDown();
                    resultSlee.setSleepDown(sleepDownTime);
                    //清醒时间
                    String sleepUpStr1 = tempSleepData.getSleepUp().getDateAndClockForSleepSecond();
                    String sleepUpStr2 = sleepData.getSleepUp().getDateAndClockForSleepSecond();

                    CusVPTimeData sleepUpTime = BraceUtils.comPariDateDetail(sleepUpStr2, sleepUpStr1) ? tempSleepData.getSleepUp() : deviceSstr.getSleepUp();

                    resultSlee.setSleepUp(sleepUpTime);
                    //计算两段时间间隔，第二段的入睡时间-第一段的清醒时间
                    int sleepLen = BraceUtils.intervalTimeStr(sleepUpStr1, time2);
                    int sleepStatus = sleepLen / 5;
                    StringBuilder stringBuffer = new StringBuilder();
                    for (int i = 1; i <= sleepStatus; i++) {
                        stringBuffer.append("2");
                    }
                    //所有睡眠时间 = 几段的总的睡眠时间+清醒的时间
                    resultSlee.setAllSleepTime(Integer.valueOf(tempSleepData.getAllSleepTime()) + Integer.valueOf(deviceSstr.getAllSleepTime()) + (sleepStatus * 5));
                    resultSlee.setSleepLine(BraceUtils.comPariDateDetail(time1, time2) ?
                            (tempSleepData.getSleepLine() + stringBuffer + "" + deviceSstr.getSleepLine()) :
                            (deviceSstr.getSleepLine() + stringBuffer + "" + tempSleepData.getSleepLine()));
                    Log.e(TAG, "----------结果睡眠---=" + resultSlee.toString());
                    sleepMap.put(deviceSstr.getDate(), resultSlee);
                }
            }

        }

    }



    /**
     * 版本协议为0的数据回调，旧版协议
     */
    private IAllHealthDataListener iAllHealthDataListener = new IAllHealthDataListener() {
        @Override
        public void onProgress(float v) {

        }

        @Override
        public void onSleepDataChange(SleepData sleepData) {
            Log.e(TAG,"-----普通睡眠="+sleepData.toString());
            saveGeneralSleep(sleepData);
        }

        @Override
        public void onReadSleepComplete() {
            handler.sendEmptyMessageDelayed(1001,3 * 1000);
        }

        @Override
        public void onOringinFiveMinuteDataChange(OriginData originData) {

        }

        @Override
        public void onOringinHalfHourDataChange(OriginHalfHourData originHalfHourData) {
            if(originHalfHourData == null)
                return;
            saveHalfData(originHalfHourData);
        }

        @Override
        public void onReadOriginComplete() {
            if(onDataCompleteListener != null)
                onDataCompleteListener.dataReadComplete();
            if(isSupportSpo2){
                readSpo2Data();
            }
        }
    };


    //读取旧版血氧数据
    private void readSpo2Data() {
        try {
            if(readSpo2AndHrvAsyTask != null && readSpo2AndHrvAsyTask.getStatus() == AsyncTask.Status.RUNNING){
                readSpo2AndHrvAsyTask.cancel(true);
                readSpo2AndHrvAsyTask = null;
                readSpo2AndHrvAsyTask = new ReadSpo2AndHrvAsyTask();
            }else{
                readSpo2AndHrvAsyTask = new ReadSpo2AndHrvAsyTask();
            }
            readSpo2AndHrvAsyTask.execute();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    //保存健康数据
    private void saveHalfData(OriginHalfHourData originHalfHourData) {
        if(originHalfHourData == null)
            return;
        String bleMac = BaseApplication.getBaseApplication().getBleMac();
        if(bleMac == null)
            return;
        saveHalfBloodData(originHalfHourData.getHalfHourBps(),bleMac);
        saveHalfHeartData(originHalfHourData.getHalfHourRateDatas(),bleMac);
        saveHalfSportData(originHalfHourData.getHalfHourSportDatas(),bleMac);
    }

    /**
     * 保存详细的运动数据
     * @param halfHourSportDatas 运动详情
     * @param bleMac 地址
     */
    private void saveHalfSportData(List<HalfHourSportData> halfHourSportDatas, String bleMac) {
        if(halfHourSportDatas == null || halfHourSportDatas.isEmpty())
            return;
        List<BraceHalfHourSportBean> sportList = new ArrayList<>();
        for(HalfHourSportData hourSportData : halfHourSportDatas){
            CusVPTimeData cusVPTimeData = new CusVPTimeData(hourSportData.getTime().getYear(),hourSportData.getTime().getMonth(),hourSportData.getTime().getDay(),
                    hourSportData.getTime().getHour(),hourSportData.getTime().getMinute(),hourSportData.getTime().getSecond(),hourSportData.getTime().getWeekDay());
            BraceHalfHourSportBean braceHalfHourSportBean = new BraceHalfHourSportBean();
            braceHalfHourSportBean.setDate(hourSportData.getDate());
            braceHalfHourSportBean.setCalValue(hourSportData.getCalValue());
            braceHalfHourSportBean.setDisValue(hourSportData.getDisValue());
            braceHalfHourSportBean.setStepValue(hourSportData.getStepValue());
            braceHalfHourSportBean.setTime(cusVPTimeData);
            sportList.add(braceHalfHourSportBean);
        }

        BraceCommB31Db braceCommB31Db = new BraceCommB31Db();
        braceCommB31Db.setRtcStr(halfHourSportDatas.get(0).getDate());
        braceCommB31Db.setType(Constant.DB_TYPE_SPORT);
        braceCommB31Db.setAddressStr(bleMac);
        braceCommB31Db.setDataSourceStr(gson.toJson(sportList));
        braceCommB31Db.setIsUploadStatus(0);
        BraceCommDbInstance.getBraceCommDbInstance().saveDataSoreceData(braceCommB31Db);

    }

    /**
     * 保存心率详细数据
     * @param halfHourRateDatas 心率
     * @param mac 地址
     */
    private void saveHalfHeartData(List<HalfHourRateData> halfHourRateDatas, String mac) {
        if(halfHourRateDatas == null || halfHourRateDatas.isEmpty())
            return;
        List<BraceHalfHeartBean> heartList = new ArrayList<>();
        for(HalfHourRateData ht : halfHourRateDatas){
            CusVPTimeData cusVPTimeData = new CusVPTimeData(ht.getTime().getYear(),ht.getTime().getMonth(),ht.getTime().getDay(),
                    ht.getTime().getHour(),ht.getTime().getMinute(),ht.getTime().getSecond(),ht.getTime().getWeekDay());
            BraceHalfHeartBean braceHalfHeartBean = new BraceHalfHeartBean();
            braceHalfHeartBean.setDate(ht.getDate());
            braceHalfHeartBean.setRateValue(ht.getRateValue());
            braceHalfHeartBean.setTime(cusVPTimeData);
            braceHalfHeartBean.setEcgCount(ht.getEcgCount());
            braceHalfHeartBean.setPpgCount(ht.getPpgCount());
            heartList.add(braceHalfHeartBean);
        }

        BraceCommB31Db braceCommB31Db = new BraceCommB31Db();
        braceCommB31Db.setType(Constant.DB_TYPE_HEART);
        braceCommB31Db.setRtcStr(halfHourRateDatas.get(0).getDate());
        braceCommB31Db.setAddressStr(mac);
        braceCommB31Db.setIsUploadStatus(0);
        braceCommB31Db.setDataSourceStr(gson.toJson(heartList));
        BraceCommDbInstance.getBraceCommDbInstance().saveDataSoreceData(braceCommB31Db);


    }

    //保存血压数据
    private void saveHalfBloodData(List<HalfHourBpData> halfHourBps, String mac) {
        if(halfHourBps == null || halfHourBps.isEmpty())
            return;
        //Log.e(TAG,"-------半小时血压="+new Gson().toJson(halfHourBps));
        List<BraceHalfBpBean> bpList = new ArrayList<>();
        for(HalfHourBpData bp : halfHourBps){
            TimeData timeData = bp.getTime();
            CusVPTimeData cusVPTimeData = new CusVPTimeData(timeData.getYear(),timeData.getMonth(),timeData.getDay(),timeData
                    .getHour(),timeData.getMinute(),timeData.getSecond(),timeData.getWeekDay());
            BraceHalfBpBean braceHalfBpBean = new BraceHalfBpBean();
            braceHalfBpBean.setDate(bp.getDate());
            braceHalfBpBean.setHighValue(bp.getHighValue());
            braceHalfBpBean.setLowValue(bp.getLowValue());
            braceHalfBpBean.setTime(cusVPTimeData);
            bpList.add(braceHalfBpBean);
        }


        BraceCommB31Db braceCommB31Db = new BraceCommB31Db();
        braceCommB31Db.setRtcStr(halfHourBps.get(0).getDate());
        braceCommB31Db.setAddressStr(mac);
        braceCommB31Db.setDataSourceStr(gson.toJson(bpList));
        braceCommB31Db.setIsUploadStatus(0);
        braceCommB31Db.setType(Constant.DB_TYPE_BLOOD);
        BraceCommDbInstance.getBraceCommDbInstance().saveDataSoreceData(braceCommB31Db);

    }


    private IBleWriteResponse iBleWriteResponse = new IBleWriteResponse() {
        @Override
        public void onResponse(int i) {

        }
    };


    public void setConnBleOperListener(ConnBleOperListener connBleOperListener) {
        this.connBleOperListener = connBleOperListener;
    }

    public void setCurrentCountStepsListener(OnCurrentCountStepsListener currentCountStepsListener) {
        this.currentCountStepsListener = currentCountStepsListener;
    }

    public void setOnDataCompleteListener(OnDataCompleteListener onDataCompleteListener) {
        this.onDataCompleteListener = onDataCompleteListener;
    }



    //版本协议为3即新版本的数据读取
    class ReadAllDataAsync extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {

            if(!isSupportSpo2){
                Intent intent = new Intent();
                intent.setAction(Constant.B31_SPO2_COMPLETE);
                BaseApplication.getBaseApplication().sendBroadcast(intent);
                return null;
            }

            getVpManager().readOriginData(iBleWriteResponse, new IOriginData3Listener() {
                @Override
                public void onOriginFiveMinuteListDataChange(List<OriginData3> list) {

                }

                @Override
                public void onOriginHalfHourDataChange(OriginHalfHourData originHalfHourData) {
                    saveHalfData(originHalfHourData);
                }

                @Override
                public void onOriginHRVOriginListDataChange(List<HRVOriginData> list) {
                    if(b31sSaveHrvAsyncTask != null && b31sSaveHrvAsyncTask.getStatus() == Status.RUNNING){
                        b31sSaveHrvAsyncTask.cancel(true);
                        b31sSaveHrvAsyncTask = null;
                        b31sSaveHrvAsyncTask = new B31sSaveHrvAsyncTask();
                    }else{
                        b31sSaveHrvAsyncTask = new B31sSaveHrvAsyncTask();
                    }
                    b31sSaveHrvAsyncTask.execute(list);
                }

                @Override
                public void onOriginSpo2OriginListDataChange(List<Spo2hOriginData> list) {
                    if(b31SaveSpo2AsyncTask != null && b31SaveSpo2AsyncTask.getStatus() == Status.RUNNING){
                        b31SaveSpo2AsyncTask.cancel(true);
                        b31SaveSpo2AsyncTask = null;
                        b31SaveSpo2AsyncTask = new B31SaveSpo2AsyncTask();
                    }else{
                        b31SaveSpo2AsyncTask = new B31SaveSpo2AsyncTask();
                    }
                    b31SaveSpo2AsyncTask.execute(list);
                }

                @Override
                public void onReadOriginProgressDetail(int i, String s, int i1, int i2) {

                }

                @Override
                public void onReadOriginProgress(float v) {

                }

                @Override
                public void onReadOriginComplete() {
                    if(onDataCompleteListener != null)
                        onDataCompleteListener.dataReadComplete();
                }
            }, 1);
            return null;
        }
    }


    //发送广播
    private void sendAction(String action){
        Intent intent = new Intent();
        intent.setAction(action);
        BaseApplication.getBaseApplication().sendBroadcast(intent);
    }
}
