package sneer.android.sendfiles;

import java.io.File;
import java.util.HashMap;

import sneer.android.ui.MessageActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;

public class ViewSendFilesActivity extends MessageActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		HashMap<String, Object> map = (HashMap<String, Object>) message();
		toast(map.get("contents"));
		toast(map.get("filename"));
		toast(map.get("last-modified"));
		
//		File file = new File(new File(System.getProperty("java.io.tmpdir"), "voicemessage.3gp").getAbsolutePath());
//
//		MimeTypeMap mime = MimeTypeMap.getSingleton();
//		int index = file.getName().lastIndexOf('.') + 1;
//		String ext = file.getName().substring(index).toLowerCase();
//		String type = mime.getMimeTypeFromExtension(ext);
//
//		Intent intent = new Intent();
//		intent.setDataAndType(Uri.fromFile(file), type);
//		try {
//			startActivity(intent);
//		} catch (ActivityNotFoundException ex) {
//			ex.printStackTrace();
//
//		}
	}
}
