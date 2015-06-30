package org.bbs.apkparser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bbs.apkparser.PackageInfoX.ActivityInfoX;
import org.bbs.apkparser.PackageInfoX.ApplicationInfoX;
import org.bbs.apkparser.PackageInfoX.IntentFilterX;
import org.bbs.apkparser.PackageInfoX.ServiceInfoX;
import org.bbs.apkparser.PackageInfoX.UsesSdkX;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.text.TextUtils;
import android.util.Log;

@SuppressLint("NewApi")
public class ApkManifestParser {
	static final String TAG = ApkManifestParser.class.getSimpleName();
	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

	private static final String TAG_CATEGORY         = "category";
	private static final String TAG_INTENT_FILTER    = "intent-filter";
	private static final String TAG_ACTION           = "action";
	private static final String TAG_ACTIVITY         = "activity";
	private static final String TAG_APPLICATION      = "application";
	private static final String TAG_MANIFEST         = "manifest";
	private static final String TAG_USES_SDK         = "uses-sdk";
	private static final String TAG_META_DATA        = "meta-data";
	private static final String TAG_SERVICE          = "service";
	private static final String TAG_USES_PERMISSION  = "uses-permission";
	private static final String TAG_PERMISSION       = "permission";
	private static final String TAG_PERMISSION_TREE  = "permission-tree";
	private static final String TAG_PERMISSION_GROUP = "permission-group";

	private static final String ATTR_SHARED_USER_LABEL = "sharedUserLabel";
	private static final String ATTR_SHARED_USER_ID = "sharedUserId";
	private static final String ATTR_BACKUP_AGENT = "backupAgent";
	private static final String ATTR_ALLOW_TASK_REPARENTING = "allowTaskReparenting";
	private static final String ATTR_DEBUGGABLE = "debuggable";
	private static final String ATTR_PROCESS = "process";
	private static final String ATTR_BANNER = "banner";
	private static final String ATTR_LOGO = "logo";
	private static final String ATTR_RESOURCE = "resource";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_ICON = "icon";
	private static final String ATTR_THEME = "theme";
	private static final String ATTR_LABEL = "label";
	private static final String ATTR_VERSION_NAME = "versionName";
	private static final String ATTR_VERSION_CODE = "versionCode";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_PACKAGE = "package";
	private static final String ATTR_PROTECTION_LEVEL = "protectionLevel";
	private static final String ATTR_PERMISSION_GROUP = "permissionGroup";
	private static final String ATTR_DESCRIPTION = "description";
	private static final String ATTR_TARGET_SDK_VERSION = "targetSdkVersion";
	private static final String ATTR_MAX_SDK_VERSION = "maxSdkVersion";
	private static final String ATTR_MIN_SDK_VERSION = "minSdkVersion";
	
	// copy from InentFilter
    private static final String SGLOB_STR = "sglob";
    private static final String PREFIX_STR = "prefix";
    private static final String LITERAL_STR = "literal";
    private static final String PATH_STR = "path";
    private static final String PORT_STR = "port";
    private static final String HOST_STR = "host";
    private static final String AUTH_STR = "auth";
    private static final String SSP_STR = "ssp";
    private static final String SCHEME_STR = "scheme";
    private static final String TYPE_STR = "type";
    private static final String CAT_STR = "cat";
    private static final String NAME_STR = "name";
    private static final String ACTION_STR = "action";

	private static final boolean LOG_UN_HANDLED_ITEM = false;

	public static PackageInfoX parseAPk(Context context, String apkFile) {
		return parseAPk(context, apkFile, true);
	}

	public static PackageInfoX parseAPk(Context context, String apkFile,
			boolean resolve) {
		return parseAPk(context, apkFile, resolve, false);
	}

