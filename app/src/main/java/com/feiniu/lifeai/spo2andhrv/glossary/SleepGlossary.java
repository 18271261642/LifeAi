package com.feiniu.lifeai.spo2andhrv.glossary;

import android.content.Context;

import com.feiniu.lifeai.R;


/**
 * Created by Administrator on 2017/9/19.
 */

public class SleepGlossary extends AGlossary {
    public SleepGlossary(Context context) {
        super(context);
    }

    @Override
    public void getGlossaryString() {
        head = getResoureStr(R.string.vpspo2h_sleep_sportvalue);
        groupString = getResoures(R.array.glossary_osahs);
        itemString = new String[][]{
                getResoures(R.array.glossary_sleep_item_1),
                getResoures(R.array.glossary_sleep_item_2),
                getResoures(R.array.glossary_sleep_item_3),
        };
    }

}
