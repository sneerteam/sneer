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

public class ComposeSendFilesActivity extends MessageActivity {

	private static final int REQUESTCODE_PICK_FILE = 0;
	private String fileName;

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
	    
	    File file = fileFrom(data);
	    fileName = file.lastModified() + file.getName();
		
		byte[] bytes = bytesFrom(file);
		send(bytes);
	    finish();
	}


	private File fileFrom(Intent data) {
		Uri fileUri = data.getData();
	    return new File(fileUri.getPath());	    
	}


	private byte[] bytesFrom(File file) {
		byte[] ret = null;
		try {
			ret = readFully(new FileInputStream(file));
		} catch (FriendlyException e) {
			toast("Error reading file");
			finish();
		} catch (FileNotFoundException e) {
			toast("File not found. (" + e.getMessage() + ").");
			finish();
		}
		return ret;
	}


	private void send(byte[] bytes) {
		if (bytes != null) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("contents", bytes);
			map.put("filename", fileName);
			send(fileName, map, null);
		}
	}
	
}
