package com.feiniu.lifeai.spo2andhrv.glossary;

import android.content.Context;

import com.feiniu.lifeai.R;


/**
 * Created by Administrator on 2017/9/19.
 */

public class RateVariGlossary extends AGlossary {
    public RateVariGlossary(Context context) {
        super(context);
    }

    @Override
    public void getGlossaryString() {
        head = getResoureStr(R.string.vpspo2h_ratevaraive);
        groupString = getResoures(R.array.glossary_osahs);
        itemString = new String[][]{
                getResoures(R.array.glossary_ratevarable_item_1),
                getResoures(R.array.glossary_ratevarable_item_2),
                getResoures(R.array.glossary_ratevarable_item_3),
        };
    }

}
