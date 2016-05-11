package com.zsm.whiteboard.app;

import com.zsm.driver.android.log.LogInstaller;
import com.zsm.driver.android.log.LogPreferences;
import com.zsm.log.Log;

import android.app.Application;

public class WhiteBoardApp extends Application {

	public WhiteBoardApp() {
		super();
		LogInstaller.installAndroidLog( "WhiteBoard" );
	}

	@Override
	public void onCreate() {
		super.onCreate();
		LogPreferences.init( this );
		LogInstaller.installFileLog( this );
		Log.setGlobalLevel( Log.LEVEL.DEBUG );
	}

}
