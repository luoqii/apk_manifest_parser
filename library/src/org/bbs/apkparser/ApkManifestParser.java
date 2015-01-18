package org.bbs.apkparser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.bbs.apkparser.ApkManifestParser.PackageInfoX.ActivityInfoX;
import org.bbs.apkparser.ApkManifestParser.PackageInfoX.ApplicationInfoX;
import org.bbs.apkparser.ApkManifestParser.PackageInfoX.IntentInfoX;
import org.bbs.apkparser.ApkManifestParser.PackageInfoX.UsesSdkX;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

@SuppressLint("NewApi")
public class ApkManifestParser {
	private static final String ATTR_BACKUP_AGENT = "backupAgent";
	private static final String ATTR_ALLOW_TASK_REPARENTING = "allowTaskReparenting";
	private static final String ATTR_DEBUGGABLE = "debuggable";
	private static final String ATTR_PROCESS = "process";
	private static final String TAG_META_DATA = "meta-data";
	private static final String ATTR_BANNER = "banner";
	private static final String ATTR_LOGO = "logo";
	private static final String TAG = ApkManifestParser.class.getSimpleName();
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
	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
	private static final String ATTR_VERSION_CODE = "versionCode";
	private static final String TAG_APPLICATION = "application";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_PACKAGE = "package";
	private static final String TAG_MANIFEST = "manifest";

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
			
			parser = assets.openXmlResourceParser(cookie, "AndroidManifest.xml");
			dumpParser(parser);
//			info.dump();

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
				ActivityInfoX aX = (ActivityInfoX) a;
				if (aX.theme == 0 && appInfo.theme > 0) {
					aX.theme = appInfo.theme;
				}

				aX.labelRes = aX.labelRes != 0 ? aX.labelRes : appInfo.labelRes;
				aX.nonLocalizedLabel = !TextUtils.isEmpty(aX.nonLocalizedLabel) ? aX.nonLocalizedLabel : appInfo.nonLocalizedLabel;
				aX.packageName = appInfo.packageName;
				aX.mPackageInfo = info;
				
