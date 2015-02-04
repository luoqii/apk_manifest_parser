package org.bbs.apkparser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import org.bbs.apkparser.ApkManifestParser.PackageInfoX.ActivityInfoX;
import org.bbs.apkparser.ApkManifestParser.PackageInfoX.ApplicationInfoX;
import org.bbs.apkparser.ApkManifestParser.PackageInfoX.IntentInfoX;
import org.bbs.apkparser.ApkManifestParser.PackageInfoX.ServiceInfoX;
import org.bbs.apkparser.ApkManifestParser.PackageInfoX.UsesSdkX;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

@SuppressLint("NewApi")
public class ApkManifestParser {
	private static final String TAG = ApkManifestParser.class.getSimpleName();

	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
	private static final String TAG_SERVICE = "service";
	private static final String ATTR_BACKUP_AGENT = "backupAgent";
	private static final String ATTR_ALLOW_TASK_REPARENTING = "allowTaskReparenting";
	private static final String ATTR_DEBUGGABLE = "debuggable";
	private static final String ATTR_PROCESS = "process";
	private static final String TAG_META_DATA = "meta-data";
	private static final String ATTR_BANNER = "banner";
	private static final String ATTR_LOGO = "logo";
	private static final String TAG_USES_SDK = "uses-sdk";
	private static final String ATTR_RESOURCE = "resource";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_ICON = "icon";
	private static final String ATTR_THEME = "theme";
	private static final String ATTR_LABEL = "label";
	private static final String TAG_CATEGORY = "category";
	private static final String TAG_INTENT_FILTER = "intent-filter";
	private static final String TAG_ACTION = "action";
	private static final String TAG_ACTIVITY = "activity";
	private static final String ATTR_VERSION_NAME = "versionName";
	private static final String ATTR_VERSION_CODE = "versionCode";
	private static final String TAG_APPLICATION = "application";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_PACKAGE = "package";
	private static final String TAG_MANIFEST = "manifest";
	
	private static final boolean LOG_UN_HANDLED_ITEM = false;

