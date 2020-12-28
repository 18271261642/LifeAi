package com.feiniu.lifeai.spo2andhrv.glossary;

import android.content.Context;

import com.feiniu.lifeai.R;


/**
 * Created by Administrator on 2017/9/19.
 */

public class LowRemainGlossary extends AGlossary {
    public LowRemainGlossary(Context context) {
        super(context);
    }

    @Override
    public void getGlossaryString() {
        head = context.getString(R.string.vpspo2h_low_remain);
        groupString = getResoures(R.array.glossary_lowremain);
        itemString = new String[][]{
                getResoures(R.array.glossary_lowremain_item_1),
        };
    }

}