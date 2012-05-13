package de.toth.executer;

import android.app.Application;
import android.content.Context;

public class ApplicationContext extends Application {

	private static ApplicationContext instance = null;

	public ApplicationContext() {
		super();
		ApplicationContext.instance = this;
	}

	public static Context getInstance() {
		if (ApplicationContext.instance == null) {
			ApplicationContext.instance = new ApplicationContext();
		}

		return ApplicationContext.instance;
	}
}