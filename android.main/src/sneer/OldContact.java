package sneer;

import java.util.*;

public class OldContact {
	
	private String public_key;
	private String nickname;
	
	public OldContact(String public_key, String nickname) {
		this.public_key = public_key;
		this.nickname = nickname;
	}

	public String getPublicKey() {
		return public_key;
	}
	
	public void setPublicKey(String public_key) {
		this.public_key = public_key;
	}
	
	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	@Override
	public String toString() {
		return "Contact [public_key=" + public_key + ", nickname=" + nickname + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((public_key == null) ? 0 : public_key.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OldContact other = (OldContact) obj;
		if (public_key == null) {
			if (other.public_key != null)
				return false;
		} else if (!public_key.equals(other.public_key))
			return false;
		return true;
	}	
	
	public int compareTo(OldContact contact) {
		return this.getNickname().compareToIgnoreCase(((OldContact) contact).getNickname());
	}	
 
	public static Comparator<OldContact> BY_NICKNAME_IGNORING_CASE = new Comparator<OldContact>() {
 
	    public int compare(OldContact nick1, OldContact nick2) {
	      return nick1.compareTo(nick2);
	    }
 
	};
	

}

