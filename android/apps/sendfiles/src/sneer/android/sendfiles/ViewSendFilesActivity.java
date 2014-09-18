package sneer.android.sendfiles;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import sneer.android.ui.MessageActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.MimeTypeMap;

public class ViewSendFilesActivity extends MessageActivity {

	private byte[] bytes;
	private String filename;
	private String extension;
	private String type;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		dataFrom(message());

		File file = fileFrom();

		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(bytes);
			bos.flush();
			bos.close();
		} catch (IOException e) {
			toast("Error preparing file (" + e.getMessage() + ")");
			finish();
			return;
		}
		
		Uri fileUri = Uri.fromFile(file);

		Intent intent = new Intent();
		intent.setDataAndType(fileUri, type);
		
		try {
			startActivity(intent);
			finish();
		} catch (ActivityNotFoundException e) {
			toast("Error opening file (" + e.getMessage() + ")");
			finish();
			return;
		}
	}

	private File fileFrom() {
		File file = new File(new File(Environment.getExternalStorageDirectory(), filename).getAbsolutePath());
		extension = MimeTypeMap.getFileExtensionFromUrl(file.toURI().toString());
		type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		return file;
	}

	private void dataFrom(Object message) {
		@SuppressWarnings("unchecked")
		HashMap<String, Object> map = (HashMap<String, Object>) message;
		bytes = (byte[]) map.get("contents");
		filename = (String)map.get("filename");
	}
}