	public static PackageInfoX parseAPk(Context context, String apkFile) {
		PackageInfoX info = new PackageInfoX();
		
		AssetManager assets;
		XmlResourceParser parser = null;
		try {
			assets = AssetManager.class.getConstructor(null).newInstance(null);
			Method method = assets.getClass().getMethod("addAssetPath",
					new Class[] { String.class });
			int cookie = (Integer) method.invoke(assets, apkFile);
			parser = assets
					.openXmlResourceParser(cookie, "AndroidManifest.xml");
			parseApk(parser, info);
			info.applicationInfo.publicSourceDir = apkFile;
			info.applicationInfo.sourceDir = apkFile;
			resolveParsedApk(info);
			
//			parser = assets.openXmlResourceParser(cookie, "AndroidManifest.xml");
//			dumpParser(parser);
			info.dump();

			return info;
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private static void resolveParsedApk(PackageInfoX info) {
		ApplicationInfo appInfo = info.applicationInfo;
		
		appInfo.packageName = info.packageName;
		if (!TextUtils.isEmpty(appInfo.name)) {
			if (!appInfo.name.contains(".")) {
				appInfo.name = appInfo.packageName + "." + appInfo.name;
			} else if (appInfo.name.startsWith(".")) {
				appInfo.name = appInfo.packageName + "" + appInfo.name;
			}
		}
		appInfo.className = appInfo.name;
		
		if (info.mUsesSdk != null) {
			UsesSdkX sdk = info.mUsesSdk;

			if (sdk.mMaxSdkVersion == 0) {
				sdk.mMaxSdkVersion = sdk.mTargetSdkVersion;
			}
			if (sdk.mTargetSdkVersion == 0) {
				sdk.mTargetSdkVersion = sdk.mMaxSdkVersion;
			}
			
			if (sdk.mMaxSdkVersion > 0) {
				appInfo.targetSdkVersion = sdk.mTargetSdkVersion;
			}
		}
		
		if (info.activities != null && info.activities.length > 0) {
			for (ActivityInfo a : info.activities) {
				ActivityInfoX comX = (ActivityInfoX) a;
				comX.mPackageInfo = info;
				if (comX.theme == 0 && appInfo.theme > 0) {
					comX.theme = appInfo.theme;
				}

				resolveComponentInfo((ApplicationInfoX) appInfo, comX);
			}
		}		
		
		if (info.services != null && info.services.length > 0) {
			for (ServiceInfo a : info.services) {
				ServiceInfoX comX = (ServiceInfoX) a;
				comX.mPackageInfo = info;

				resolveComponentInfo((ApplicationInfoX) appInfo, comX);
			}
		}
	}
	
	private static void resolveComponentInfo(ApplicationInfoX appInfo, ComponentInfo cInfo) {
		cInfo.labelRes = cInfo.labelRes != 0 ? cInfo.labelRes : appInfo.labelRes;
		cInfo.nonLocalizedLabel = !TextUtils.isEmpty(cInfo.nonLocalizedLabel) ? cInfo.nonLocalizedLabel : appInfo.nonLocalizedLabel;
		cInfo.packageName = appInfo.packageName;
		
		if (!TextUtils.isEmpty(cInfo.name) && 
				(cInfo.name.startsWith(".") || !cInfo.name.contains("."))) {
			String D = !cInfo.name.contains(".") ? "." : "";
			cInfo.name = appInfo.packageName + D + cInfo.name;
		}
	}

	private static void parseApk(XmlResourceParser parser, PackageInfoX info) {
		int eventType;
		try {
			eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String tag = parser.getName();
				if (eventType == XmlPullParser.START_DOCUMENT) {
				} else if (eventType == XmlPullParser.START_TAG) {
					if (TAG_MANIFEST.equals(tag)) {
						parserManifest(parser, info);
					} 
				} else if (eventType == XmlPullParser.END_TAG) {
				} else if (eventType == XmlPullParser.TEXT) {
				}

				final int attCount = parser.getAttributeCount();
				for (int i = 0; i < attCount; i++) {
					String attName = parser.getAttributeName(i);
					String attValue = parser.getAttributeValue(i);
				}

				eventType = parser.next();
			}
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void parserManifest(XmlResourceParser parser,
			PackageInfoX info) throws XmlPullParserException, IOException {
		// parse attr
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_PACKAGE.equals(attName)) {
				info.packageName = attValue;
			} else if (ATTR_VERSION_NAME.equals(attName)) {
				info.versionName = attName;
			} else if (ATTR_VERSION_CODE.equals(attName)) {
				info.versionCode = Integer.parseInt(attValue);
			} else if ("sharedUserId".equals(attName)) {
				info.sharedUserId = (attValue);
			} else if ("sharedUserLabel".equals(attName)) {
				info.sharedUserLabel = toResId(attValue);
//			} else if ("installLocation".equals(attName)) {
//				info.installLocation = toResId(attValue);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}
		
		// parse sub-element
		int type;
		int outerDepth = parser.getDepth();
		while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
				&& (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
			if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
				continue;
			}

			String tagName = parser.getName();
			if (TAG_APPLICATION.equals(tagName)) {
				parseApplication(parser, info);
			} else if (TAG_USES_SDK.equals(tagName)) {
				parseUsesSdk(parser, info);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled tag: " + tagName);
				}
			}
		}

	}
	
	private static int toResId(String attValue) {
		if (attValue.startsWith("")) {
			return Integer.parseInt(attValue.substring(1));
		}
		
		throw new RuntimeException("res attValue must start with @.");
//		return -1;
	}
	
	private static boolean toBoolean(String attValue) {
		if ("true".equals(attValue))	 {
			return true;
		}
		return false;
	}

	private static void parseUsesSdk(XmlResourceParser parser,
			PackageInfoX info) {// parse attr
		UsesSdkX sdk = new UsesSdkX();
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if ("minSdkVersion".equals(attName)) {
				sdk.mMinSdkVersion = Integer.parseInt(attValue);
			} else if ("maxSdkVersion".equals(attName)) {
				sdk.mMaxSdkVersion = Integer.parseInt(attValue);
			} else if ("targetSdkVersion".equals(attName)) {
				sdk.mTargetSdkVersion = Integer.parseInt(attValue);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}
		
		info.mUsesSdk = sdk;
	}

	private static void parseApplication(XmlResourceParser parser,
			PackageInfoX info) throws XmlPullParserException, IOException {
		info.applicationInfo = new ApplicationInfoX();
		ApplicationInfoX app = (ApplicationInfoX) info.applicationInfo;
		// parse attr
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_THEME.equals(attName)) {
				app.theme = Integer.parseInt(attValue.substring(1));
			} else if (ATTR_BACKUP_AGENT.equals(attName)) {
				app.backupAgentName = attValue;
			} else if (ATTR_ALLOW_TASK_REPARENTING.equals(attName)) {
				app.mAllowTaskReparenting = toBoolean(attValue);
			} else if (ATTR_DEBUGGABLE.equals(attName)) {
				app.mDebuggable = toBoolean(attValue);
			} else if (ATTR_PROCESS.equals(attName)) {
				app.processName = (attValue);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}
		parsePackageItem(parser, app);
		
		// parse sub-element
		int type;
		int outerDepth = parser.getDepth();
		while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
				&& (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
			if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
				continue;
			}

			String tagName = parser.getName();

			if (TAG_ACTIVITY.equals(tagName)) {
				parseActivity(parser, info);
			} else if (TAG_META_DATA.equals(tagName)) {
				if (info.applicationInfo == null){
					info.applicationInfo = new ApplicationInfoX();
				}
				if (info.applicationInfo.metaData == null) {
					info.applicationInfo.metaData = new Bundle();
				}
				parseMetaData(parser, info.applicationInfo.metaData);
			} if (TAG_SERVICE.equals(tagName)) {
				parseService(parser, info);
			}  else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled tag: " + tagName);
				}
			}
		}

	}
	
	/**
	 * <href a="http://developer.android.com/reference/android/content/pm/ComponentInfo.html">aaa</href>
	 */
	private static void parseComponentItem(XmlResourceParser parser, ComponentInfo info) {
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if ("process".equals(attName)) {
				String cName = attValue;
				info.processName = cName;
			} else if ("exported".equals(attName)) {
				info.exported = Boolean.parseBoolean(attName);
			} else if ("enabled".equals(attName)) {
				info.enabled = Boolean.parseBoolean(attName);
			} else if ("description".equals(attName)) {
				info.descriptionRes = toResId(attValue);
			}  
		}
		
		parsePackageItem(parser, info);
	}

	private static void parsePackageItem(XmlResourceParser parser, PackageItemInfo info) {
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_NAME.equals(attName)) {
				String cName = attValue;
				info.name = cName;
			} else if (ATTR_LABEL.equals(attName)) {
				if (attValue.startsWith("@")) {
					info.labelRes = Integer.parseInt(attValue.substring(1));
				} else {
					info.nonLocalizedLabel = attValue;
				}
			} else if (ATTR_ICON.equals(attName)) {
					info.icon = toResId(attValue);
			} else if (ATTR_LOGO.equals(attName)) {
					info.logo = toResId(attValue);
			} else if (ATTR_BANNER.equals(attName)) {
					info.logo = toResId(attValue);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			} 
		}
	}

	private static void parseMetaData(XmlResourceParser parser, Bundle metaData) {
		String key = null;
		String value = null;
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_NAME.equals(attName)) {
				key = attValue;
			} else if (ATTR_VALUE.equals(attName)) {
				value = attValue;
			} else if (ATTR_RESOURCE.equals(attName)) {
				if (attValue.startsWith("@")) {
					value = attValue;
				}
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}
		
		metaData.putString(key, value);		
	}

	private static void parseActivity(XmlResourceParser parser,
			PackageInfoX info) throws XmlPullParserException, IOException {
		ActivityInfoX component = new ActivityInfoX();
		component.applicationInfo = info.applicationInfo;
		// parse attr
		final int attCount = parser.getAttributeCount();
		boolean hasLabel = false;
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_THEME.equals(attName)) {
				component.theme = Integer.parseInt(attValue.substring(1));
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}
		parseComponentItem(parser, component);		
		
		// parse sub-element
		int type;
		int outerDepth = parser.getDepth();
		while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
				&& (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
			if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
				continue;
			}

			String tagName = parser.getName();

			if (TAG_INTENT_FILTER.equals(tagName)) {
				parseIntentFilter(parser, info, component);
			} else if (TAG_META_DATA.equals(tagName)) {
				if (component.metaData == null) {
					component.metaData = new Bundle();
				}
				parseMetaData(parser, component.metaData);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled tag: " + tagName);
				}
			}
			
		}

		if (info.activities == null) {
			info.activities = new ActivityInfoX[1];

			info.activities[0] = component;
		} else {
			int len = info.activities.length;
			ActivityInfoX[] components = new ActivityInfoX[len + 1];
			System.arraycopy(info.activities, 0, components, 0, len);
			components[len] = component;

			info.activities = components;
		}
	}	
	
	private static void parseService(XmlResourceParser parser,
			PackageInfoX info) throws XmlPullParserException, IOException {
		ServiceInfoX component = new ServiceInfoX();
		component.applicationInfo = info.applicationInfo;
		// parse attr
		final int attCount = parser.getAttributeCount();
		boolean hasLabel = false;
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_THEME.equals(attName)) {
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}
		parseComponentItem(parser, component);		
		
		// parse sub-element
		int type;
		int outerDepth = parser.getDepth();
		while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
				&& (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
			if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
				continue;
			}

			String tagName = parser.getName();

