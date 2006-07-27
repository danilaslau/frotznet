package dgs.libs.hipfloat;

// Copyright 2003, Daniel Grobe Sachs. All Rights Reserved.
// See LICENSE for redistribution terms
//
// Some algorithms borrowed from GNU BC, but all code was rewritten.

public class hipfloatBadNum extends hipfloat implements Comparable 
{
	public hipfloat add(hipfloat a) { return this; }
	public hipfloat sub(hipfloat a) { return this; } 
	public hipfloat mul(hipfloat a) { return this; }
	public hipfloat div(hipfloat a) { return this; }
	public hipfloat sqrt() { return this; }
	public hipfloat exp() { return this; }
	public hipfloat ln() { return this; } 

	public boolean isError() { return true; } 

	public static final hipfloatBadNum NAN = new hipfloatBadNum(1);
	public static final hipfloatBadNum OVF = new hipfloatBadNum(2);
	public static final hipfloatBadNum NoError = new hipfloatBadNum(0);

	public hipfloatBadNum(int i)
	{
		super(0);
		this.mantissa = 2000000000 + i;
		this.exponent = 0;
	}
}

