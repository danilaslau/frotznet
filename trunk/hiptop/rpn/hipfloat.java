package dgs.libs.hipfloat;

// Copyright 2003, Daniel Grobe Sachs. All Rights Reserved.
// See LICENSE for redistribution terms
//
// Some algorithms borrowed from GNU BC, but all code was rewritten.
// 
// arctrig functions contributed by Greg Vander Rhodes <greg@vanderrhodes.com>

public class hipfloat implements Comparable {
  	protected int mantissa;
  	protected int exponent;

	static public final hipfloat ZERO = new hipfloat(0);
	static public final hipfloat ONE = new hipfloat(1);
	static public final hipfloat HALF = new hipfloat(5,-1);
	static public final hipfloat TWO = new hipfloat(2);
	static public final hipfloat PI = new hipfloat(314159265,-8);
	static public final hipfloat PI2 = new hipfloat(628318530,-8);
	static public final hipfloat HALFPI = PI.div(TWO);
	static public final hipfloat E = new hipfloat(271828182,-8);
	static public final hipfloat MAXEXP = new hipfloat(23026);

	static public final hipfloatBadNum OVF = hipfloatBadNum.OVF;
	static public final hipfloatBadNum NAN = hipfloatBadNum.NAN;
	static public final hipfloatBadNum NoError = hipfloatBadNum.NoError;

	static private final int fact_db[] = 
		{ 1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916000 };

	static public final int MAX_EXP = 9999;

	public hipfloat(hipfloat in)
	{
		hipfloat out;

		mantissa = in.mantissa;
		exponent = in.exponent;

		out = normalize(this);
		
		if( out != this )  // must be an error
			throw new hipfloatError("Overflow",OVF);
	}

	public hipfloat(int in)
	{
		mantissa = in;
		exponent = 0;

		normalize(this);
	}

	public hipfloat(int man, int exp)
	{
		hipfloat out;

		mantissa = man;
		exponent = exp;

		out = normalize(this);
		
		if( out != this )  // must be an error
			throw new hipfloatError("Overflow",OVF);
	}

	public hipfloat(long man, int exp)
	{
		while ( (exp < -(MAX_EXP+8)) && (man != 0))
		{
			exp += 1;
			man /= 10;
		}
		
		if( man == 0 )
		{
			mantissa = 0;
			exponent = -8;
			return;
		}

		while( (Math.abs(man) >= 1000000000) && (exp < (MAX_EXP-8)) )
		{
			if( (Math.abs(man) < 10000000000L) && ((Math.abs(man)%10) >= 5) )
				man += 10;  // need to round up

			man /= 10;
			exp += 1;
		}

		while( (Math.abs(man) < 100000000) && (exp > -(MAX_EXP+8)) )
		{
			man *= 10;
			exp -= 1;
		}

		if( (exp > (MAX_EXP-8)) || (man >= 1000000000) )
			throw new hipfloatError("Overflow",OVF);

		mantissa = (int)man;
		exponent = exp;
	}

	public hipfloat(String in)
	{
		hipfloat work = fromString(in);

		if( work.isError() )
		{
			if( work == OVF ) 
				throw new hipfloatError("Overflow on conversion",
						(hipfloatBadNum)work);
			else if( work == NAN )
				throw new hipfloatError("Invalid number conversion",
						(hipfloatBadNum)work);
			else
				throw new hipfloatError("Unknown error converting string",
						(hipfloatBadNum)work);
		}

		this.mantissa = work.mantissa;
		this.exponent = work.exponent;
	}
						
	public int mantissa()
	{
		return mantissa;
	}

	public int exponent()
	{
		return exponent;
	}

	public hipfloat floor()
	{
		if( exponent < -10 ) return ZERO;
		if( exponent > 0 ) return this;

		hipfloat out = new hipfloat(this);

		while( out.exponent < 0 ) 
		{
			out.exponent++;
			out.mantissa /= 10;
		}

		return normalize(out);
	}

	public hipfloat ceil()
	{
		if( this.compareTo(this.floor()) == 0 )
			return this;

		return( (this.add(ONE)).floor() );
	}

