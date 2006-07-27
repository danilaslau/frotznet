package dgs.libs.hipfloat;

// Copyright 2003, Daniel Grobe Sachs. All Rights Reserved.
// See LICENSE for redistribution terms
//
// Some algorithms borrowed from GNU BC, but all code was rewritten.

public class hipfloatError extends Error
{
	public hipfloatBadNum actual_return;
	public hipfloatError(String reason, hipfloatBadNum err) 
	{ 
		super(reason); 
		actual_return = err;
	}
}