	public static PackageInfoX parseAPk(Context context, String apkFile,
			boolean resolve, boolean debug) {
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
			if (resolve) {
				resolveParsedApk(info, apkFile);
			}

			if (debug) {
				parser = assets.openXmlResourceParser(cookie,
						"AndroidManifest.xml");
				dumpParser(parser);
				info.dump(PackageInfoX.DUMP_ALL);
			}

			return info;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static void resolveParsedApk(PackageInfoX info, String apkFile) {
		info.applicationInfo.publicSourceDir = apkFile;
		info.applicationInfo.sourceDir = apkFile;

		ApplicationInfo appInfo = info.applicationInfo;

		appInfo.packageName = info.packageName;
		if (!TextUtils.isEmpty(appInfo.name)) {
			if (!appInfo.name.contains(".")) {
				appInfo.name = appInfo.packageName + "." + appInfo.name;
			} else if (appInfo.name.startsWith(".")) {
				appInfo.name = appInfo.packageName + "" + appInfo.name;
			}
			appInfo.className = appInfo.name;
		} else {
			appInfo.className = Application.class.getName();
			appInfo.name = appInfo.className;
		}

		if (info.mUsesSdk != null) {
			UsesSdkX sdk = info.mUsesSdk;

			if (sdk.mMaxSdkVersion == 0) {
				sdk.mMaxSdkVersion = sdk.mTargetSdkVersion;
			}
			if (sdk.mTargetSdkVersion == 0) {
				sdk.mTargetSdkVersion = sdk.mMaxSdkVersion;
			}

			if (sdk.mTargetSdkVersion > 0) {
				appInfo.targetSdkVersion = sdk.mTargetSdkVersion;
			}
			if (sdk.mMinSdkVersion == 0) {
				// http://developer.android.com/guide/topics/manifest/uses-sdk-element.html
				sdk.mMinSdkVersion = 1;
			}
		} else {
			Log.w(TAG, "no <uses-sdk> in AndroidManifest.xml!");
		}

		if (info.activities != null && info.activities.length > 0) {
			for (ActivityInfo a : info.activities) {
				ActivityInfoX comX = (ActivityInfoX) a;
				comX.mPackageInfo = info;
				if (comX.theme == 0 && appInfo.theme > 0) {
					Log.i(TAG, "use app's theme : " + appInfo.theme);
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

	private static void resolveComponentInfo(ApplicationInfoX appInfo,
			ComponentInfo cInfo) {
		cInfo.labelRes = cInfo.labelRes != 0 ? cInfo.labelRes
				: appInfo.labelRes;
		cInfo.nonLocalizedLabel = !TextUtils.isEmpty(cInfo.nonLocalizedLabel) ? cInfo.nonLocalizedLabel
				: appInfo.nonLocalizedLabel;
		cInfo.packageName = appInfo.packageName;

		if (!TextUtils.isEmpty(cInfo.name)
				&& (cInfo.name.startsWith(".") || !cInfo.name.contains("."))) {
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
				info.versionName = attValue;
			} else if (ATTR_VERSION_CODE.equals(attName)) {
				info.versionCode = Integer.parseInt(attValue);
			} else if (ATTR_SHARED_USER_ID.equals(attName)) {
				info.sharedUserId = (attValue);
			} else if (ATTR_SHARED_USER_LABEL.equals(attName)) {
				info.sharedUserLabel = toResId(attValue);
				// } else if ("installLocation".equals(attName)) {
				// info.installLocation = toResId(attValue);
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
			} else if (TAG_USES_PERMISSION.equals(tagName)) {
				parseUsesPermission(parser, info);
			} else if (TAG_PERMISSION.equals(tagName)) {
				parsePermission(parser, info);
			} else if (TAG_PERMISSION_GROUP.equals(tagName)) {
				parsePermissionGroup(parser, info);
			} else if (TAG_PERMISSION_TREE.equals(tagName)) {
				parsePermissionTree(parser, info);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled tag: " + tagName);
				}
			}
		}

	}

	private static int toResId(String attValue) {
		if (attValue.startsWith("@")) {
			return Integer.parseInt(attValue.substring(1));
		}
		if (attValue.startsWith("0X") || attValue.startsWith("0x")) {
			return Integer.parseInt(attValue.substring(2), 16);
		}

		try {
			return Integer.parseInt(attValue);
		} catch (Exception e) {
			throw new RuntimeException("invalid int string: " + attValue);
		}

	}

	private static boolean toBoolean(String attValue) {
		if ("true".equals(attValue)) {
			return true;
		}
		return false;
	}

	private static void parseUsesSdk(XmlResourceParser parser, PackageInfoX info) {
		// parse attr
		UsesSdkX sdk = new UsesSdkX();
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_MIN_SDK_VERSION.equals(attName)) {
				sdk.mMinSdkVersion = Integer.parseInt(attValue);
			} else if (ATTR_MAX_SDK_VERSION.equals(attName)) {
				sdk.mMaxSdkVersion = Integer.parseInt(attValue);
			} else if (ATTR_TARGET_SDK_VERSION.equals(attName)) {
				sdk.mTargetSdkVersion = Integer.parseInt(attValue);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}

		info.mUsesSdk = sdk;
	}

	private static void parseUsesPermission(XmlResourceParser parser,
			PackageInfoX info) {
		// parse attr
		PackageInfoX.UsesPermissionX perm = new PackageInfoX.UsesPermissionX();
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_MAX_SDK_VERSION.equals(attName)) {
				perm.mMaxSdkVersion = Integer.parseInt(attValue);
			} else if (ATTR_NAME.equals(attName)) {
				perm.name = attValue;
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}

		if (info.mUsedPermissions == null) {
			info.mUsedPermissions = new PackageInfoX.UsesPermissionX[1];

			info.mUsedPermissions[0] = perm;
		} else {
			int len = info.mUsedPermissions.length;
			PackageInfoX.UsesPermissionX[] permissions = new PackageInfoX.UsesPermissionX[len + 1];
			System.arraycopy(info.mUsedPermissions, 0, permissions, 0, len);
			permissions[len] = perm;

			info.mUsedPermissions = permissions;
		}
	}

	private static void parsePermission(XmlResourceParser parser,
			PackageInfoX info) {
		// parse attr
		PermissionInfo perm = new PermissionInfo();
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_NAME.equals(attName)) {
				perm.name = attValue;
			} else if (ATTR_LABEL.equals(attName)) {
				perm.labelRes = toResId(attValue);
			} else if (ATTR_DESCRIPTION.equals(attName)) {
				perm.descriptionRes = toResId(attValue);
			} else if (ATTR_ICON.equals(attName)) {
				// perm.ic = toResId(attValue);
			} else if (ATTR_PERMISSION_GROUP.equals(attName)) {
				perm.group = (attValue);
			} else if (ATTR_PROTECTION_LEVEL.equals(attName)) {
				perm.protectionLevel = toResId(attValue);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}

		if (info.permissions == null) {
			info.permissions = new PermissionInfo[1];

			info.permissions[0] = perm;
		} else {
			int len = info.permissions.length;
			PermissionInfo[] permissions = new PermissionInfo[len + 1];
			System.arraycopy(info.permissions, 0, permissions, 0, len);
			permissions[len] = perm;

			info.permissions = permissions;
		}
	}