	public hipfloat round()
	{
		return (this.add(HALF)).floor();
	}

	public int toint()
	{
		if( (this.exponent > 0) || 
				((this.exponent == 1) && (this.mantissa > 214748364)) )
			throw new hipfloatError("Overflow",OVF);

		if( this.exponent == 1 )
			return this.mantissa * 10;

		int exp = this.exponent, man = this.mantissa;

		while( exp < 0 )
		{
			exp ++;
			man /= 10;
		}

		return man;
	}


	public hipfloat add(hipfloat a)
	{
		if( a.isError() ) return a;

		if( mantissa == 0 )
			return a;

		if( a.mantissa == 0 )
			return this;
			
		if( a.exponent > exponent )
			return a.add(this);
		else
		{
			int aexp, aman;

			aexp = a.exponent;
			aman = a.mantissa;

			while( aexp < exponent-1 )
			{
				aexp++;
				aman /= 10;
			}

			if( aexp == exponent-1 )
			{
				if( Math.abs(aman)%10 >= 5 )
					aman += 10;

				aman /= 10;
				aexp++;
			}

			try
				{ return new hipfloat(mantissa+aman, exponent); }
			catch( hipfloatError error )
				{ return error.actual_return; }
		}

	}

	public hipfloat mul(hipfloat a)
	{
		if( a.isError() ) return a;

		long man;
		int exp;

		exp = exponent + a.exponent;
		man = (long)mantissa * a.mantissa;

		try
			{ return new hipfloat(man, exp); }
		catch( hipfloatError error )
			{ return error.actual_return; }
	}

	public hipfloat sub(hipfloat a)
	{
		if( a.isError() ) return a;

		try
			{ return this.add(a.neg()); }
		catch( hipfloatError error )
			{ return error.actual_return; }
	}
	
	public hipfloat div(hipfloat a)
	{
		if( a.isError() ) return a;

		if( a.compareTo(ZERO) == 0 )
			return NAN;

		long divs = 1000000000000000000L/a.mantissa;;
		int exp = -a.exponent - 18;

		hipfloat b;
		
		try
			{ b = new hipfloat(divs, exp); }
		catch( hipfloatError error )
			{ return error.actual_return; }

		return this.mul(b);
	}

	public hipfloat pow(hipfloat a)
	{
		if( a.isError() ) return a;

		hipfloat x;
		int i, t;

		x = this;

		if( a.compareTo(a.floor()) == 0 ) // if integer
			try
			{
				t = (a.abs()).toint();

				hipfloat out=ONE,z=x;
				
				while( t > 0 )
				{
					if( 1 == (t % 2) ) 
						out = out.mul(z);

					z = z.mul(z);
					t = t / 2;
				}

				return out;
			}
			catch( hipfloatError error )
			{
				// fall back to using logs
				;
			}

		if( x.compareTo(ZERO) < 0 ) 
			return NAN; // if a isn't integer, this isn't valid

		return ((x.ln()).mul(a)).exp();
	}

	public hipfloat sqrt()
	{
		hipfloat guess;
		hipfloat check;

		if( this.compareTo(ZERO) < 0 ) 
			return NAN;

		if( this.compareTo(ZERO) == 0 )
			return ZERO;

		try {
			int i;

			//if( this.compareTo(new hipfloat(1)) > 0 )
				guess = new hipfloat(1,(exponent+8)/2);
			//else
			//	guess = new hipfloat(1,0);

			for( i = 0; i < 8; i++ )
			{
				check = this.div(guess);
				guess = (check.add(guess)).mul(HALF);
			} 
		}
		catch( hipfloatError error )
			{ return error.actual_return; }

		return guess;
	}

	// Use the Taylor series:
	//             	        3       5
	// 	    x-1   1 x-1   1 x-1
	// ln(x) =2 --- + - --- + - ---  + ...
	// 	    x+1   3 x+1   5 x+1 

