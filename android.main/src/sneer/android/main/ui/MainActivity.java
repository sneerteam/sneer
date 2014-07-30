package sneer.android.main.ui;

import rx.functions.*;
import sneer.android.main.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		Fluxo da inicializacao do main:
//		Tenho prik guardada nas private preferences do Android?
//		Se sim: sneer = SneerAdmin.initialize(prik)
//		Se nao: prik = Keys.newPrivateKey(); salvar prik nas preferences; SneerAdmin.initialize(prik);

//		sneer.self().name() está nulo?
//		Se sim: Abrir tela de profile pro usuario entrar c seu nome e sobrenome; SneerAdmin.setOwnName(nome + " " + sobrenome);
		SneerSingleton.admin().sneer().self().name().subscribe(new Action1<String>() { @Override public void call(String name) {
			if (name == null || name.isEmpty())
				startActivity(new Intent(MainActivity.this, ProfileActivity.class));
			else
				startActivity(new Intent(MainActivity.this, InteractionListActivity.class));
		}});
	}

}
