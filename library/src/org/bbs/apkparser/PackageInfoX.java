package org.bbs.apkparser;

import java.util.Iterator;

import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

/**
 * all new-added member MUST has a 'm" prefix; all new-added class MUST has a
 * 'X' suffix.
 * 
 * @author bysong
 *
 */
public class PackageInfoX extends PackageInfo {
	private static final String TAG = ApkManifestParser.TAG;
	public static final int DUMP_APPLICATION = 1 << 0;
	public static final int DUMP_ACTIVITY = 1 << 1 | DUMP_APPLICATION;
	public static final int DUMP_USES_SDK = 1 << 2;
	public static final int DUMP_SERVICE = 1 << 3 | DUMP_APPLICATION;
	public static final int DUMP_PERMISSION = 1 << 4;
	public static final int DUMP_META_DATA = 1 << 5 | DUMP_APPLICATION
			| DUMP_SERVICE | DUMP_ACTIVITY;

	public static final int DUMP_ALL = 0xEFFF;
	public static final int FLAG_DUMP = DUMP_ALL;

	public PackageInfoX.UsesSdkX mUsesSdk;
	public UsesPermissionX[] mUsedPermissions;
	public PermissionGroupInfo[] mPermissionGroups;
	public PermissionTreeX[] mPermissionTrees;

	// evaluate by application.
	public String mLibPath;

	public static boolean hasFlag(int flag, int mask) {
		return (flag & mask) == mask;
	}

