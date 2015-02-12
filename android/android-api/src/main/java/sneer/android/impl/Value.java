package sneer.android.impl;

import android.os.Parcel;
import android.os.Parcelable;

public class Value implements Parcelable {

	final Object value;

	
	public static Value of(Object o) {
		return new Value(o);
	}

	
	Value(Object value) {
		this.value = value;
	}
	
	
	@Override
	public int describeContents() {
		return 0;
	}

	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(value);
	}

	
	public static final Creator<Value> CREATOR = new Creator<Value>() {
		public Value createFromParcel(Parcel in) {
			return Value.of(in.readValue(null));
		}
		
		public Value[] newArray(int size) {
			return new Value[size];
		}
	};

	
	public Object get() {
		return value;
	}
	
	
	@Override
	public String toString() {
		return "Value(" + value + ")";
	}

	
	@Override
	public int hashCode() {
		return value == null ? 0 : value.hashCode();
	}
	

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Value other = (Value) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
}
