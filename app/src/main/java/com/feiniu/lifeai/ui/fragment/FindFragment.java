package com.feiniu.lifeai.ui.fragment;

import android.widget.TextView;

import com.feiniu.lifeai.R;
import com.feiniu.lifeai.comm.MyFragment;
import com.feiniu.lifeai.ui.HomeActivity;
import com.hjq.base.BaseFragment;

/**
 * 发现模块
 * Created by Admin
 * Date 2020/12/28
 */
public class FindFragment extends MyFragment<HomeActivity> {


    private TextView titleTv;

    public static FindFragment getInstance(){
        return new FindFragment();
    }


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_find_layout;
    }

    @Override
    protected void initView() {
        titleTv = findViewById(R.id.commentTitleTv);

        titleTv.setText("发现");
    }

    @Override
    protected void initData() {

    }
}
