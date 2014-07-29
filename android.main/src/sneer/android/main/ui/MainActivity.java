package sneer.android.main.ui;

import sneer.android.main.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent activity;
		
//		Fluxo da inicializacao do main:
//		Tenho prik guardada nas private preferences do Android?
//		Se sim: sneer = SneerAdmin.initialize(prik)
//		Se nao: prik = Keys.newPrivateKey(); salvar prik nas preferences; SneerAdmin.initialize(prik);
//		sneer.self().name() est� nulo?
//		Se sim: Abrir tela de profile pro usuario entrar c seu nome e sobrenome; SneerAdmin.setOwnName(nome + " " + sobrenome);
		
		if(SneerSingleton.admin().sneer().self().name().toString()==null &&
				SneerSingleton.admin().sneer().self().name().toString().isEmpty())
			activity = new Intent(this, ProfileActivity.class);
		else
//		For now we'll just call the InteractionListActivity
			activity = new Intent(this, InteractionListActivity.class);
		startActivity(activity);
	}

}
