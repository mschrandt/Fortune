package com.android.mschrandt.fortune.model;

public enum InterestRateType {
    APR {
        @Override
        public String toString() {
            return "APR";
        }
    },

    APY {
        @Override
        public String toString() {
            return "APY";
        }
    }
}
