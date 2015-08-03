package com.tokbox.android;

/**
 * Created by madhav on 7/21/15.
 */
public interface MemStatListener {
    public void onMemoryStat(double available_mem, double total_mem, double used_mem, double used_per);
}
