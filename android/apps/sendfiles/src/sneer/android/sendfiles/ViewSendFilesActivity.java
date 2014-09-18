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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		@SuppressWarnings("unchecked")
		HashMap<String, Object> map = (HashMap<String, Object>) message();
		byte[] bytes = (byte[]) map.get("contents");
		String filename = map.get("filename").toString();
		long lastModified = (long) map.get("last-modified");

		File file = new File(new File(Environment.getExternalStorageDirectory(), filename).getAbsolutePath());
		MimeTypeMap mime = MimeTypeMap.getSingleton();
		String extension = MimeTypeMap.getFileExtensionFromUrl(file.toURI().toString() );
		String type = mime.getMimeTypeFromExtension(extension);

		
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


		log(this, "felipeteste> byteslength: "  + bytes.length + 
						  "  -  filename: "     + filename + 
						  "  -  lastModified: " + lastModified + 
						  "  -  ext: "          + extension +
						  "  -  type: "         + type +
						  "  -  exists: "       + file.exists() +
						  "  -  fileUri: "      + fileUri);
		
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException ex) {
			ex.printStackTrace();
		}
	}
}
