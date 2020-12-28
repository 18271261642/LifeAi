package com.feiniu.lifeai.ui.fragment;

import android.widget.TextView;

import com.feiniu.lifeai.R;
import com.feiniu.lifeai.comm.MyFragment;
import com.feiniu.lifeai.ui.HomeActivity;
import com.hjq.base.BaseFragment;

/**
 * 数据展示页面
 * Created by Admin
 * Date 2020/12/28
 */
public class RecordFragment extends MyFragment<HomeActivity> {


    private TextView titleTv;


    public static RecordFragment getInstance(){
        return new RecordFragment();
    }


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_record_layout;
    }

    @Override
    protected void initView() {
        titleTv = findViewById(R.id.commentTitleTv);
        titleTv.setText("记录");
    }

    @Override
    protected void initData() {

    }
}
