package sneer.android.sendfiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;

import sneer.android.ui.MessageActivity;
import sneer.commons.exceptions.FriendlyException;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class SendFilesActivity extends MessageActivity {

	private static final int REQUESTCODE_PICK_FILE = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pickFile();
	}

	
	private void pickFile() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		startActivityForResult(intent, REQUESTCODE_PICK_FILE);
	}
	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    if (requestCode != REQUESTCODE_PICK_FILE || resultCode != Activity.RESULT_OK) {
	    	finish();
	    	return;
	    }
	    
	    Uri fileUri = data.getData();
	    File file = new File(fileUri.getPath());
	    String fileName = file.getName();
		long lastModified = file.lastModified();
		
		byte[] bytes = null;
		try {
			bytes = readFully(new FileInputStream(file));
		} catch (FriendlyException e) {
			toast(e);
			finish();
		} catch (FileNotFoundException e) {
			toast(e);
			finish();
		}
	
		if (bytes != null) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("contents", bytes);
			map.put("filename", fileName);
			map.put("last-modified", lastModified);
			send(fileName, map);
		}
		
	    finish();
	}
	
}