	public hipfloat ln()
	{
		if( this.compareTo(ZERO) < 0 )
			return NAN;

		hipfloat in = new hipfloat(this);
		int scale = 2; 		
		int i;

		// bring input to the range 0.5 - 2, exclusive

		while( (in.compareTo(TWO) >= 0) || (in.compareTo(HALF) <= 0) )
		{
			scale *= 2;
			in = in.sqrt();
		}

		hipfloat t;
		hipfloat t2;

		hipfloat out = ZERO;

		t = (in.sub(ONE)).div(in.add(ONE));
		t2 = t.mul(t);
		

		for( i = 0; i < 20; i++ )
		{
			out = out.add(t.div(new hipfloat(1+2*i)));
			t = t.mul(t2);

			if( t.nearTo(ZERO,10) )
				break;
		}

		return out.mul(new hipfloat(scale));
	}

	public hipfloat exp()
	{
		hipfloat x = new hipfloat(this);
		hipfloat xx = new hipfloat(0);
		hipfloat out = new hipfloat(0);
		int count = 0, i;

		// don't waste time doing computation if it'll overflow

		if( x.compareTo(MAXEXP) > 0 )
			return OVF;

		// Scale to range where we can use the Taylor series

		while( x.compareTo(ONE) > 0 )
		{
			x = x.div(TWO);
			count ++;
		}

		// Taylor series of 1+x+x^2/2!+x^3/3! ... 
		
		out = (new hipfloat(1)).add(x);
		xx = x.mul(x);

		for( i = 2; i < 12; i++ )
		{
			out = out.add(xx.div(this.factorial(i)));
			xx = xx.mul(x);
		}

		while( count-- > 0 )
			out = out.mul(out);

		return out;
	}

	public hipfloat sin()
	{
		// series is x - x^3/3! + x^5/5! - ... 

		int i, sign = -1; 
		hipfloat x = this;

		if( x.exponent > 0 )
			return OVF;

		while( x.abs().compareTo(PI) > 0 )
		{
			int exp = x.exponent + 7;
			if( exp < 0 ) exp = 0;

			if( x.compareTo(ZERO) > 0 )
				x = x.sub(PI2.mul(new hipfloat(1,exp)));
			else
				x = x.add(PI2.mul(new hipfloat(1,exp)));
		}
			
		if( x.abs().compareTo(HALFPI) > 0 )
			if( x.compareTo(ZERO) > 0 ) 
				x = PI.sub(x);
			else
				x = PI.neg().add(x);

		hipfloat out = x;
		hipfloat f = x.mul(x);
		hipfloat d = ONE;

		for( i = 3; i < 13; i += 2)
		{
			x = x.mul(f);
			d = d.mul(new hipfloat(i-1)).mul(new hipfloat(i));

			if( sign == 1 )
				out = out.add(x.div(d));
			else
				out = out.sub(x.div(d));

			sign = -sign; 
		}

		return out;
	}

	public hipfloat cos()
	{
		return HALFPI.sub(this).sin();
	}

	public hipfloat tan()
	{
		return this.sin().div(this.cos());
	}

	public hipfloat arctan ()
	{
		int i, sign = -1;
		hipfloat x = this.abs ();
		hipfloat xsign = new hipfloat (1);
		if (this.compareTo (ZERO) < 0)
		{
			xsign = xsign.neg ();
		}
		boolean over1flag = false;
		boolean oneflag = false;
		if (x.compareTo (ONE) > 0)
		{
			x = ONE.div (x);
			over1flag = true;
		}

		if (x.compareTo (ONE) == 0)
		{
			oneflag = true;
		}
		// use series
		// arctan(y) = y - y^3/3 + y^5/5 - y^7/7 + y^9/9 - ...

		hipfloat out = x;
		if (!oneflag)
		{

			hipfloat f = x.mul (x);
			hipfloat d = ONE;

			for (i = 3; i < 23; i += 2)
			{
				x = x.mul (f);
				d = new hipfloat (i);

				if (sign == 1)
					out = out.add (x.div (d));
				else
					out = out.sub (x.div (d));

				sign = -sign;
			}

			if (over1flag)
			{
				out = PI.div (new hipfloat (2)).sub (out);
			}
		}
		else
		{
			out = new hipfloat (785398163, -9);
		}

		return out.mul (xsign);
	}

