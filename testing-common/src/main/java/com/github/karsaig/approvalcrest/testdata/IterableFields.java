package com.github.karsaig.approvalcrest.testdata;

import java.util.Collection;

import com.github.karsaig.approvalcrest.testdata.cyclic.Two;

public class IterableFields {

	private Iterable<Throwable> ones;
	private Collection<Two> twos;
	
	public void setOnes(Iterable<Throwable> ones) {
		this.ones = ones;
	}
	
	public void setTwos(Collection<Two> twos) {
		this.twos = twos;
	}
}
