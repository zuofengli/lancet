package edu.uwm.jiaoduan;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAMERule = "edu.uwm.jiaoduan.messagesRule"; //$NON-NLS-1$
	private static final String BUNDLE_NAME = "edu.uwm.jiaoduan.messages"; //$NON-NLS-1$

	private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private Messages() {
	}

	public static void setUseRuleBundle(boolean use) {
		if (use)
			RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAMERule);
		else
			RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			System.out.println('!' + key + '!');
			return null;
		}
	}
}