				if (!TextUtils.isEmpty(aX.name) && 
						(aX.name.startsWith(".") || !aX.name.contains("."))) {
					String D = !aX.name.contains(".") ? "." : "";
					aX.name = appInfo.packageName + D + aX.name;
				}
			}
			
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
				Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
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
				parserApplication(parser, info);
			} else if (TAG_USES_SDK.equals(tagName)) {
				parserUsesSdk(parser, info);
			} else {
				Log.w(TAG, "un-handled tag: " + tagName);
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

	private static void parserUsesSdk(XmlResourceParser parser,
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
				Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
			}
		}
		
		info.mUsesSdk = sdk;
	}

	private static void parserApplication(XmlResourceParser parser,
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
				Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
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
				parserActivity(parser, info);
			} else if (TAG_META_DATA.equals(tagName)) {
				if (info.applicationInfo == null){
					info.applicationInfo = new ApplicationInfoX();
				}
				if (info.applicationInfo.metaData == null) {
					info.applicationInfo.metaData = new Bundle();
				}
				parserMetaData(parser, info.applicationInfo.metaData);
			} else {
				Log.w(TAG, "un-handled tag: " + tagName);
			}
		}

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
				Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
			} 
		}
	}

	private static void parserMetaData(XmlResourceParser parser, Bundle metaData) {
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
				Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
			}
		}
		
		metaData.putString(key, value);		
	}

	private static void parserActivity(XmlResourceParser parser,
			PackageInfoX info) throws XmlPullParserException, IOException {
		ActivityInfoX a = new ActivityInfoX();
		a.applicationInfo = info.applicationInfo;
		// parse attr
		final int attCount = parser.getAttributeCount();
		boolean hasLabel = false;
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_THEME.equals(attName)) {
				a.theme = Integer.parseInt(attValue.substring(1));
			} else {
				Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
			}
		}
		parsePackageItem(parser, a);		
		
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
				parserIntentFilter(parser, info, a);
			} else if (TAG_META_DATA.equals(tagName)) {
				if (a.metaData == null) {
					a.metaData = new Bundle();
				}
				parserMetaData(parser, a.metaData);
			} else {
				Log.w(TAG, "un-handled tag: " + tagName);
			}
			
		}

		if (info.activities == null) {
			info.activities = new ActivityInfoX[1];

			info.activities[0] = a;
		} else {
			int len = info.activities.length;
			ActivityInfoX[] as = new ActivityInfoX[len + 1];
			System.arraycopy(info.activities, 0, as, 0, len);
			as[len] = a;

			info.activities = as;
		}
	}

	private static void parserIntentFilter(XmlResourceParser parser,
			PackageInfoX info, ActivityInfoX a) throws XmlPullParserException,
			IOException {
		IntentInfoX i = new IntentInfoX();
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
				parserAction(parser, info, i);
			} else if (TAG_CATEGORY.equals(tagName)) {
				parseCategory(parser, info, i);
			} else {
				Log.w(TAG, "un-handled tag: " + tagName);
			}
		}

		if (a.mIntents == null) {
			a.mIntents = new IntentInfoX[1];

			a.mIntents[0] = i;
		} else {
			int len = a.mIntents.length;
			IntentInfoX[] as = new IntentInfoX[len + 1];
			System.arraycopy(a.mIntents, 0, as, 0, len);
			as[len] = i;

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
				Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
			}
		}
	}

	private static void parserAction(XmlResourceParser parser,
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
				Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
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
		public static final int DUMP_ACTIVITY = 1 << 1;
		public static final int DUMP_USES_SDK = 1 << 2;
		
		public static final int DUMP_ALL = 0xFFFF;
		
		public UsesSdkX mUsesSdk;
		
		// evaluate by application.
		public String mLibPath;

		public static void dump(int level, Bundle metaData) {
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

		public static class ApplicationInfoX extends ApplicationInfo {

			public boolean mDebuggable;
			public boolean mAllowTaskReparenting;

			public void dump(int level) {	
				Log.d(TAG, makePrefix(level) + "appliction: ");
				level++;
				Log.d(TAG, makePrefix(level) + "packageName: " + packageName);
				Log.d(TAG, makePrefix(level) + "theme      : " + theme);

				PackageInfoX.dump(level, metaData);
			}
		}

		public static class ActivityInfoX extends ActivityInfo implements
				Parcelable {
			public IntentInfoX[] mIntents;
			public PackageInfoX mPackageInfo;

			public int describeContents() {
				return 0;
			}

			public void dump(int level) {
				Log.d(TAG, makePrefix(level) + "activity:");
				Log.d(TAG, makePrefix(level + 1) + "name : " + name);
				Log.d(TAG, makePrefix(level + 1) + "icon : " + icon);
				Log.d(TAG, makePrefix(level + 1) + "theme: " + theme);
				
				PackageInfoX.dump(level + 1, metaData);
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

		public static class IntentInfoX extends IntentFilter {
			public String[] mActions;
		}
		
		public static class UsesSdkX {
			public int mMinSdkVersion;
			public int mTargetSdkVersion;
			public int mMaxSdkVersion;
			
			public void dump(int level) {		
				Log.d(TAG, makePrefix(level) + "mUseSdk: ");
				Log.d(TAG, makePrefix(level++) + "mMinSdkVersion: " + mMinSdkVersion);
				Log.d(TAG, makePrefix(level++) + "mTargetSdkVersion: " + mTargetSdkVersion);
				Log.d(TAG, makePrefix(level++) + "mMaxSdkVersion: " + mMaxSdkVersion);
			}
		}
		
		public void dump(){
			Log.d(TAG, "dump manefest info:");
			dump(0);
		}
		
		public void dump(int level) {
			if (mUsesSdk != null) {
				mUsesSdk.dump(level + 1);
			}
			
			if (applicationInfo != null) {
				((ApplicationInfoX)applicationInfo).dump(level + 1);
			}
			
			if (activities != null && activities.length > 0) {
				Log.d(TAG, makePrefix(level) + "activities: ");
				for (ActivityInfo a : activities) {
					((ActivityInfoX)a).dump(level + 1);
				}
			}
		}
	}
}
