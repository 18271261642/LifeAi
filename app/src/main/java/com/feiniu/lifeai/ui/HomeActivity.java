package com.feiniu.lifeai.ui;

import android.view.MenuItem;

import com.feiniu.lifeai.R;
import com.feiniu.lifeai.comm.MyActivity;
import com.feiniu.lifeai.comm.MyFragment;
import com.feiniu.lifeai.ui.fragment.DeviceManagerFragment;
import com.feiniu.lifeai.ui.fragment.FindFragment;
import com.feiniu.lifeai.ui.fragment.MineFragment;
import com.feiniu.lifeai.ui.fragment.RecordFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hjq.base.BaseFragmentAdapter;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

/**
 * Created by Admin
 * Date 2020/12/16
 */
public class HomeActivity extends MyActivity implements BottomNavigationView.OnNavigationItemSelectedListener{


    private ViewPager mViewPager;
    private BottomNavigationView mBottomNavigationView;

    private BaseFragmentAdapter<MyFragment> mPagerAdapter;



    @Override
    protected int getLayoutId() {
        return R.layout.activity_home_layout;
    }

    @Override
    protected void initView() {
        mViewPager = findViewById(R.id.vp_home_pager);
        mBottomNavigationView = findViewById(R.id.vp_home_navigation);
        // 不使用图标默认变色
        mBottomNavigationView.setItemIconTintList(null);
        mBottomNavigationView.setOnNavigationItemSelectedListener(this);
    }

    @Override
    protected void initData() {
        mPagerAdapter = new BaseFragmentAdapter<>(this);
        mPagerAdapter.addFragment(RecordFragment.getInstance());
        mPagerAdapter.addFragment(DeviceManagerFragment.getInstance());
        mPagerAdapter.addFragment(FindFragment.getInstance());
        mPagerAdapter.addFragment(MineFragment.getInstance());
        // 设置成懒加载模式
        mPagerAdapter.setLazyMode(true);
        mViewPager.setAdapter(mPagerAdapter);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_home:
                mViewPager.setCurrentItem(0);
                return true;
            case R.id.home_found:
                mViewPager.setCurrentItem(1);
                return true;
            case R.id.home_message:
                mViewPager.setCurrentItem(2);
                return true;
            case R.id.home_me:
                mViewPager.setCurrentItem(3);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        mViewPager.setAdapter(null);
        mBottomNavigationView.setOnNavigationItemSelectedListener(null);
        super.onDestroy();
    }
}
