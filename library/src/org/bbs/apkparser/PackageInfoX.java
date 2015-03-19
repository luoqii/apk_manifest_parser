package org.bbs.apkparser;

import java.util.Iterator;

import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
	 * all new-added member MUST has a 'm" prefix.
	 * 
	 * @author bysong
	 *
	 */
	public class PackageInfoX extends PackageInfo {
		public static final int DUMP_APPLICATION = 1 << 0;
		public static final int DUMP_ACTIVITY = 1 << 1 | DUMP_APPLICATION;
		public static final int DUMP_USES_SDK = 1 << 2;
		public static final int DUMP_META_DATA = 1 << 3 | DUMP_APPLICATION;
		public static final int DUMP_SERVICE = 1 << 4 | DUMP_APPLICATION;
		
		public static final int DUMP_ALL = 0xEFFF;
		public static final int FLAG_DUMP = DUMP_ALL;
		
		public PackageInfoX.UsesSdkX mUsesSdk;
		
		// evaluate by application.
		public String mLibPath;
		
		public static boolean hasFlag(int flag, int mask) {
			return (flag & mask) == mask;
		}

		public static class ApplicationInfoX extends ApplicationInfo {

			public boolean mDebuggable;
			public boolean mAllowTaskReparenting;

			public void dump(int level, int flag) {	
				level++;
				String prefix = ApkManifestParser.makePrefix(level);
				if (shouldLog(flag)) Log.d(ApkManifestParser.TAG, prefix + "packageName: " + packageName);
				if (shouldLog(flag)) Log.d(ApkManifestParser.TAG, prefix + "theme      : " + theme);

				PackageInfoX.dumpMetaData(level, metaData, flag);
			}

			public static boolean shouldLog(int flag)	 {
				return hasFlag(flag, DUMP_APPLICATION);
			}
		}

		public static class ActivityInfoX extends ActivityInfo 
		implements Parcelable 
		{
			public PackageInfoX.IntentInfoX[] mIntents;
			public PackageInfoX mPackageInfo;

			public int describeContents() {
				return 0;
			}

			public void dump(int level, int flag) {
				Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level) + "activity:");
				String prefix = ApkManifestParser.makePrefix(level + 1);
				if (shouldLog(flag)) Log.d(ApkManifestParser.TAG, prefix + "name : " + name);
				if (shouldLog(flag)) Log.d(ApkManifestParser.TAG, prefix + "icon : " + icon);
				if (shouldLog(flag)) Log.d(ApkManifestParser.TAG, prefix + "theme: " + theme);
				
				PackageInfoX.dumpMetaData(level + 1, metaData, flag);
			}

			public static boolean shouldLog(int flag)	 {
				return hasFlag(flag, DUMP_ACTIVITY);
			}
			
			public void writeToParcel(Parcel out, int flags) {
				super.writeToParcel(out, flags);
				out.writeParcelableArray(mIntents, flags);
			}

			public static final Parcelable.Creator<PackageInfoX.ActivityInfoX> CREATOR = new Parcelable.Creator<PackageInfoX.ActivityInfoX>() {
				public PackageInfoX.ActivityInfoX createFromParcel(Parcel in) {
					return new ActivityInfoX(in);
				}

				public PackageInfoX.ActivityInfoX[] newArray(int size) {
					return new PackageInfoX.ActivityInfoX[size];
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
				if ((shouldLog(flag))) Log.d(ApkManifestParser.TAG, "service: ");
				if (shouldLog(flag)) Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level + 1) + "name : " + name);

				PackageInfoX.dumpMetaData(level + 2, metaData, flag);
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
				if (shouldLog(flag)) Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level) + "mUseSdk: ");
                String prefix = ApkManifestParser.makePrefix(level++);
				if (shouldLog(flag)) Log.d(ApkManifestParser.TAG, prefix + "mMinSdkVersion     : " + mMinSdkVersion);
				if (shouldLog(flag)) Log.d(ApkManifestParser.TAG, prefix + "mTargetSdkVersion  : " + mTargetSdkVersion);
				if (shouldLog(flag)) Log.d(ApkManifestParser.TAG, prefix + "mMaxSdkVersion     : " + mMaxSdkVersion);
			}
			
			public static boolean shouldLog(int flag)	 {
				return hasFlag(flag, DUMP_USES_SDK);
			}
		}
		
		static void dumpMetaData(int level, Bundle metaData, int flag){
			if (hasFlag(flag, DUMP_META_DATA)) {

				if (metaData != null) {
					Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level) + "metaData: ");
					level++;
					Iterator<String> it = metaData.keySet().iterator();
					while (it.hasNext()) {
						String key = it.next();
						Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level) + key + ": " + metaData.getString(key));
					}
				}
			}
		}
		
		public void dump() {
			dump(0, FLAG_DUMP);
		}
		
		public void dump(int level, int flag) {
			Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level) + "parsed manifest:");
			level = level + 1;
			if (mUsesSdk != null) {
				mUsesSdk.dump(level, flag);
			}
			
			if (applicationInfo != null && ApplicationInfoX.shouldLog(flag)) {
				Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level) + "application: ");
				((PackageInfoX.ApplicationInfoX)applicationInfo).dump(level + 1, flag);
			}
			
			if (activities != null && activities.length > 0 && ActivityInfoX.shouldLog(flag)) {
				Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level) + "activities: ");
				for (ActivityInfo a : activities) {
					((PackageInfoX.ActivityInfoX)a).dump(level + 1, flag);
				}
			}
			
			if (services != null && services.length > 0 && ServiceInfoX.shouldLog(flag)) {
				Log.d(ApkManifestParser.TAG, ApkManifestParser.makePrefix(level) + "services: ");
				for (ServiceInfo a : services) {
					((PackageInfoX.ServiceInfoX)a).dump(level + 1, flag);
				}
			}
		}
	}