	private static void parsePermissionGroup(XmlResourceParser parser,
			PackageInfoX info) {
		// parse attr
		PermissionGroupInfo perm = new PermissionGroupInfo();
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_NAME.equals(attName)) {
				perm.name = attValue;
			} else if (ATTR_LABEL.equals(attName)) {
				perm.labelRes = toResId(attValue);
			} else if (ATTR_DESCRIPTION.equals(attName)) {
				perm.descriptionRes = toResId(attValue);
			} else if (ATTR_ICON.equals(attName)) {
				// perm.ic = toResId(attValue);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}

		if (info.mPermissionGroups == null) {
			info.mPermissionGroups = new PermissionGroupInfo[1];

			info.mPermissionGroups[0] = perm;
		} else {
			int len = info.permissions.length;
			PermissionGroupInfo[] permissions = new PermissionGroupInfo[len + 1];
			System.arraycopy(info.permissions, 0, permissions, 0, len);
			permissions[len] = perm;

			info.mPermissionGroups = permissions;
		}
	}

	private static void parsePermissionTree(XmlResourceParser parser,
			PackageInfoX info) {
		// parse attr
		PackageInfoX.PermissionTreeX perm = new PackageInfoX.PermissionTreeX();
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_NAME.equals(attName)) {
				perm.mName = attValue;
			} else if (ATTR_LABEL.equals(attName)) {
				perm.mLabelRes = toResId(attValue);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}

