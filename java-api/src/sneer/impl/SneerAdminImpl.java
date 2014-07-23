package sneer.impl;

import java.io.*;

import sneer.*;
import sneer.admin.*;
import sneer.commons.exceptions.*;

public class SneerAdminImpl implements SneerAdmin {

	/** @param secureFolder A safe folder to keep things like passwords and private keys. */
	public static SneerAdmin open(File secureFolder) throws FriendlyException {
		return null;
	}

	private SneerAdminImpl() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void initialize(PrivateKey prik) {
		// TODO Auto-generated method stub
	}

	@Override
	public PrivateKey privateKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setOwnName(String newName) {
		// TODO Auto-generated method stub
	}

	@Override
	public Sneer sneer() {
		// TODO Auto-generated method stub
		return null;
	}

}
