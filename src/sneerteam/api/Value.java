package sneerteam.api;

import java.util.HashMap;
import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;

public class Value implements Parcelable {

	public static Value of(Object o) {
		return new Value(o);
	}

	final Object value;
	
	Value(Object value) {
		this.value = value;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		write(dest, value);
	}

	public static final Parcelable.Creator<Value> CREATOR = new Parcelable.Creator<Value>() {
		public Value createFromParcel(Parcel in) {
			return Value.of(read(in));
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
		return Type.of(value).name() + "(" + value + ")";
	}
	
	@Override
	public boolean equals(Object o) {
		if (super.equals(o))
			return true;
		if (!(o instanceof Value))
			return false;
		return equals(value, ((Value)o).value);
	}

	static boolean equals(Object x, Object y) {
		return x == y || (x != null && y != null && x.equals(y));
	}
	
	public static enum Type {
		NULL {
			@Override
			public Object createFromParcel(Parcel in) {
				return null;
			}

			@Override
			public void writeToParcel(Parcel dest, Object value) {
			}
		},
		STRING {
			@Override
			public Object createFromParcel(Parcel in) {
				return in.readString();
			}

			@Override
			public void writeToParcel(Parcel dest, Object value) {
				dest.writeString((String)value);
			}
		},
		LONG {
			@Override
			public Object createFromParcel(Parcel in) {
				return in.readLong();
			}

			@Override
			public void writeToParcel(Parcel dest, Object value) {
				dest.writeLong((Long)value);
			}
		},
		MAP {
			@Override
			public Object createFromParcel(Parcel in) {
				int size = in.readInt();
				HashMap<Object, Object> map = new HashMap<Object, Object>(size);
				for (int i = 0; i < size; i++) {
					map.put(read(in), read(in));
				}
				return map;
			}

			@Override
			public void writeToParcel(Parcel dest, Object value) {
				Map<?, ?> map = (Map<?, ?>)value;
				dest.writeInt(map.size());
				for (Map.Entry<?, ?> entry : map.entrySet()) {
					write(dest, entry.getKey());
					write(dest, entry.getValue());
				}
			}
		};

		public static Type of(Object o) {
			if (o instanceof String)
				return STRING;
			if (o instanceof Long)
				return LONG;
			if (o instanceof Map)
				return MAP;
			return NULL;
		}

		public abstract Object createFromParcel(Parcel in);

		public abstract void writeToParcel(Parcel dest, Object value);
	}
	
	static Object read(Parcel in) {
		Type type = Type.values()[in.readInt()];
		return type.createFromParcel(in);
		
	}
	
	static void write(Parcel dest, Object value) {
		Type t = Type.of(value);
		dest.writeInt(t.ordinal());
		t.writeToParcel(dest, value);
	}
}