//			if (TAG_INTENT_FILTER.equals(tagName)) {
//				parserIntentFilter(parser, info, component);
//			} else 
				if (TAG_META_DATA.equals(tagName)) {
				if (component.metaData == null) {
					component.metaData = new Bundle();
				}
				parseMetaData(parser, component.metaData);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled tag: " + tagName);
				}
			}
			
		}

		if (info.services == null) {
			info.services = new ServiceInfo[1];

			info.services[0] = component;
		} else {
			int len = info.services.length;
			ServiceInfo[] components = new ServiceInfo[len + 1];
			System.arraycopy(info.services, 0, components, 0, len);
			components[len] = component;

			info.services = components;
		}
	}

	private static void parseIntentFilter(XmlResourceParser parser,
			PackageInfoX info, ActivityInfoX a) throws XmlPullParserException,
			IOException {
		IntentInfoX intentInfo = new IntentInfoX();
		// parse attr

		// parse sub-element
		int type;
		int outerDepth = parser.getDepth();
		while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
				&& (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
			if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
				continue;
			}

			String tagName = parser.getName();

			if (TAG_ACTION.equals(tagName)) {
				parseAction(parser, info, intentInfo);
			} else if (TAG_CATEGORY.equals(tagName)) {
				parseCategory(parser, info, intentInfo);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled tag: " + tagName);
				}
			}
		}

		if (a.mIntents == null) {
			a.mIntents = new IntentInfoX[1];

			a.mIntents[0] = intentInfo;
		} else {
			int len = a.mIntents.length;
			IntentInfoX[] as = new IntentInfoX[len + 1];
			System.arraycopy(a.mIntents, 0, as, 0, len);
			as[len] = intentInfo;

			a.mIntents = as;
		}

	}

	private static void parseCategory(XmlResourceParser parser,
			PackageInfoX info, IntentInfoX intentInfo) {
		// parse attr
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_NAME.equals(attName)) {
				String category = attValue;
				intentInfo.addCategory(category);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}
	}

	private static void parseAction(XmlResourceParser parser,
			PackageInfoX info, IntentInfoX intentInfo) {
		// parse attr
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_NAME.equals(attName)) {
				String action = attValue;
				intentInfo.addAction(action);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}
	}

	private static void dumpParser(XmlResourceParser parser) {
		int depth = 0;
		int eventType;
		try {
			eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_DOCUMENT) {
					Log.d(TAG, makePrefix(depth) + "");
				} else if (eventType == XmlPullParser.START_TAG) {
					depth++;
					Log.d(TAG,
							makePrefix(depth) + "" + parser.getName());
				} else if (eventType == XmlPullParser.END_TAG) {
					Log.d(TAG,
							makePrefix(depth) + "" + parser.getName());
					depth--;
				} else if (eventType == XmlPullParser.TEXT) {
					Log.d(TAG, makePrefix(depth) + "" + parser.getText());
				}
				final int attCount = parser.getAttributeCount();
				for (int i = 0; i < attCount; i++) {
					String attName = parser.getAttributeName(i);
					String attValue = parser.getAttributeValue(i);
					Log.d(TAG, makePrefix(depth + 1) + "" + i + " " + attName
							+ " : " + attValue);
				}
				eventType = parser.next();
			}
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String makePrefix(int depth) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < depth; i++) {
			b.append("  ");
		}
		return b.toString();
	}

	/**
	 * all new-added member MUST has a 'm" prefix.
	 * 
	 * @author bysong
	 *
	 */
	public static class PackageInfoX extends PackageInfo {
		public static final int DUMP_APPLICATION = 1 << 0;
		public static final int DUMP_ACTIVITY = 1 << 1 | DUMP_APPLICATION;
		public static final int DUMP_USES_SDK = 1 << 2;
		public static final int DUMP_META_DATA = 1 << 3 | DUMP_APPLICATION;
		public static final int DUMP_SERVICE = 1 << 4 | DUMP_APPLICATION;
		
		public static final int DUMP_ALL = 0xFFFF;
		public static final int FLAG_DUMP = DUMP_SERVICE;
		
		public UsesSdkX mUsesSdk;
		
		// evaluate by application.
		public String mLibPath;
		
		public static boolean hasFlag(int flag, int mask) {
			return (flag & mask) == mask;
		}

		public static class ApplicationInfoX extends ApplicationInfo {

			public boolean mDebuggable;
			public boolean mAllowTaskReparenting;

			public void dump(int level, int flag) {	
				Log.d(TAG, makePrefix(level) + "appliction: ");
				level++;
				if (shouldLog(flag)) Log.d(TAG, makePrefix(level) + "packageName: " + packageName);
				if (shouldLog(flag)) Log.d(TAG, makePrefix(level) + "theme      : " + theme);

				PackageInfoX.dumpMetaData(level, metaData, flag);
			}

			public static boolean shouldLog(int flag)	 {
				return hasFlag(flag, DUMP_APPLICATION);
			}
		}

		public static class ActivityInfoX extends ActivityInfo 
		implements
				Parcelable {
			public IntentInfoX[] mIntents;
			public PackageInfoX mPackageInfo;

			public int describeContents() {
				return 0;
			}

			public void dump(int level, int flag) {
				Log.d(TAG, makePrefix(level) + "activity:");
				if (shouldLog(flag)) Log.d(TAG, makePrefix(level + 1) + "name : " + name);
				if (shouldLog(flag)) Log.d(TAG, makePrefix(level + 1) + "icon : " + icon);
				if (shouldLog(flag)) Log.d(TAG, makePrefix(level + 1) + "theme: " + theme);
				
				PackageInfoX.dumpMetaData(level + 1, metaData, flag);
			}

			public static boolean shouldLog(int flag)	 {
				return hasFlag(flag, DUMP_ACTIVITY);
			}
			
			public void writeToParcel(Parcel out, int flags) {
				super.writeToParcel(out, flags);
				out.writeParcelableArray(mIntents, flags);
			}

			public static final Parcelable.Creator<ActivityInfoX> CREATOR = new Parcelable.Creator<ActivityInfoX>() {
				public ActivityInfoX createFromParcel(Parcel in) {
					return new ActivityInfoX(in);
				}

				public ActivityInfoX[] newArray(int size) {
					return new ActivityInfoX[size];
				}
			};
			
			public ActivityInfoX(){
				
			}

			private ActivityInfoX(Parcel in) {
//				super(in);
//				mData = in.readInt();
			}

		}
		
		public static class ServiceInfoX extends ServiceInfo {
			public PackageInfoX mPackageInfo;

			public void dump(int level, int flag) {
				if ((shouldLog(flag))) Log.d(TAG, " service: ");
				if (shouldLog(flag)) Log.d(TAG, makePrefix(level + 1) + "name : " + name);
			}

			public static boolean shouldLog(int flag)	 {
				return hasFlag(flag, DUMP_SERVICE);
			}
			
		}

		public static class IntentInfoX extends IntentFilter {
			public String[] mActions;
		}
		
		public static class UsesSdkX {
			public int mMinSdkVersion;
			public int mTargetSdkVersion;
			public int mMaxSdkVersion;
			
			public void dump(int level, int flag) {		
				if (shouldLog(flag)) Log.d(TAG, makePrefix(level) + "mUseSdk: ");
				if (shouldLog(flag)) Log.d(TAG, makePrefix(level++) + "mMinSdkVersion: " + mMinSdkVersion);
				if (shouldLog(flag)) Log.d(TAG, makePrefix(level++) + "mTargetSdkVersion: " + mTargetSdkVersion);
				if (shouldLog(flag)) Log.d(TAG, makePrefix(level++) + "mMaxSdkVersion: " + mMaxSdkVersion);
			}
			
			public static boolean shouldLog(int flag)	 {
				return hasFlag(flag, DUMP_USES_SDK);
			}
		}
		
		static void dumpMetaData(int level, Bundle metaData, int flag){
			if (hasFlag(flag, DUMP_META_DATA)) {

				if (metaData != null) {
					Log.d(TAG, makePrefix(level) + "metaData: ");
					level++;
					Iterator<String> it = metaData.keySet().iterator();
					while (it.hasNext()) {
						String key = it.next();
						Log.d(TAG, makePrefix(level) + key + ": " + metaData.getString(key));
					}
				}
			}
		}
		
		public void dump() {
			dump(0, FLAG_DUMP);
		}
		
		void dump(int level, int flag) {
			if (mUsesSdk != null) {
				mUsesSdk.dump(level + 1, flag);
			}
			
			if (applicationInfo != null) {
				((ApplicationInfoX)applicationInfo).dump(level + 1, flag);
			}
			
			if (activities != null && activities.length > 0 && ActivityInfoX.shouldLog(flag)) {
				Log.d(TAG, makePrefix(level) + "activities: ");
				for (ActivityInfo a : activities) {
					((ActivityInfoX)a).dump(level + 1, flag);
				}
			}
			
			if (services != null && services.length > 0 && ServiceInfoX.shouldLog(flag)) {
				Log.d(TAG, makePrefix(level) + "services: ");
				for (ServiceInfo a : services) {
					((ServiceInfoX)a).dump(level + 1, flag);
				}
			}
		}
	}
}
