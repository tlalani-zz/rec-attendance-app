package com.tanwir.qrcodescanner;

import android.content.Intent;

public class ReConfigs {

     static class ReCurrentConfig {
        public String re_center;
        public String re_class;
        public String re_shift;

        ReCurrentConfig(Intent i) {
            this.re_center = i.getStringExtra("center");
            this.re_class = i.getStringExtra("class");
            this.re_shift = i.getStringExtra("shift").replace(", ", "/");
        }

    }
}
