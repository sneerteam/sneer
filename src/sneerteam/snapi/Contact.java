package sneerteam.snapi;

import rx.*;

public class Contact {

    private String publicKey;
    private String nickname;

    public Contact(String publicKey, String nickname) {
        this.publicKey = publicKey;
        this.nickname = nickname;
    }

    public String publicKey() {
        return publicKey;
    }

    public String nickname() {
        return nickname;
    }
    
    public Observable<Contact> contacts(Cloud cloud) {
        return cloud.contacts(publicKey);
    }
    
}
