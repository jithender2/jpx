package com.proxy.listener;

import com.proxy.data.Message;

public class RepeaterListener {

	public static LogCallBack logCallBack;

	public interface LogCallBack {
		public void onLog(Message httpMessage);
	}

	public static void setLogCallBack(LogCallBack logCallBack1) {
		logCallBack = logCallBack1;
	}
	/*
		public static void log(Object str, int streamId) {
			if (logCallBack != null) {
	
				logCallBack.onLog(str, streamId);
			}
		}*/
	public static void log(Message str) {
		if (logCallBack != null) {
			new Thread(() -> {
				try {
					logCallBack.onLog(str);
				} catch (Exception e) {
				//	SetLogger.log("Exception in logCallBack: " + e.getMessage());
					e.printStackTrace();
				}
			}).start();
		}
	}
}