package sneer.android.main.ui;

import static sneer.android.main.ui.SneerAndroidProvider.sneer;
import static sneer.android.main.ui.SneerAndroidProvider.sneerAndroid;
import static sneer.commons.Clock.now;
import static sneer.commons.SystemReport.updateReport;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collection;

import javax.crypto.KeyAgreement;

import rx.functions.Action1;
import sneer.Conversation;
import sneer.Party;
import sneer.Profile;
import sneer.android.main.R;
import sneer.android.main.utils.Puk;
import sneer.android.ui.SneerActivity;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.Exceptions;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainActivity extends SneerActivity {
	
	private MainAdapter adapter;
	private ListView conversations;

	private Party self = sneer().self();
	private Profile ownProfile = sneer().profileFor(self);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		try {
			cryptoSpike();
		} catch (Exception e) {
			SystemReport.updateReport("Crypto", Exceptions.asNiceMessage(e));
		}
		
		if (!sneerAndroid().checkOnCreate(this)) return;
		
		startProfileActivityIfFirstTime();
		
		makeConversationList();
	}

	
	private void cryptoSpike() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
	    ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
	    keyGen.initialize(ecSpec, new SecureRandom());
	    KeyPair keyPair1 = keyGen.generateKeyPair();
	    KeyPair keyPair2 = keyGen.generateKeyPair();

	    updateReport("Crypto.puk1", Arrays.toString(keyPair1.getPublic().getEncoded()));

	    
	    
	    byte[] pukBytes = keyPair1.getPublic().getEncoded();
		PublicKey clonePuk = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(pukBytes));
	    updateReport("crypto/encoding", (keyPair1.getPublic().equals(clonePuk)));

	    ECPublicKeySpec spec = KeyFactory.getInstance("EC").getKeySpec(clonePuk, ECPublicKeySpec.class);
	    BigInteger x = spec.getW().getAffineX();
	    BigInteger y = spec.getW().getAffineY();

	    updateReport("crypto/encoding/x", x.toString(16));
	    updateReport("crypto/encoding/y", y.toString(16));

	    ECPublicKeySpec spec2 = KeyFactory.getInstance("EC").getKeySpec(keyPair2.getPublic(), ECPublicKeySpec.class);
	    updateReport("crypto/same-specs", spec.equals(spec2));
	    BigInteger x2 = spec2.getW().getAffineX();
	    BigInteger y2 = spec2.getW().getAffineY();

	    updateReport("crypto/encoding/x2", x2.toString(16));
	    updateReport("crypto/encoding/y2", y2.toString(16));
	    
	    
	    
	    
	    ECPoint w = new ECPoint(x, y);
		PublicKey clonePuk2 = KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(w, spec2.getParams()));
	    updateReport("crypto/encoding2", (keyPair1.getPublic().equals(clonePuk2)));


	    verifySignature(keyPair1);
	    
	    diffieHellman(keyPair1, keyPair2);
	}


	private void diffieHellman(KeyPair keyPair1, KeyPair keyPair2) throws Exception {
		PublicKey puk1 = keyPair1.getPublic();
		PublicKey puk2 = keyPair2.getPublic();
		
		PrivateKey prik1 = keyPair1.getPrivate();
		PrivateKey prik2 = keyPair2.getPrivate();
		
		//Device1
		KeyAgreement dh1 = KeyAgreement.getInstance("ECDH");
		dh1.init(prik1, (SecureRandom)null);
		dh1.doPhase(puk2, true);
		updateReport("Crypto/Secret1", Arrays.toString(dh1.generateSecret()));

		//Device2
		KeyAgreement dh2 = KeyAgreement.getInstance("ECDH");
		dh2.init(prik2, (SecureRandom)null);
		dh2.doPhase(puk1, true);
		updateReport("Crypto/Secret2", Arrays.toString(dh2.generateSecret()));
	}


	private void verifySignature(KeyPair keyPair) throws Exception {
		byte[] message = "abc".getBytes();
	    
	    Signature signature = Signature.getInstance("ECDSA");
	    signature.initSign(keyPair.getPrivate(), new SecureRandom());

	    signature.update(message);

	    byte[] sigBytes = signature.sign();

	    long count = 20;
	    long t0 = now();
	    for (int i = 0; i < count; i++) {
	    	signature.initVerify(keyPair.getPublic());
	    	
	    	signature.update(message);
	    	Exceptions.check(signature.verify(sigBytes));
	    	//updateReport("Crypto/Verified: " + verified);
		}
	    updateReport("Crypto/Verified/Count", count);
		updateReport("Crypto/Verified/Time", (now() - t0) / count + " millis");

	}


	private void makeConversationList() {
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setHomeButtonEnabled(true);
		
		plugActionBarTitle(actionBar, ownProfile.ownName());
		plugActionBarIcon(actionBar, ownProfile.selfie());

		conversations = (ListView) findViewById(R.id.conversationList);
		adapter = new MainAdapter(this);
		conversations.setAdapter(adapter);

		conversations.setOnItemClickListener(new OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			Conversation conversation = adapter.getItem(position);
			onClicked(conversation);
		}});
		
		deferUI(sneer().conversations()).subscribe(new Action1<Collection<Conversation>>() { @Override public void call(Collection<Conversation> conversations) {
			adapter.clear();
			adapter.addAll(conversations);
			adapter.notifyDataSetChanged();
		}});
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			navigateTo(ProfileActivity.class);
			break;
		case R.id.action_add_contact:
			shareDialog();
			break;
		case R.id.action_search_for_apps:
			Intent viewIntent =
	          new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/search?q=SneerApp"));
	          startActivity(viewIntent);
			break;
		case R.id.action_advanced:
			navigateTo(SystemReportActivity.class);
			break;
		}

		return true;
	}
	
	
	private void shareDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage("To add contacts, send them your public key and they must send you theirs.")
			.setIcon(android.R.drawable.ic_dialog_info)
			.setPositiveButton("Send Public Key", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which) {
				Puk.sendYourPublicKey(MainActivity.this, self, true, null);
			}})
			.show();
	}

	
	protected void onClicked(Conversation conversation) {
		Intent intent = new Intent();
		intent.setClass(this, ConversationActivity.class);
		intent.putExtra("partyPuk", conversation.party().publicKey().current());
		startActivity(intent);
	}

	
	@Override
	protected void onDestroy() {
		if (adapter != null)
			adapter.dispose();
		super.onDestroy();
	}

	
	@Override
	protected void onRestart() {
		super.onRestart();
		if (isOwnNameLocallyAvailable()) return;
		finish();
		toast("First and last name must be filled in");
	}
	
	
	private void startProfileActivityIfFirstTime() {
		if (!isOwnNameLocallyAvailable())
			navigateTo(ProfileActivity.class);
	}
	
	
	private boolean isOwnNameLocallyAvailable() {
		return ownProfile.isOwnNameLocallyAvailable();
	}
	
}
