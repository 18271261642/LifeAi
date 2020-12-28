package com.feiniu.lifeai.adapter;

import android.content.Context;
import com.feiniu.lifeai.R;
import com.feiniu.lifeai.bean.BraceHalfHeartBean;
import java.util.List;

/**
 * 心率详情adapter
 * Created by Admin
 * Date 2019/11/28
 */
public class HeartDetailAdapter extends CommonRecyclerAdapter<BraceHalfHeartBean> {


    public HeartDetailAdapter(Context context, List<BraceHalfHeartBean> data, int layoutId) {
        super(context, data, layoutId);
    }

    @Override
    public void convert(MyViewHolder holder, BraceHalfHeartBean item) {
        holder.setText(R.id.itemSportHealthyTimeTv,item.getTime().getColck());
        holder.setText(R.id.itemSportHealthyValueTv,item.getRateValue()+" bpm");
        holder.setImageResource(R.id.itemSportHealthyTypeImg, R.mipmap.history_heart);
    }
}