		if (info.mPermissionTrees == null) {
			info.mPermissionTrees = new PackageInfoX.PermissionTreeX[1];

			info.mPermissionTrees[0] = perm;
		} else {
			int len = info.mPermissionTrees.length;
			PackageInfoX.PermissionTreeX[] permissions = new PackageInfoX.PermissionTreeX[len + 1];
			System.arraycopy(info.permissions, 0, permissions, 0, len);
			permissions[len] = perm;

			info.mPermissionTrees = permissions;
		}
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
				if (info.applicationInfo == null) {
					info.applicationInfo = new ApplicationInfoX();
				}
				if (info.applicationInfo.metaData == null) {
					info.applicationInfo.metaData = new Bundle();
				}
				parseMetaData(parser, info.applicationInfo.metaData);
			}
			if (TAG_SERVICE.equals(tagName)) {
				parseService(parser, info);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled tag: " + tagName);
				}
			}
		}

	}

	/**
	 * <href a="http://developer.android.com/reference/android/content/pm/ComponentInfo.html">aaa</href>
	 */
	private static void parseComponentItem(XmlResourceParser parser,
			ComponentInfo info) {
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
			} else if (ATTR_DESCRIPTION.equals(attName)) {
				info.descriptionRes = toResId(attValue);
			}
		}

		parsePackageItem(parser, info);
	}

	private static void parsePackageItem(XmlResourceParser parser,
			PackageItemInfo info) {
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
		int intValue = -1;
		final int attCount = parser.getAttributeCount();
		for (int i = 0; i < attCount; i++) {
			String attName = parser.getAttributeName(i);
			String attValue = parser.getAttributeValue(i);
			if (ATTR_NAME.equals(attName)) {
				key = attValue;
			} else if (ATTR_VALUE.equals(attName)) {
				value = attValue;
			} else if (ATTR_RESOURCE.equals(attName)) {
				intValue = toResId(attValue);
			} else {
				if (LOG_UN_HANDLED_ITEM) {
					Log.w(TAG, "un-handled att: " + attName + "=" + attValue);
				}
			}
		}

		if (intValue != -1) {
			metaData.putInt(key, intValue);
		} else {
			metaData.putString(key, value);
		}
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
			} else if (TAG_ACTION.equals(tagName)) {
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

	private static void parseService(XmlResourceParser parser, PackageInfoX info)
			throws XmlPullParserException, IOException {
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

			// if (TAG_INTENT_FILTER.equals(tagName)) {
			// parserIntentFilter(parser, info, component);
			// } else
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
		IntentFilterX intentInfo = new IntentFilterX();

		if (false) {
			intentInfo.readFromXml(parser);
			
			if (a.mIntentFilters == null) {
				a.mIntentFilters = new IntentFilterX[1];

				a.mIntentFilters[0] = intentInfo;
			} else {
				int len = a.mIntentFilters.length;
				IntentFilterX[] as = new IntentFilterX[len + 1];
				System.arraycopy(a.mIntentFilters, 0, as, 0, len);
				as[len] = intentInfo;

				a.mIntentFilters = as;
			}
			return;
		}
		
		// parse attr

		// parse sub-element
		int type;
		int outerDepth = parser.getDepth();
		while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
				&& (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
			if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
				continue;
			}
			
			// copy from IntentFilter
//			String tagName = parser.getName();
//            if (tagName.equals(ACTION_STR)) {
//                String name = parser.getAttributeValue(null, ACTION_STR);
//                if (name != null) {
//                	intentInfo.addAction(name);
//                }
//            } else if (tagName.equals(CAT_STR)) {
//                String name = parser.getAttributeValue(null, NAME_STR);
//                if (name != null) {
//                	intentInfo.addCategory(name);
//                }
//            } else if (tagName.equals(TYPE_STR)) {
//                String name = parser.getAttributeValue(null, NAME_STR);
//                if (name != null) {
//                    try {
//                    	intentInfo.addDataType(name);
//                    } catch (MalformedMimeTypeException e) {
//                    }
//                }
//            } else if (tagName.equals(SCHEME_STR)) {
//                String name = parser.getAttributeValue(null, NAME_STR);
//                if (name != null) {
//                	intentInfo.addDataScheme(name);
//                }
//            } else if (tagName.equals(SSP_STR)) {
//                String ssp = parser.getAttributeValue(null, LITERAL_STR);
//                if (ssp != null) {
//                	intentInfo.addDataSchemeSpecificPart(ssp, PatternMatcher.PATTERN_LITERAL);
//                } else if ((ssp=parser.getAttributeValue(null, PREFIX_STR)) != null) {
//                	intentInfo.addDataSchemeSpecificPart(ssp, PatternMatcher.PATTERN_PREFIX);
//                } else if ((ssp=parser.getAttributeValue(null, SGLOB_STR)) != null) {
//                	intentInfo.addDataSchemeSpecificPart(ssp, PatternMatcher.PATTERN_SIMPLE_GLOB);
//                }
//            } else if (tagName.equals(AUTH_STR)) {
//                String host = parser.getAttributeValue(null, HOST_STR);
//                String port = parser.getAttributeValue(null, PORT_STR);
//                if (host != null) {
//                	intentInfo.addDataAuthority(host, port);
//                }
//            } else if (tagName.equals(PATH_STR)) {
//                String path = parser.getAttributeValue(null, LITERAL_STR);
//                if (path != null) {
//                	intentInfo.addDataPath(path, PatternMatcher.PATTERN_LITERAL);
//                } else if ((path=parser.getAttributeValue(null, PREFIX_STR)) != null) {
//                	intentInfo.addDataPath(path, PatternMatcher.PATTERN_PREFIX);
//                } else if ((path=parser.getAttributeValue(null, SGLOB_STR)) != null) {
//                	intentInfo.addDataPath(path, PatternMatcher.PATTERN_SIMPLE_GLOB);
//                }
//            } else {
//                Log.w("IntentFilter", "Unknown tag parsing IntentFilter: " + tagName);
//            }

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

		if (a.mIntentFilters == null) {
			a.mIntentFilters = new IntentFilterX[1];

			a.mIntentFilters[0] = intentInfo;
		} else {
			int len = a.mIntentFilters.length;
			IntentFilterX[] as = new IntentFilterX[len + 1];
			System.arraycopy(a.mIntentFilters, 0, as, 0, len);
			as[len] = intentInfo;

			a.mIntentFilters = as;
		}

	}

	private static void parseCategory(XmlResourceParser parser,
			PackageInfoX info, IntentFilterX intentInfo) {
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
			PackageInfoX info, IntentFilterX intentInfo) {
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
		Log.d(TAG, makePrefix(depth) + "" + "orignal manifest:");
		try {
			eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_DOCUMENT) {
					Log.d(TAG, makePrefix(depth) + "");
				} else if (eventType == XmlPullParser.START_TAG) {
					depth++;
					Log.d(TAG, makePrefix(depth) + "" + parser.getName());
				} else if (eventType == XmlPullParser.END_TAG) {
					Log.d(TAG, makePrefix(depth) + "" + parser.getName());
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static String makePrefix(int depth) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < depth; i++) {
			b.append(" ");
		}
		return b.toString();
	}
}
