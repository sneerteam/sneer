package sneer;

import static sneer.ObservableTestUtils.*;
import static sneer.tuples.TupleUtils.*;

import java.io.*;

import org.junit.*;

import sneer.impl.*;
import sneer.tuples.*;

public class SimpleP2P extends TestsBase {
	
	
//	public static void main(String[] args) {
//		
//		Sneer sneer = SneerFactory.newSneer();
//		
//		KeyPair fabio = sneer.newKeyPair();
//		PublicKey felipePuk = null;
//		PublicKey diegoPuk = null;
//		
//		Cloud cloud = sneer.newCloud(fabio);
//		
//		
//		
//		cloud.newTuplePublisher()
//			.intent("profile", "name")
//			.value("Fabio Roger Manera")
//			.pub();
//		
//		
//		//   /:me/contacts/felipePuk/nickname
//		
//		cloud.newTuplePublisher()
//			.audience(sneer.self())
//			.intent("contact", felipePuk, "nickname")
//			.pub("Felipe");
//		
//		
//		Map<String, Object> map = new HashMap<String, Object>();
//		map.put("audience", sneer.self());
//		map.put("intent", "contact");
//		map.put("field", "nickname");
//		map.put("puk", felipePuk);
//		map.put("value", "Felipe");
//		
//		
//		
//		
//		
//		
//		TuplePublisher moves = cloud.newTuplePublisher()
//			.audience(felipePuk)
//			.intent("rock-paper-scissor", "move");
//		
//		moves.pub("rock");
//		moves.pub("paper");
//	
//		TuplePublisher messages = cloud.newTuplePublisher()
//			.intent("rock-paper-scissor", "message");
//		
//		messages.audience(felipePuk).pub("opa!!");
//		messages.audience(diegoPuk).pub("opa, beleza??");
//		
//		
//		
//	
//		
//		
//		Cloud felipeCloud = null;
//		
//		felipeCloud.newTupleSubscriber()
//			.intent("rock-paper-scissor")
//			.tuples()
//			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
//				System.out.println("----> " + t1);
//			}});
//	
//		felipeCloud.newTupleSubscriber()
//			.intent("rock-paper-scissor", "message")
//			.tuples()
//			.map(new Func1<Tuple, String>() {  @Override public String call(Tuple t1) {
//				return (String) t1.value(); 
//			} })
//			.subscribe(new Action1<String>() {  @Override public void call(String t1) {
//				System.out.println("----> " + t1);
//			}});
//	
//	
//	
//	
//	Tuple nickname = sneer.cloud().newTupleSubscriber()
//			.audience(sneer.self())
//			.author(sneer.self())
//			.intent("nickname")
//			.where("puk", tuple.author())
//			.localOrNull()
//			.values()
//			.toBlockingObservable()
//			.last();
////			.values()
////			.cast(String.class)
////			.subscribe(new Action1<String>() {  @Override public void call(String nickname) {
////				System.out.println("-----> " + nickname);
////			}});
//
//
//		sneer.cloud().newTupleSubscriber()
//			.audience(sneer.self())
//			.author(sneer.self())
//			.intent("picture")
//			.where("puk", tuple.author())
//			.values()
//			.cast(String.class)
//			.subscribe(new Action1<String>() {  @Override public void call(String nickname) {
//				System.out.println("-----> " + nickname);
//			}});
//
//		sneer.ownFile("notes", tuple.author())
//			.subscribe(new Action1<DataInput>() {  @Override public void call(DataInput in) {
//				// use in
//			}});
//		
//		sneer.cloud().newTupleSubscriber()
//			.audience(sneer.self())
//			.author(sneer.self())
//			.intent("file")
//			.where("path", new Object[]{"notes", tuple.author()})
//			.values()
//			.cast(byte[].class)
//			.subscribe(new Action1<byte[]>() {  @Override public void call(byte[] notes) {
//				DataInput in = new DataInputStream(new ByteArrayInputStream(notes));
//				// use in
//			}});
//
//
//		sneer.cloud().newTupleSubscriber()
//			.author(sneer.self())
//			.audience(sneer.self())
//			.nested("contact")
//				.nested(tuple.author())
//					.nested("nickname")
//						.currentValue();

	
//	}
	
	
	
	@Test
	public void messagePassing() throws IOException {

		TuplePublisher publisher = tuplesA.newTuplePublisher()
			.audience(userB.publicKey())
			.intent("rock-paper-scissor/move")
			.pub("paper");
			
		publisher.pub("rock");
		
		publisher.intent("rock-paper-scissor/message")
			.pub("hehehe");
		
		
		TupleSubscriber subscriber = tuplesB.newTupleSubscriber();

		expectValues(subscriber.tuples(), "paper", "rock", "hehehe");
		expectValues(subscriber.intent("rock-paper-scissor/move").tuples(), "paper", "rock");
		expectValues(subscriber.intent("rock-paper-scissor/message").tuples(), "hehehe");
		
	}

	@Test
	public void tupleWithIntent() throws IOException {

		tuplesA.newTuplePublisher()
			.audience(userB.publicKey())
			.intent("rock-paper-scissor/move")
			.pub("paper")
			.intent("rock-paper-scissor/message")
			.pub("hehehe");
		
		assertEqualsUntilNow(tuplesB.newTupleSubscriber().tuples().map(TO_INTENT), "rock-paper-scissor/move", "rock-paper-scissor/message");
		
	}
	
	@Test
	public void targetUser() {
		
		tuplesA.newTuplePublisher()
			.audience(userC.publicKey())
			.intent("rock-paper-scissor/move")
			.pub("paper");
		
		assertCount(0, tuplesB.newTupleSubscriber().tuples());
		assertCount(1, tuplesC.newTupleSubscriber().tuples());
	}
	
	@Test
	public void publicTuples() {
		
		tuplesA.newTuplePublisher()
			.intent("profile/name")
			.pub("UserA McCloud");
		
		assertCount(1, tuplesA.newTupleSubscriber().tuples()); // should I receive my own public tuples?
		assertCount(1, tuplesB.newTupleSubscriber().tuples());
		assertCount(1, tuplesC.newTupleSubscriber().tuples());
		
	}
	
	@Test
	public void byAuthor() {
		tuplesA.newTuplePublisher()
			.intent("profile/name")
			.pub("UserA McCloud");
		
		assertCount(1, tuplesB.newTupleSubscriber().author(userA.publicKey()).tuples());
		assertCount(0, tuplesB.newTupleSubscriber().author(userC.publicKey()).tuples());
	}
	
	@Test
	public void differenceAudience() {
		
		PrivateKey group = Keys.newPrivateKey();
		
		tuplesA.newTuplePublisher()
			.audience(group.publicKey())
			.intent("chat/message")
			.pub("hey people!");
		
		assertCount(0, tuplesB.newTupleSubscriber().author(userC.publicKey()).tuples());
		expectValues(tuplesB.newTupleSubscriber().audience(group).tuples(), "hey people!");
	}
	
	@Test
	public void audienceIgnoresPublic() {
		
		tuplesA.newTuplePublisher()
			.intent("chat/message")
			.pub("hey people!");
		
		PrivateKey group = Keys.newPrivateKey();
		assertCount(0, tuplesB.newTupleSubscriber().audience(group).tuples());
	}
	
}
