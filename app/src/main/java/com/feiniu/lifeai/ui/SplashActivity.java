package com.feiniu.lifeai.ui;

import com.feiniu.lifeai.R;
import com.feiniu.lifeai.comm.MyActivity;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import androidx.annotation.NonNull;

/**
 * 引导页
 * Created by Admin
 * Date 2020/12/16
 */
public class SplashActivity extends MyActivity {


    @Override
    protected int getLayoutId() {
        return R.layout.activity_splash_layout;
    }


    @Override
    protected void initView() {

    }

    @Override
    protected void initData() {
        startActivity(HomeActivity.class);
        finish();
    }

    @NonNull
    @Override
    protected ImmersionBar createStatusBarConfig() {
        return super.createStatusBarConfig().hideBar(BarHide.FLAG_HIDE_BAR);
    }
}
