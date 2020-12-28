package com.feiniu.lifeai.ui.activity;

import com.feiniu.lifeai.R;
import com.feiniu.lifeai.comm.MyActivity;
import com.feiniu.lifeai.ui.fragment.LoginFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * 登录页面
 * Created by Admin
 * Date 2020/12/19
 */
public class LoginActivity extends MyActivity {

    private FragmentManager fragmentManager;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_login_layout;
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initData() {
        fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.loginFly, LoginFragment.getInstance());
        fragmentTransaction.commit();

    }
}
