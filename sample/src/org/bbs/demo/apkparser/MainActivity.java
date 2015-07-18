package org.bbs.demo.apkparser;

import org.bbs.apkparser.ApkManifestParser;
import org.bbs.apkparser.PackageInfoX;
import org.bbs.apkparser.demo.R;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.util.Iterator;

public class MainActivity extends ActionBarActivity {

	private static final String TAG = MainActivity.class.getSimpleName();
	private static final int REQUEST_APP = 0;
	private static final int REQUEST_APK = 1;
	private FrameLayout mTree;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTree = (FrameLayout)findViewById(R.id.tree);
		
		String apkFile = getApplicationInfo().publicSourceDir;
		parseApk(apkFile, false);
	}

	private void parseApk(String apkFile, boolean updatePackageName) {
//		apkFile = "/storage/emulated/legacy/Download/ESdanganliulanqi_229.apk"
		PackageInfoX info = ApkManifestParser.parseAPk(this, apkFile, true, true);
		Log.d(TAG, "apkFile: " + apkFile);
		info.dump(PackageInfoX.DUMP_ALL);
		mTree.removeAllViews();
		mTree.addView(createTreeView(info));
		if (updatePackageName){
			((TextView)findViewById(R.id.desc)).setText("app: " + info.packageName);
		}

		int flags = PackageManager.GET_ACTIVITIES|PackageManager.GET_CONFIGURATIONS
				|PackageManager.GET_DISABLED_COMPONENTS| PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
				|PackageManager.GET_GIDS| PackageManager.GET_INSTRUMENTATION
				|PackageManager.GET_INTENT_FILTERS | PackageManager.GET_META_DATA
				|PackageManager.GET_PERMISSIONS | PackageManager.GET_PROVIDERS
				|PackageManager.GET_PROVIDERS | PackageManager.GET_RECEIVERS
				|PackageManager.GET_RESOLVED_FILTER | PackageManager.GET_SERVICES
				|PackageManager.GET_SHARED_LIBRARY_FILES| PackageManager.GET_SERVICES
				|PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_URI_PERMISSION_PATTERNS;
		PackageInfo pinfo = getPackageManager().getPackageArchiveInfo(apkFile, flags);

		Log.d(TAG, "pinfo: " + pinfo);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		if (id == R.id.action_pick_app){
			Intent pick = new Intent(this, AppPicker.class);
			startActivityForResult(pick, REQUEST_APP);
			return  true;
		}
		if (id == R.id.action_pick_apk){
			Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
			pick.setType("*/*");
			startActivityForResult(pick, REQUEST_APK);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (REQUEST_APP == requestCode && RESULT_OK == resultCode){
			PackageInfo info = data.getParcelableExtra("app");
			((TextView)findViewById(R.id.desc)).setText("app: " + info.packageName);
			parseApk(info.applicationInfo.publicSourceDir, false);
		}

		if (REQUEST_APK == requestCode && RESULT_OK == resultCode){
			Uri uri = data.getData();
			Log.d(TAG, "uri: " + uri);
			if ("file".equals(uri.getScheme())){
				String apkPath = uri.getPath();
				parseApk(apkPath, true);
//				parseApk(uri.toString(), true);
			}
		}
	}

	View createTreeView(PackageInfoX info){
		TreeNode root = TreeNode.root();
		TreeNode manifestNode = new TreeNode("manifest");

		TreeNode useSdk = new TreeNode("mUsesSdk");
		useSdk.addChild(new TreeNode("mMaxSdkVersion: " + info.mUsesSdk.mMaxSdkVersion));
		useSdk.addChild(new TreeNode("mMinSdkVersion: " + info.mUsesSdk.mMinSdkVersion));
		useSdk.addChild(new TreeNode("mTargetSdkVersion: " + info.mUsesSdk.mTargetSdkVersion));
		manifestNode.addChild(useSdk);
		if (info.mUsedPermissions != null && info.mUsedPermissions.length > 0) {
			TreeNode useP = new TreeNode("mUsedPermissions");
			for (PackageInfoX.UsesPermissionX p : info.mUsedPermissions){
				TreeNode pN = new TreeNode(p.name);

				useP.addChild(pN);
			}
			manifestNode.addChild(useP);
		}

		TreeNode appNode = createApplicationNode(info);
		parseMetaDataAndAdd(info.applicationInfo.metaData, appNode);
		manifestNode.addChild(appNode);

		if (info.activities != null && info.activities.length > 0){
			TreeNode actRoot = new TreeNode("activities");
			for (ActivityInfo a : info.activities){
				PackageInfoX.ActivityInfoX aX = (PackageInfoX.ActivityInfoX) a;
				TreeNode act = createActivityNode(aX);
				if (aX.mIntentFilters != null && aX.mIntentFilters.length > 0){
					TreeNode filtersN = new TreeNode("mIntentFilters");
					for (PackageInfoX.IntentFilterX f: aX.mIntentFilters){
						TreeNode filter = new TreeNode("intentfilter");
						int count = f.countActions();
						for  (int i = 0 ; i < count; i++){
							TreeNode aN = new TreeNode("action: " + f.getAction(i));
							filter.addChild(aN);
						}
						count = f.countCategories();
						for  (int i = 0 ; i < count; i++){
							TreeNode aN = new TreeNode("category: " + f.getCategory(i));
							filter.addChild(aN);
						}

						filtersN.addChild(filter);
					}

					act.addChild(filtersN);
				}
				parseMetaDataAndAdd(aX.metaData, act);
				actRoot.addChild(act);
			}

			appNode.addChild(actRoot);
		}

		root.addChild(manifestNode);

		AndroidTreeView v = new AndroidTreeView(this, root);
		v.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
		return v.getView();
	}

	private TreeNode createApplicationNode(PackageInfoX info) {
		TreeNode appNode = new TreeNode("applicationInfo");
		appNode.addChild(new TreeNode("name: " + info.applicationInfo.name));
		appNode.addChild(new TreeNode("className: " + info.applicationInfo.className));

		// packageinfo
//		if (hasSet(info.baseRevisionCode)){
//
//		}
		if (hasSet(info.firstInstallTime)){
			appNode.addChild(new TreeNode("firstInstallTime: " + info.firstInstallTime));
		}
		if (hasSet(info.installLocation)){
			appNode.addChild(new TreeNode("installLocation: " + info.installLocation));
		}
		if (hasSet(info.lastUpdateTime)){
			appNode.addChild(new TreeNode("lastUpdateTime: " + info.lastUpdateTime));
		}
		if (hasSet(info.packageName)){
			appNode.addChild(new TreeNode("packageName: " + info.packageName));
		}
		if (hasSet(info.sharedUserLabel)){
			appNode.addChild(new TreeNode("sharedUserLabel: " + info.sharedUserLabel));
		}
		if (hasSet(info.versionCode)){
			appNode.addChild(new TreeNode("versionCode: " + info.versionCode));
		}
		if (hasSet(info.packageName)){
			appNode.addChild(new TreeNode("packageName: " + info.packageName));
		}
		if (hasSet(info.packageName)){
			appNode.addChild(new TreeNode("packageName: " + info.packageName));
		}
		if (hasSet(info.versionName )){
			appNode.addChild(new TreeNode("versionName : " + info.versionName ));
		}

		// application info
		ApplicationInfo aInfo = info.applicationInfo;
		if (hasSet(aInfo.backupAgentName )){
			appNode.addChild(new TreeNode("backupAgentName : " + aInfo.backupAgentName ));
		}
		if (hasSet(aInfo.className )){
			appNode.addChild(new TreeNode("className : " + aInfo.className ));
		}
		if (hasSet(aInfo.compatibleWidthLimitDp )){
			appNode.addChild(new TreeNode("compatibleWidthLimitDp : " + aInfo.compatibleWidthLimitDp ));
		}
		if (hasSet(aInfo.dataDir )){
			appNode.addChild(new TreeNode("dataDir : " + aInfo.dataDir ));
		}
		if (hasSet(aInfo.descriptionRes )){
			appNode.addChild(new TreeNode("descriptionRes : " + aInfo.descriptionRes ));
		}
		if (hasSet(aInfo.enabled )){
			appNode.addChild(new TreeNode("enabled : " + aInfo.enabled ));
		}
		if (hasSet(aInfo.flags )){
			appNode.addChild(new TreeNode("flags : " + aInfo.flags ));
		}
		if (hasSet(aInfo.largestWidthLimitDp )){
			appNode.addChild(new TreeNode("largestWidthLimitDp : " + aInfo.largestWidthLimitDp ));
		}
		if (hasSet(aInfo.manageSpaceActivityName )){
			appNode.addChild(new TreeNode("manageSpaceActivityName : " + aInfo.manageSpaceActivityName ));
		}
		if (hasSet(aInfo.nativeLibraryDir )){
			appNode.addChild(new TreeNode("nativeLibraryDir : " + aInfo.nativeLibraryDir ));
		}
		if (hasSet(aInfo.permission )){
			appNode.addChild(new TreeNode("permission : " + aInfo.permission ));
		}
		if (hasSet(aInfo.processName )){
			appNode.addChild(new TreeNode("processName : " + aInfo.processName ));
		}
		if (hasSet(aInfo.publicSourceDir )){
			appNode.addChild(new TreeNode("publicSourceDir : " + aInfo.publicSourceDir ));
		}
		if (hasSet(aInfo.requiresSmallestWidthDp )){
			appNode.addChild(new TreeNode("requiresSmallestWidthDp : " + aInfo.requiresSmallestWidthDp ));
		}
		if (hasSet(aInfo.sharedLibraryFiles )){
			appNode.addChild(new TreeNode("sharedLibraryFiles : " + aInfo.sharedLibraryFiles ));
		}
		if (hasSet(aInfo.sourceDir )){
			appNode.addChild(new TreeNode("sourceDir : " + aInfo.sourceDir ));
		}
//		if (hasSet(aInfo.splitPublicSourceDirs )){
//			appNode.addChild(new TreeNode("splitPublicSourceDirs : " + aInfo.splitPublicSourceDirs ));
//		}
//		if (hasSet(aInfo.splitSourceDirs )){
//			appNode.addChild(new TreeNode("splitSourceDirs : " + aInfo.splitSourceDirs ));
//		}
		if (hasSet(aInfo.targetSdkVersion )){
			appNode.addChild(new TreeNode("targetSdkVersion : " + aInfo.targetSdkVersion ));
		}
		if (hasSet(aInfo.taskAffinity )){
			appNode.addChild(new TreeNode("taskAffinity : " + aInfo.taskAffinity ));
		}
		if (hasSet(aInfo.theme )){
			appNode.addChild(new TreeNode("theme : " + aInfo.theme ));
		}
		if (hasSet(aInfo.uiOptions )){
			appNode.addChild(new TreeNode("uiOptions : " + aInfo.uiOptions ));
		}
		if (hasSet(aInfo.uid )){
			appNode.addChild(new TreeNode("uid : " + aInfo.uid ));
		}

		parsePackageItem(appNode, aInfo);

		return appNode;
	}

	private void parseMetaDataAndAdd(Bundle metaData, TreeNode parent) {
		if (metaData != null){
			TreeNode nodes = new TreeNode("meta-datas");
			Iterator<String> it = metaData.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				String value = "";
				value = metaData.getString(key);
				if (null == value) {
					value = "@" + metaData.getInt(key) + "";
				}

				TreeNode node = new TreeNode("meta-data");
				node.addChildren(new TreeNode("key: " + key), new TreeNode("value: " + value));
				nodes.addChild(node);
			}
			parent.addChild(nodes);
		}
	}

	private TreeNode createActivityNode(PackageInfoX.ActivityInfoX item) {
		TreeNode act = new TreeNode(item.name);

		if (hasSet(item.configChanges)){
			TreeNode node = new TreeNode("configChanges: " + item.configChanges);
			act.addChild(node);
		}
		if (hasSet(item.documentLaunchMode)){
			TreeNode node = new TreeNode("documentLaunchMode: " + item.documentLaunchMode);
			act.addChild(node);
		}
		if (hasSet(item.flags)){
			TreeNode node = new TreeNode("flags: " + item.flags);
			act.addChild(node);
		}
		if (hasSet(item.launchMode)){
			TreeNode node = new TreeNode("launchMode: " + item.launchMode);
			act.addChild(node);
		}
		if (hasSet(item.maxRecents)){
			TreeNode node = new TreeNode("maxRecents: " + item.maxRecents);
			act.addChild(node);
		}
		if (hasSet(item.parentActivityName)){
			TreeNode node = new TreeNode("parentActivityName: " + item.parentActivityName);
			act.addChild(node);
		}
		if (hasSet(item.permission)){
			TreeNode node = new TreeNode("permission: " + item.permission);
			act.addChild(node);
		}
		if (hasSet(item.permission)){
			TreeNode node = new TreeNode("permission: " + item.permission);
			act.addChild(node);
		}
		if (hasSet(item.persistableMode)){
			TreeNode node = new TreeNode("persistableMode: " + item.persistableMode);
			act.addChild(node);
		}
		if (hasSet(item.screenOrientation)){
			TreeNode node = new TreeNode("screenOrientation: " + item.screenOrientation);
			act.addChild(node);
		}
		if (hasSet(item.softInputMode)){
			TreeNode node = new TreeNode("softInputMode: " + item.softInputMode);
			act.addChild(node);
		}
		if (hasSet(item.targetActivity)){
			TreeNode node = new TreeNode("targetActivity: " + item.targetActivity);
			act.addChild(node);
		}
		if (hasSet(item.taskAffinity)){
			TreeNode node = new TreeNode("taskAffinity: " + item.taskAffinity);
			act.addChild(node);
		}
		if (hasSet(item.theme)){
			TreeNode node = new TreeNode("theme: " + item.theme);
			act.addChild(node);
		}
		if (hasSet(item.uiOptions)){
			TreeNode node = new TreeNode("uiOptions: " + item.uiOptions);
			act.addChild(node);
		}

		parseComponent(act, item);

		return act;
	}

	private void parseComponent(TreeNode root, ComponentInfo item) {
		if (hasSet(item.descriptionRes)){
			TreeNode node = new TreeNode("descriptionRes: " + item.descriptionRes);
			root.addChild(node);
		}
		if (hasSet(item.enabled)){
			TreeNode node = new TreeNode("enabled: " + item.enabled);
			root.addChild(node);
		}
		if (hasSet(item.exported)){
			TreeNode node = new TreeNode("exported: " + item.exported);
			root.addChild(node);
		}
		if (hasSet(item.descriptionRes)){
			TreeNode node = new TreeNode("descriptionRes: " + item.descriptionRes);
			root.addChild(node);
		}
		parsePackageItem(root, item);
	}

	private void parsePackageItem(TreeNode root, PackageItemInfo item) {
		if (hasSet(item.banner)) {
			TreeNode node = new TreeNode("banner: " + item.banner);
			root.addChild(node);
		}
		if (hasSet(item.icon)) {
			TreeNode node = new TreeNode("icon: " + item.icon);
			root.addChild(node);
		}
		if (hasSet(item.labelRes)) {
			TreeNode node = new TreeNode("labelRes: " + item.labelRes);
			root.addChild(node);
		}
		if (hasSet(item.logo)) {
			TreeNode node = new TreeNode("logo: " + item.logo);
			root.addChild(node);
		}
		if (hasSet(item.name)) {
			TreeNode node = new TreeNode("name: " + item.name);
			root.addChild(node);
		}
		if (hasSet(item.nonLocalizedLabel)) {
			TreeNode node = new TreeNode("nonLocalizedLabel: " + item.nonLocalizedLabel);
			root.addChild(node);
		}
		if (hasSet(item.packageName)) {
			TreeNode node = new TreeNode("packageName: " + item.packageName);
			root.addChild(node);
		}
	}

	boolean hasSet(Object o){
		if (o instanceof Integer){
			return 0 != o;
		}
		if (o instanceof String){
			return !TextUtils.isEmpty((String)o);
		}

		return false;
	}

}
