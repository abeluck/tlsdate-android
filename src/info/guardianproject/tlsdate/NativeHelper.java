package info.guardianproject.tlsdate;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

public class NativeHelper {
	public static final String TAG = "NativeHelper";

	public static File app_opt; // an /opt tree for the UNIX cmd line tools
	public static File app_log; // a place to store logs
	public static File app_home; // dir for $HOME

	// full paths to key executables, with globally used flags
	public static String tlsdate;

	public static String sdcard;
	public static String[] envp; // environment variables

	private static String config_file = "/data/data/info.guardianproject.tlsdate/app_opt/etc/tlsdate/ca-roots/tlsdate-ca-roots.conf";
	private static Context context;

	public static void setup(Context c) {
		context = c;
		app_opt = context.getDir("opt", Context.MODE_WORLD_READABLE).getAbsoluteFile();
		app_log = context.getDir("log", Context.MODE_PRIVATE).getAbsoluteFile();
		app_home = context.getDir("home", Context.MODE_PRIVATE).getAbsoluteFile();

		File bin = new File(app_opt, "bin");
		tlsdate = new File(bin, "tlsdate").getAbsolutePath() + " -C " + config_file + " --verbose --showtime --dont-set-clock ";

		sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
		String ldLibraryPath = System.getenv("LD_LIBRARY_PATH");
		String path = System.getenv("PATH");
		envp = new String[] { "HOME=" + NativeHelper.app_home,
				"LD_LIBRARY_PATH=" + ldLibraryPath + ":" + NativeHelper.app_opt + "/lib",
				"PATH=" + path + ":" + bin.getAbsolutePath(),
				"app_opt=" + app_opt.getAbsolutePath() };
		Log.i(TAG, "Finished NativeHelper.setup()");
	}


	private static void copyFileOrDir(String path, File dest) {
		AssetManager assetManager = context.getAssets();
		String assets[] = null;
		try {
			assets = assetManager.list(path);
			if (assets.length == 0) {
				copyFile(path, dest);
			} else {
				File destdir = new File(dest, new File(path).getName());
				if (!destdir.exists())
					destdir.mkdirs();
				for (int i = 0; i < assets.length; ++i) {
					copyFileOrDir(new File(path, assets[i]).getPath(), destdir);
				}
			}
		} catch (IOException ex) {
			Log.e(TAG, "I/O Exception", ex);
		}
	}

	private static void copyFile(String filename, File dest) {
		AssetManager assetManager = context.getAssets();

		InputStream in = null;
		OutputStream out = null;
		try {
			in = assetManager.open(filename);
			out = new FileOutputStream(new File(app_opt, filename).getAbsolutePath());

			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		} catch (Exception e) {
			Log.e(TAG, filename + ": " + e.getMessage());
		}

	}

	/*
	 * since we are using the whole gnupg package of programs and libraries, we
	 * are setting up the whole UNIX directory tree that it expects
	 */
	private static void setupEmptyDirs() {
		new File(app_opt, "etc/gnupg/trusted-certs").mkdirs();
		new File(app_opt, "share/gnupg/extra-certs").mkdirs();
		new File(app_opt, "var/run/gnupg").mkdirs();
		new File(app_opt, "var/cache/gnupg").mkdirs();
		// /home is outside of this tree, in app_home
	}

	/* write out a sh .profile file to ease testing in the terminal */
	private static void writeShProfile() {
		File etc_profile = new File(app_opt, "etc/profile");
		String global = "";
		for (String s : envp) {
			global += "export " + s + "\n";
		}
		File home_profile = new File(app_home, ".profile");
		String local = ". " + etc_profile.getAbsolutePath() + "\n. "
				+ new File(app_home, ".gpg-agent-info").getAbsolutePath() + "\n"
				+ "export GPG_AGENT_INFO\n" + "export SSH_AUTH_SOCK\n";
		try {
			FileWriter outFile = new FileWriter(etc_profile);
			PrintWriter out = new PrintWriter(outFile);
			out.println(global);
			out.close();
			outFile = new FileWriter(home_profile);
			out = new PrintWriter(home_profile);
			out.println(local);
			out.close();
		} catch (Exception e) {
			Log.e(TAG, "Cannot write file: ", e);
		}
	}


	public static void unpackAssets(Context context) {
		Log.i(TAG, "Setting up assets in " + app_opt);
		setupEmptyDirs();
		writeShProfile();

		AssetManager am = context.getAssets();
		final String[] assetList;
		try {
			assetList = am.list("");
		} catch (IOException e) {
			Log.e(TAG, "cannot get asset list", e);
			return;
		}
		// unpack the assets to app_opt
		for (String asset : assetList) {
			if (asset.equals("images")
					|| asset.equals("sounds")
					|| asset.equals("webkit")
					|| asset.equals("databases")  // Motorola
					|| asset.equals("kioskmode")) // Samsung
				continue;
			Log.i(TAG, "copying asset: " + asset);
			copyFileOrDir(asset, app_opt);
		}

		chmod("0755", app_opt, true);
	}


	public static void chmod(String modestr, File path) {
		Log.i(TAG, "chmod " + modestr + " " + path.getAbsolutePath());
		try {
			Class<?> fileUtils = Class.forName("android.os.FileUtils");
			Method setPermissions = fileUtils.getMethod("setPermissions", String.class,
					int.class, int.class, int.class);
			int mode = Integer.parseInt(modestr, 8);
			int a = (Integer) setPermissions.invoke(null, path.getAbsolutePath(), mode,
					-1, -1);
			if (a != 0) {
				Log.i(TAG, "ERROR: android.os.FileUtils.setPermissions() returned " + a
						+ " for '" + path + "'");
			}
		} catch (ClassNotFoundException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (IllegalAccessException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (InvocationTargetException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (NoSuchMethodException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		}
	}

	public static void chmod(String mode, File path, boolean recursive) {
		chmod(mode, path);
		if (recursive) {
			File[] files = path.listFiles();
			for (File d : files) {
				if (d.isDirectory()) {
					Log.i(TAG, "chmod recurse: " + d.getAbsolutePath());
					chmod(mode, d, true);
				} else {
					chmod(mode, d);
				}
			}
		}
	}

}
