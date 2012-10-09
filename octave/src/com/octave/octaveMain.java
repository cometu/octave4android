package com.octave;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;
import android.widget.Toast;

public class octaveMain extends Activity {

	private ProgressDialog mPd_ring;
	private boolean mAlreadyStarted;
	private Toast mToast;
	private int mBufferSize = 1024;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main); 
		TextView textView = (TextView)findViewById(R.id.myTextView);
		textView.setText("Octave launching via Android Terminal Emulator\n\nIf this fails:\nFor now, users are required to have a recent version of the Android Terminal Emulator installed before installing Octave.  Please uninstall Octave, confirm you have Android Terminal Emulator installed and up to date and then reinstall Octave.");
		mToast = Toast.makeText(this, "For now, users are required to have a recent version of the Android Terminal Emulator installed before installing Octave.  Please uninstall Octave, confirm you have Android Terminal Emulator installed and up to date and then reinstall Octave.", Toast.LENGTH_LONG);

		mAlreadyStarted = false;

		ViewTreeObserver viewTreeObserver = textView.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) {
			viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					if (mAlreadyStarted == false) {
						mAlreadyStarted = true;
						mPd_ring = ProgressDialog.show(octaveMain.this, "Unpacking Octave", "This may take a while (several minutes if this is the first time).",true);
						mPd_ring.setCancelable(false);
						Thread t = new Thread() {
							public void run() {
								try {
									unpackAll();
								} catch (Exception e) {
									Log.e("LongToast", "", e);
								}
							}
						};
						t.start();
					}
				}
			});
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);

		if(getIntent().hasExtra("returnFromMeasure")) {
			Intent i2 = new Intent("jackpal.androidterm.RUN_SCRIPT");
			i2.addCategory(Intent.CATEGORY_DEFAULT);
			i2.putExtra("jackpal.androidterm.iInitialCommand", "cd /data/data/com.octave/; /data/data/com.octave/mylib/ld-linux.so.3 --library-path /data/data/com.octave/mylib /data/data/com.octave/bin/octave");
			//i.putExtra("jackpal.androidterm.iInitialCommand", "/data/data/com.octave/mylib/ld-linux.so.3 /data/data/com.octave/bin/octave");
			try {
				startActivity(i2);
			} catch (ActivityNotFoundException e1) {
				fireLongToast();
				//To do: send them to the market in the future
			} catch (SecurityException e2) {
				fireLongToast();
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (requestCode == 0) {
			if (resultCode == 0) {
				kickItOff();
			}
		}
	}

	private void goMeasure() {
		mPd_ring.dismiss();
		// opens a new window and runs "echo 'Hi there!'"
		// application must declare jackpal.androidterm.permission.RUN_SCRIPT in manifest
		Intent i1 = new Intent();
		i1.setClassName("com.droidplot", "com.droidplot.droidplotMain");
		i1.putExtra("measureAndExit",true);
		try {
			startActivityForResult(i1,0);
		} catch (ActivityNotFoundException e) {
			//TODO: Should give them information that plotting is disabled
			kickItOff();
		}
	}

	private void kickItOff() {
		// opens a new window and runs "echo 'Hi there!'"
		// application must declare jackpal.androidterm.permission.RUN_SCRIPT in manifest
		Intent i2 = new Intent("jackpal.androidterm.RUN_SCRIPT");
		i2.addCategory(Intent.CATEGORY_DEFAULT);
		i2.putExtra("jackpal.androidterm.iInitialCommand", "cd /data/data/com.octave/; /data/data/com.octave/mylib/ld-linux.so.3 --library-path /data/data/com.octave/mylib /data/data/com.octave/bin/octave");
		//i.putExtra("jackpal.androidterm.iInitialCommand", "/data/data/com.octave/mylib/ld-linux.so.3 /data/data/com.octave/bin/octave");
		try {
			startActivity(i2);
		} catch (ActivityNotFoundException e1) {
			fireLongToast();
			//To do: send them to the market in the future
		} catch (SecurityException e2) {
			fireLongToast();
		}
	}

	private void fireLongToast() {
		Thread t = new Thread() {
			public void run() {
				int count = 0;
				try {
					while (count < 5) {
						mToast.show();
						sleep(2500);
						count++;
					}
				} catch (Exception e) {
					Log.e("LongToast", "", e);
				}
			}
		};
		t.start();
	}


	private void unpackAll() {

		String version;

		try {
			PackageInfo pi = getPackageManager().getPackageInfo("com.addi", 0);
			version = pi.versionName;     // this is the line Eclipse complains
		}
		catch (PackageManager.NameNotFoundException e) {
			// eat error, for testing
			version = "?";
		}

		File versionFile = new File("/data/data/com.octave/unzippedFiles/version");
		if (versionFile.exists()==false) {
			Runtime runtime = Runtime.getRuntime(); 
			Process process;
			try {
				process = runtime.exec("chmod 0755 /data/data/com.octave");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				process = runtime.exec("chmod 0755 /data/data/com.octave/lib");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			} 

			File unzipList = new File("/data/data/com.octave/unzippedFiles/");
			if (unzipList.exists()==false) { 
				unzipList.mkdir();
			}

			File tmpDir = new File("/data/data/com.octave/bin/");
			if (tmpDir.exists()==false) { 
				tmpDir.mkdir();
			}
			try {
				process = runtime.exec("rm -f /data/data/com.octave/bin/*");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				process = runtime.exec("chmod 0755 /data/data/com.octave/bin");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			tmpDir = new File("/data/data/com.octave/mylib/");
			if (tmpDir.exists()==false) { 
				tmpDir.mkdir();
			}
			try {
				process = runtime.exec("rm -f /data/data/com.octave/mylib/*;");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				process = runtime.exec("chmod 0777 /data/data/com.octave/mylib");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			tmpDir = new File("/data/data/com.octave/tmp/");
			if (tmpDir.exists()==false) { 
				tmpDir.mkdir();
			}
			try {
				process = runtime.exec("chmod 0777 /data/data/com.octave/tmp");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			File tmpOctRc = new File("/data/data/com.octave/.octaverc");
			if (tmpOctRc.exists()==false) { 
				try {
					tmpOctRc.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				process = runtime.exec("chmod 0777 /data/data/com.octave/.octaverc");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			File fileList = new File("/data/data/com.octave/lib/"); 
			if (fileList != null) { 
				File[] filenames = fileList.listFiles(); 
				for (File tmpf : filenames) { 
					String fileName = tmpf.getName();
					if (fileName.startsWith("lib__")) {
						String[] splitStr;
						splitStr = fileName.substring(0,fileName.length()-3).split("__");  //drop .so and split
						String newFileName = splitStr[2];  //build up correct filename
						for(int i=3; i < splitStr.length ; i++) {
							newFileName = newFileName + "." + splitStr[i];
						}
						try {
							process = runtime.exec("ln -s /data/data/com.octave/lib/" + fileName + " /data/data/com.octave/"+splitStr[1]+"/"+newFileName);
							try {
								process.waitFor();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					} else if (fileName.startsWith("libzip")) {
						boolean alreadyUnpacked = false;
						File[] unzipnames = unzipList.listFiles(); 
						for (File tmpUnzipName: unzipnames) {
							String unzipName = tmpUnzipName.getName();
							if (unzipName.equalsIgnoreCase(fileName)) {
								alreadyUnpacked = true;
							}
						}
						if (alreadyUnpacked == false) {
							unzipFile(fileName);
							File tmpFile = new File("/data/data/com.octave/unzippedFiles/" + fileName);
							try {
								tmpFile.createNewFile();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			try {
				versionFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		goMeasure();
	}

	private void unzipFile(String filename) { 
		String unzipLocation = "/data/data/com.octave/";
		String filePath = "/data/data/com.octave/lib/";

		String fullPath = filePath + filename; 
		Log.d("UnZip", "unzipping " + fullPath + " to " + unzipLocation); 
		try {
			unzip(fullPath, unzipLocation);
		} catch (IOException e) {
			Log.d("UnZip", "Failed");
		} 
	}

	public void unzip(String zipFile, String location) throws IOException {
		int size;
		byte[] buffer = new byte[mBufferSize];

		String[] splitPath = zipFile.split("/");
		String[] splitExtension = splitPath[splitPath.length-1].substring(6).split(".so");
		String[] splitVersion = splitExtension[0].split("_");
		deleteDir(new File(location + "/" + splitVersion[0]));

		try {
			File f = new File(location);
			if(!f.isDirectory()) {
				f.mkdirs();
			}
			ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), mBufferSize));
			try {
				ZipEntry ze = null;
				while ((ze = zin.getNextEntry()) != null) {
					String path = location + ze.getName();

					File unzipFile = new File(path);
					if (!unzipFile.getParentFile().exists()) {
						Log.v("Unzip", "create parents " + unzipFile.getName());
						createDir(unzipFile.getParentFile()); 
					}
					if (!unzipFile.exists()) {
						if (ze.isDirectory()) {
							Log.v("Unzip", "found directory " + unzipFile.getName());
							if(!unzipFile.isDirectory()) {
								createDir(unzipFile);
							}
						}	else {	
							Log.v("Unzip", "found file " + unzipFile.getName());
							FileOutputStream out = new FileOutputStream(path, false);
							BufferedOutputStream fout = new BufferedOutputStream(out, mBufferSize);
							try {
								while ( (size = zin.read(buffer, 0, mBufferSize)) != -1 ) {
									fout.write(buffer, 0, size);
								}

								zin.closeEntry();
							}
							finally {
								fout.flush();
								fout.close();
								Log.v("Unzip", "changing permissions " + unzipFile.getName());
								Runtime runtime = Runtime.getRuntime(); 
								Process process;
								try {
									process = runtime.exec("chmod 0777 " + unzipFile.getAbsolutePath());
									try {
										process.waitFor();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
						}
					}
				}
			}
			finally {
				zin.close();
			}
		}
		catch (Exception e) {
			Log.e("main", "Unzip exception", e);
		}
	}

	public boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return dir.delete();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{ 
		super.onConfigurationChanged(newConfig);
	}

	private void createDir(File dir) {
		if (!dir.getParentFile().exists()) { 
			createDir(dir.getParentFile()); 
		}
		if (dir.exists()) { 
			return; 
		} 
		Log.v("Unzip", "Creating dir " + dir.getName()); 
		if (!dir.mkdir()) { 
			throw new RuntimeException("Can not create dir " + dir); 
		}
		Runtime runtime = Runtime.getRuntime(); 
		Process process;
		try {
			process = runtime.exec("chmod 0777 " + dir.getAbsolutePath());
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} 
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		File directory = new File("/data/data/com.octave/tmp");

		// Get all files in directory
		if (directory.exists()) {
			File[] files = directory.listFiles();
			for (File file : files)
			{
				// Delete each file
				if (!file.delete())
				{
					// Failed to delete file
					System.out.println("Failed to delete "+file);
				}
			}
		}
		finish();
	}

}