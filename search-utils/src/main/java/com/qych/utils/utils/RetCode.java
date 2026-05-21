package com.qych.utils.utils;

public interface RetCode {

    public static final int RET_OK=0;
    public static final int RET_EXCEPTION=1;
    public static final int RET_NOT_LOGIN=2;
    public static final int RET_ERROR_LOGIN=3;
    public static final int RET_EXCEL_ERROR=4;
    public static final int RET_THIRD_NOT_LOGIN=5;
    public static final int RET_PARAM_ERROR = 6;
    public static final int RET_NORM_VALUE_COVER=10001;

    public static  final int TO_LOGIN=401;
    public static final int NO_AUTH=403;
}
