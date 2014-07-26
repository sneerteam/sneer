package sneer;

import android.os.*;

public class ClientPrivateKey implements PrivateKey, Parcelable {
	
	private PublicKey publicKey;

	public ClientPrivateKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeSerializable(publicKey);
	}

	@Override
	public PublicKey publicKey() {
		return publicKey;
	}
	
	public static final Parcelable.Creator<ClientPrivateKey> CREATOR = new Parcelable.Creator<ClientPrivateKey>() {
		public ClientPrivateKey createFromParcel(Parcel in) {
			return new ClientPrivateKey((PublicKey) in.readSerializable());
		}
		
		public ClientPrivateKey[] newArray(int size) {
			return new ClientPrivateKey[size];
		}
	};
	
}