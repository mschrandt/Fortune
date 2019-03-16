package com.android.mschrandt.fortune.model;

public enum TransactionCategory {

    INCOME {
        @Override
        public String toString() {
            return "Income";
        }
    },

    EXPENSE {
        @Override
        public String toString() {
            return "Expense";
        }
    },

    TRANSFER {
        @Override
        public String toString() {
            return "Transfer";
        }
    }
}