	public hipfloat arccos ()
	{
		if (this.abs ().compareTo (ONE) > 0)
			return NAN;

		hipfloat x = this.abs ();
		boolean negflag = false;
		boolean zeroflag = false;
		boolean oneflag = false;
		if (this.compareTo (ZERO) < 0)
			negflag = true;

		if (this.compareTo (ZERO) == 0)
			zeroflag = true;

		if (x.compareTo (ONE) == 0)
			oneflag = true;

		hipfloat out = new hipfloat (1);

		if (zeroflag)
			out = PI.div (TWO);
		else
		{
			if (oneflag)
				out = ZERO;
			else
			{
				out = out.sub (x.mul (x)).sqrt ().div (x);
				out = out.arctan ();
			}
		}

		if (negflag)
			out = PI.sub (out);

		return out;
	}

	public hipfloat arcsin ()
	{
		if (this.abs ().compareTo (ONE) > 0)
			return NAN;

		hipfloat x = this.abs ();
		boolean negflag = false;
		boolean zeroflag = false;
		boolean oneflag = false;
		if (this.compareTo (ZERO) < 0)
			negflag = true;

		if (this.compareTo (ZERO) == 0)
			zeroflag = true;

		if (x.compareTo (ONE) == 0)
			oneflag = true;

		hipfloat out = new hipfloat (1);

		if (zeroflag)
			out = ZERO;
		else
		{
			if (oneflag)
				out = PI.div (TWO);
			else
			{
				out = x.div (out.sub (x.mul (x)).sqrt ());
				out = out.arctan ();
			}
		}

		if (negflag)
			out = out.neg ();

		return out;
	}
	
	public hipfloat neg()
	{
		return new hipfloat(-mantissa,exponent);
	}

	static public hipfloat factorial(int i)
	{
		if( i < 12 )
			return new hipfloat(fact_db[i]);
		else
			return (new hipfloat(i)).factorial();
	}

	public hipfloat factorial()
	{
		if( this.compareTo(ZERO) <= 0 )
			return new hipfloat( 1 );

		return this.mul((this.sub(ONE)).factorial());
	}

	public hipfloat abs()
	{
		return new hipfloat(Math.abs(mantissa),exponent);
	}

	public boolean nearTo(hipfloat a, int ulp)
	{
		if( a.isError() )
			return false;

		int x;

		hipfloat c1 = new hipfloat(this);
		hipfloat c2 = new hipfloat(a);

		while( c1.exponent < c2.exponent ) 
		{
			c2.exponent --;
			c2.mantissa /= 10;
		}

		while( c2.exponent < c1.exponent )
		{
			c1.exponent --;
			c1.mantissa /= 10;
		}

		x = c1.mantissa - c2.mantissa;

		if( Math.abs(x) <= ulp )
			return true;
		else
			return false;
	}

	public boolean isError()
	{
		return false;
	}

	public int compareTo(Object o)
	{
		if( ((hipfloat)(o)).isError() )
			return 1;

		hipfloat a = (hipfloat)o;

		if( a.mantissa == 0 )
			return this.mantissa;

		return (this.sub(a)).mantissa;
	}

	static int returnError(char[] out, String error)
	{
		int i;

		for( i = 0; i < error.length(); i++ )
			out[i] = error.charAt(i);

		return error.length();
	}

