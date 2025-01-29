package com.proxy.listener;

public class SetLogger {
	public static LogCallBack logCallBack;
	public interface LogCallBack{
		public void onLog(String str);
	}
	public static void setLogCallBack(LogCallBack logCallBack1){
		logCallBack=logCallBack1;
	}
	public static void log(String str){
		if (logCallBack!=null){
			logCallBack.onLog(str);
		}
	}
}