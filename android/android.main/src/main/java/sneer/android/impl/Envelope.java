package sneer.android.impl;

import android.os.Parcel;
import android.os.Parcelable;

/** A Parcelable envelope for any primitive value, array, String, Date and Collection. */
public class Envelope implements Parcelable {

	final public Object content;


	public static Envelope envelope(Object content) {
		return new Envelope(content);
	}


	private Envelope(Object content) {
		this.content = content;
	}


	@Override
	public int describeContents() {
		return 0;
	}


	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(content);
	}


	public static final Creator<Envelope> CREATOR = new Creator<Envelope>() {
		public Envelope createFromParcel(Parcel in) {
			return Envelope.envelope(in.readValue(null));
		}

		public Envelope[] newArray(int size) {
		return new Envelope[size];
		}
	};


	@Override
	public String toString() {
		return "Envelope(" + content + ")";
	}


	@Override
	public int hashCode() {
		return content == null ? 0 : content.hashCode();
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Envelope other = (Envelope) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		return true;
	}

}