	public int toCharArray(char[] out, boolean Scientific)
	{
		int unit;
		int frac;
		int vexp = exponent + 8;
		int i, j, sigfig;

		char c;
		int len = 0;

		if( this == NAN )
			return returnError(out, "Not a number");

		if( this == OVF )
			return returnError(out, "Overflow");

  		if( mantissa < 0 )
			out[len++] = '-';
  
  		frac = Math.abs(mantissa);

		if( !Scientific )
		{	// Can we print this number?
			i = 100000000;
			sigfig = 1;

			while( (i > 0) && (0!=(mantissa % i)) )
			{
				sigfig ++;
				i /= 10;
			}

			if( (vexp == -1) && (sigfig == 9) ) 
			{
				out[len++] = '0';
				out[len++] = '.';
				vexp = 8;
			}

			if( (vexp <= 8) && (vexp >= -9+sigfig))
			{ 	// best not scientific
				i = 100000000;

				while( vexp < 0 )
				{
					frac = frac/10;
					vexp++;
					sigfig++;
				}

				for( j = 0; j < sigfig; j++ )
				{
					if( vexp == -1 )
						out[len++] = '.';

					unit = frac/i;
					frac = frac%i;
					out[len++] = (char) ('0' + (char)unit);

					vexp--;
					i /= 10;
				}

				while( vexp >= 0 )
				{
					out[len++] = '0';
					vexp--;
				}

				return len;
			}
		}
  
		for( i = 100000000; i > 0 ; i /= 10 )
		{
			unit = frac/i;
			frac = frac%i;

			out[len++] = (char) ('0'+(char)unit);
			if( i == 100000000 )
				out[len++] = '.';
		}

		out[len++] = 'e';
		
		if( vexp < 0 )
		{
			out[len++] = '-';
			vexp = -vexp;
		}
		else
			out[len++] = '+';


		boolean printed = false;

		for( i = 10000; i > 0; i /= 10 )
		{
			if( (vexp >= i) || (i == 1) || printed )
			{
				printed = true;
				out[len++] = (char) ('0' + (char) (vexp/i));
			}

			vexp %= i;

		}


		return len;
	}

	public String toString(boolean Scientific)
	{
		char out[] = new char[64];
		int len;

		len = this.toCharArray(out, Scientific);

		return new String(out,0,len);
	}

	public String toString()
	{
		return toString(false);
	}

	public static hipfloat fromString(String in)
	{
		int i, j, state = 0, exp = 0, dot = 0;
		boolean neg = false, expneg = false; 

		hipfloat work = new hipfloat(0);
		
		hipfloatBadNum error = NoError;

		char c;

		in = in.trim();
		i = 0;

		while( (error == NoError) && (i < in.length()) )
		{
			c = in.charAt(i++);

			switch( state )
			{
				case 0: state = 1;

					if( c == '-' )
					{
						neg = true;
						break;
					}

					if( c == '+' )
						break;

				case 1: if( (c >= '0') && (c <= '9') )
					{
						if( dot == 0 )
							work = (work.mul(new hipfloat(10))).
									add(new hipfloat(c-'0'));
						else
						{
							work = work.add(new hipfloat((c-'0'),dot));
							dot --;
						}
					}
					else if( (c == '.') && (dot == 0) )
						dot = -1;
					else if( (c == 'e') || (c == 'E') )
						state = 2;
					else
						error = NAN;

					break;

				case 2: state = 3;

					if( c == '-' )
					{
						expneg = true;
						break;
					}
					
					if( c == '+' )
						break;

				case 3:	if( (c >= '0') && (c <= '9') )
					{
						exp = exp * 10 + (c - '0');

						if( exp > 10000 )
							error = OVF;
					}
					else
						error = NAN;
			}
		}

		if( error != NoError )
			return error;

		if( neg )
			work = work.neg();

		if( expneg ) 
			exp = -exp;

		if( exp != 0 )
			work = work.mul(new hipfloat(1,exp));

		//System.err.println(work);

		return work;
	}

	private static hipfloat normalize(hipfloat in)
	{
		while ( (in.exponent < -(MAX_EXP+8)) && (in.mantissa != 0))
		{
			in.exponent += 1;
			in.mantissa /= 10;
		}
		
		if( in.mantissa == 0 )
		{
			in.exponent = -8;
			return in;
		}

		while( (Math.abs(in.mantissa) >= 1000000000) && (in.exponent < (MAX_EXP-8)) )
		{
			if( (Math.abs(in.mantissa % 10)) >= 5 )
				in.mantissa += 10;  // need to round up

			in.mantissa /= 10;
			in.exponent += 1;
		}

		while( (Math.abs(in.mantissa) < 100000000) && (in.exponent > -(MAX_EXP+8)) )
		{
			in.mantissa *= 10;
			in.exponent -= 1;
		}

                if( (in.exponent > (MAX_EXP-8)) || (in.mantissa >= 1000000000) )
			return OVF;
		else
			return in;
	}
}

