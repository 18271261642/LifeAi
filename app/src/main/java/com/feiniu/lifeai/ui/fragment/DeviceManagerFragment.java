package com.feiniu.lifeai.ui.fragment;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.feiniu.lifeai.R;
import com.feiniu.lifeai.comm.MyFragment;
import com.feiniu.lifeai.ui.HomeActivity;


/**
 * 设备管理界面，设备功能展示
 * Created by Admin
 * Date 2020/12/28
 */
public class DeviceManagerFragment extends MyFragment<HomeActivity> {

//    private TextView titleTv;
//    private ImageView backImg;


    public static DeviceManagerFragment getInstance(){
        return new DeviceManagerFragment();
    }

    @Override
    protected int getLayoutId() {

        return R.layout.fragment_device_manager_layout;
    }

    @Override
    protected void initView() {
//        titleTv = findViewById(R.id.commentTitleTv);
//        backImg = findViewById(R.id.commentBackImg);
//        backImg.setVisibility(View.GONE);
//        titleTv.setText("设备");


    }

    @Override
    protected void initData() {

    }
}
