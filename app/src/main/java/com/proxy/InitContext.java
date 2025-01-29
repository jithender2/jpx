package com.proxy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;
import android.net.Uri;
import android.util.Log;
import com.proxy.setting.Settings;
import com.proxy.ssl.KeyStoreGenerator;
import com.proxy.ssl.RootKeyStoreGenerator;
import java.io.IOException;
import java.io.OutputStream;
import android.widget.Toast;
import com.proxy.setting.KeyStoreSetting;
import com.proxy.setting.ServerSetting;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

/**
 * `InitContext` handles the initialization of the application context,
 * specifically dealing with the creation and storage of a root certificate.
 * It uses an ExecutorService to perform these operations asynchronously.
 */
public class InitContext {
	private final Context context;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	public InitContext(Context context) {
		this.context = context;
	}

	public void init() {
		executorService.execute(() -> {
			try {
				// Check if the directory already exists
				if (!isDirectoryExists("JPX")) {

					AppContext appContext = new AppContext(context);
					KeyStoreGenerator generator = appContext.getSslContextManager().getKeyStoreGenerator();
					byte[] data = generator.exportRootCert(false);
					saveCertificateToExternalStorage("JPX", data);

				}
			} catch (Exception e) {
				Log.e("InitContext", "Error during initialization", e);
				Toast.makeText(context, "Failed to initialize: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		});
	}

	private boolean isDirectoryExists(String directoryName) {
		Uri contentUri = MediaStore.Files.getContentUri("external");
		String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?";
		String[] selectionArgs = new String[] { Environment.DIRECTORY_DOCUMENTS + "/" + directoryName + "/" };

		try (Cursor cursor = context.getContentResolver().query(contentUri, null, selection, selectionArgs, null)) {
			return cursor != null && cursor.getCount() > 0;
		}
	}

	public void saveCertToExternalStorage(String fileName, byte[] data) {
		executorService.execute(() -> {
			saveToExternalStorage(fileName + ".p12", data, "application/x-pkcs12",
					Environment.DIRECTORY_DOCUMENTS + "/JPX");
		});
	}

	public void saveCertificateToExternalStorage(String fileName, byte[] data) {
		executorService.execute(() -> {
			saveToExternalStorage(fileName + ".crt", data, "application/x-x509-ca-cert",
					Environment.DIRECTORY_DOCUMENTS + "/JPX");
		});
	}

	private void saveToExternalStorage(String fileName, byte[] data, String mimeType, String directory) {
		Uri contentUri = MediaStore.Files.getContentUri("external");
		String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=? AND " + MediaStore.MediaColumns.DISPLAY_NAME
				+ "=?";
		String[] selectionArgs = { directory, fileName };

		// Check if the file already exists
		try (Cursor cursor = context.getContentResolver().query(contentUri, null, selection, selectionArgs, null)) {
			if (cursor != null && cursor.getCount() > 0) {
				Toast.makeText(context, "File already exists in external storage.", Toast.LENGTH_SHORT).show();
				return;
			}
		}

		// Create the file
		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
		contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
		contentValues.put(MediaStore.MediaColumns.TITLE, "Cert File");
		contentValues.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
		contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
		contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());

		Uri uri = context.getContentResolver().insert(contentUri, contentValues);
		if (uri != null) {
			try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
				if (outputStream != null) {
					outputStream.write(data);
					Toast.makeText(context, "File saved successfully to external storage.", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(context, "Failed to open output stream.", Toast.LENGTH_SHORT).show();
				}
			} catch (IOException e) {
				Log.e("InitContext", "Error writing file to external storage", e);
				Toast.makeText(context, "Error while writing file to external storage.", Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(context, "Failed to insert content values into MediaStore.", Toast.LENGTH_SHORT).show();
		}
	}
}