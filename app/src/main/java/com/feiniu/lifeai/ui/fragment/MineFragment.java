package com.feiniu.lifeai.ui.fragment;

import android.widget.TextView;

import com.feiniu.lifeai.R;
import com.feiniu.lifeai.comm.MyFragment;
import com.feiniu.lifeai.ui.HomeActivity;

/**
 * 我的界面
 * Created by Admin
 * Date 2020/12/28
 */
public class MineFragment extends MyFragment<HomeActivity> {


    private TextView titleTv;

    public static MineFragment getInstance(){
        return new MineFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_mine_layout;
    }

    @Override
    protected void initView() {
        titleTv = findViewById(R.id.commentTitleTv);

        titleTv.setText("我的");
    }

    @Override
    protected void initData() {

    }
}