	static void dumpMetaData(int level, Bundle metaData, int flag) {
		if (hasFlag(flag, DUMP_META_DATA)) {

			if (metaData != null) {
				Log.d(ApkManifestParser.TAG,
						ApkManifestParser.makePrefix(level) + "metaData: ");
				level++;
				Iterator<String> it = metaData.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					String value = "";
					value = metaData.getString(key);
					if (null == value) {
						value = "@" + metaData.getInt(key) + "";
					}
					Log.d(ApkManifestParser.TAG,
							ApkManifestParser.makePrefix(level) + key + ": "
									+ value);
				}
			}
		}
	}

	public void dump(int flag) {
		dump(0, flag);
	}

	private void dump(int level, int flag) {
		Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level)
				+ "parsed manifest:");
		level = level + 1;
		String prefix = ApkManifestParser.makePrefix(level);
		Log.d(ApkManifestParser.TAG, prefix + "versionCode   : "+ versionCode);
		Log.d(ApkManifestParser.TAG, prefix + "versionCode   : "+ versionName);
		if (!TextUtils.isEmpty(sharedUserId)){
			Log.d(ApkManifestParser.TAG, prefix + "sharedUserId   : "+ sharedUserId);
		}
		if (sharedUserLabel > 0){
			Log.d(ApkManifestParser.TAG, prefix + "sharedUserLabel: "+ sharedUserLabel);
		}
		if (mUsesSdk != null) {
			mUsesSdk.dump(level, flag);
		}

		if (hasFlag(flag, DUMP_PERMISSION)) {
			if (permissions != null && permissions.length > 0) {
				Log.d(ApkManifestParser.TAG,
						ApkManifestParser.makePrefix(level) + "permissions: ");
				for (PermissionInfo p : permissions) {
					prefix = ApkManifestParser.makePrefix(level + 1);
					Log.d(ApkManifestParser.TAG, prefix + "permission;");
					prefix = ApkManifestParser.makePrefix(level + 2);
					Log.d(ApkManifestParser.TAG, prefix + "name           : "
							+ p.name);
					Log.d(ApkManifestParser.TAG, prefix + "protectionLevel: "
							+ p.protectionLevel);
					Log.d(ApkManifestParser.TAG, prefix + "descriptionRes : "
							+ p.descriptionRes);
					Log.d(ApkManifestParser.TAG, prefix + "group          : "
							+ p.group);
				}
			}

			if (mPermissionGroups != null && mPermissionGroups.length > 0) {
				Log.d(ApkManifestParser.TAG,
						ApkManifestParser.makePrefix(level)
								+ "permissionGroup: ");
				for (PermissionGroupInfo p : mPermissionGroups) {
					prefix = ApkManifestParser.makePrefix(level + 1);
					Log.d(ApkManifestParser.TAG, prefix + "permissiongroup: "
							+ p.name);
					prefix = ApkManifestParser.makePrefix(level + 2);
					Log.d(ApkManifestParser.TAG, prefix + "name           : "
							+ p.name);
					Log.d(ApkManifestParser.TAG, prefix + "descriptionRes : "
							+ p.descriptionRes);
					Log.d(ApkManifestParser.TAG, prefix + "descriptionRes : "
							+ p.labelRes);
				}
			}
		}

		if (applicationInfo != null && ApplicationInfoX.shouldLog(flag)) {
			Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level)
					+ "application: ");
			((PackageInfoX.ApplicationInfoX) applicationInfo).dump(level + 1,
					flag);
		}

		if (activities != null && activities.length > 0
				&& ActivityInfoX.shouldLog(flag)) {
			Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level)
					+ "activities: ");
			for (ActivityInfo a : activities) {
				((PackageInfoX.ActivityInfoX) a).dump(level + 1, flag);
			}
		}

		if (services != null && services.length > 0
				&& ServiceInfoX.shouldLog(flag)) {
			Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level)
					+ "services: ");
			for (ServiceInfo a : services) {
				((PackageInfoX.ServiceInfoX) a).dump(level + 1, flag);
			}
		}
	}

	public static class ApplicationInfoX extends ApplicationInfo {

		public boolean mDebuggable;
		public boolean mAllowTaskReparenting;

		public void dump(int level, int flag) {
			String prefix = ApkManifestParser.makePrefix(level);
			Log.d(ApkManifestParser.TAG, prefix + "packageName: " + packageName);
			Log.d(ApkManifestParser.TAG, prefix + "theme      : " + theme);

			PackageInfoX.dumpMetaData(level, metaData, flag);
		}

		public void dump() {
			dump(0, DUMP_APPLICATION);
		}

		public static boolean shouldLog(int flag) {
			return hasFlag(flag, DUMP_APPLICATION);
		}
	}

	public static class ActivityInfoX extends ActivityInfo implements
			Parcelable {
		public PackageInfoX.IntentFilterX[] mIntentFilters;
		public PackageInfoX mPackageInfo;

		public int describeContents() {
			return 0;
		}

		public void dump(int level, int flag) {
			Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level)
					+ "activity:");
			String prefix = ApkManifestParser.makePrefix(level + 1);
			Log.d(ApkManifestParser.TAG, prefix + "name : " + name);
			Log.d(ApkManifestParser.TAG, prefix + "icon : " + icon);
			Log.d(ApkManifestParser.TAG, prefix + "theme: " + theme);

			PackageInfoX.dumpMetaData(level + 1, metaData, flag);
			
			if (mIntentFilters != null && mIntentFilters.length > 0) {
				
			}
		}

		public void dump() {
			dump(0, DUMP_ACTIVITY);
		}

		public static boolean shouldLog(int flag) {
			return hasFlag(flag, DUMP_ACTIVITY);
		}

		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeParcelableArray(mIntentFilters, flags);
		}

		public static final Parcelable.Creator<PackageInfoX.ActivityInfoX> CREATOR = new Parcelable.Creator<PackageInfoX.ActivityInfoX>() {
			public PackageInfoX.ActivityInfoX createFromParcel(Parcel in) {
				return new ActivityInfoX(in);
			}

			public PackageInfoX.ActivityInfoX[] newArray(int size) {
				return new PackageInfoX.ActivityInfoX[size];
			}
		};

		public ActivityInfoX() {

		}

		private ActivityInfoX(Parcel in) {
			// super(in);
			// mData = in.readInt();
		}

	}

	public static class ServiceInfoX extends ServiceInfo {
		public PackageInfoX mPackageInfo;

		public void dump(int level, int flag) {
			Log.d(ApkManifestParser.TAG,
					ApkManifestParser.makePrefix(level + 1) + "service: ");
			Log.d(ApkManifestParser.TAG,
					ApkManifestParser.makePrefix(level + 1) + "  name : "
							+ name);

			PackageInfoX.dumpMetaData(level + 1, metaData, flag);
		}

		public void dump() {
			dump(0, DUMP_SERVICE);
		}

		public static boolean shouldLog(int flag) {
			return hasFlag(flag, DUMP_SERVICE);
		}

	}

	public static class IntentFilterX extends IntentFilter {
		
		public void dump(int level, int flag){
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG,
						ApkManifestParser.makePrefix(level) + "intentfilter: ");
			String prefix = ApkManifestParser.makePrefix(level + 1);
			if (shouldLog(flag)) {
				int count = countActions();
				for (int i = 0; i < count ; i++) {
					Log.d(ApkManifestParser.TAG, prefix + "action     : "
							+ getAction(i));
				}
				count = countCategories();
				for (int i = 0; i < count ; i++) {
					Log.d(ApkManifestParser.TAG, prefix + "category   : "
							+ getCategory(i));
				}
				// TODO
			}
		}

		private boolean shouldLog(int flag) {
			return (flag & DUMP_ACTIVITY) != 0;
		}
	}

	public static class UsesSdkX {
		public int mMinSdkVersion;
		public int mTargetSdkVersion;
		public int mMaxSdkVersion;

		public void dump(int level, int flag) {
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG,
						ApkManifestParser.makePrefix(level) + "mUseSdk: ");
			String prefix = ApkManifestParser.makePrefix(level + 1);
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG, prefix + "mMinSdkVersion     : "
						+ mMinSdkVersion);
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG, prefix + "mTargetSdkVersion  : "
						+ mTargetSdkVersion);
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG, prefix + "mMaxSdkVersion     : "
						+ mMaxSdkVersion);
		}

		public static boolean shouldLog(int flag) {
			return hasFlag(flag, DUMP_USES_SDK);
		}
	}

	public static class UsesPermissionX {
		public String mName;
		public int mMaxSdkVersion;

		public void dump(int level, int flag) {
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG,
						ApkManifestParser.makePrefix(level)
								+ "mUsePermission: ");
			String prefix = ApkManifestParser.makePrefix(level + 1);
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG, prefix + "mName          : "
						+ mName);
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG, prefix + "mMaxSdkVersion : "
						+ mMaxSdkVersion);
		}

		public void dump() {
			dump(0, DUMP_PERMISSION);
		}

		public static boolean shouldLog(int flag) {
			return hasFlag(flag, DUMP_PERMISSION);
		}
	}

	public static class PermissionTreeX {
		public String mName;
		public int mLabelRes;

		public void dump(int level, int flag) {
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG,
						ApkManifestParser.makePrefix(level)
								+ "mUsePermission: ");
			String prefix = ApkManifestParser.makePrefix(level + 1);
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG, prefix + "mName  : " + mName);
			if (shouldLog(flag))
				Log.d(ApkManifestParser.TAG, prefix + "mLabel : " + mLabelRes);
		}

		public static boolean shouldLog(int flag) {
			return hasFlag(flag, DUMP_PERMISSION);
		}

	}
	
	public static class ActionX {
		
	}
}
