/* Copyright 2002, Brian J. Swetland.  Share and Enjoy. */

package net.frotz.dictionary;

import danger.util.ByteArray;

public class JapaneseDictionary
{
	public JapaneseDictionary(byte[] _data, byte[] _index) {
		databytes = _data;
		indexbytes = _index;
		key = new char[128];
		results = new char[8192];
	}

	char data(int posn) {
		return (char) ByteArray.readShort(databytes,posn * 2);
	}

	int index(int posn) {
		return ByteArray.readInt(indexbytes,posn * 4);
	}
	
	
	int compare(char[] key, int len, int pos) {
//		char[] data = this.data;
		char a, b;
		int i = 0;
		while(i < len){
			a = key[i++];
			b = data(pos++);

			if((a >= 0x30a1) && (a <= 0x30f4)) a -= 0x60;
			if((b >= 0x30a1) && (b <= 0x30f4)) b -= 0x60;
		
			if((a >= 'A') && (a <= 'Z')) a |= 0x20;
			if((b >= 'A') && (b <= 'Z')) b |= 0x20;

			if(a != b) {
				return a - b;
			}
		}
		return 0;
	}
	
	public String Query(String search) {		
		int t,len,low,high,pos,last;
		char[] key = this.key;
//		int[] index = this.index;
		char c;
		
		len = search.length();
		search.getChars(0, len, key, 0);

		low = 0;
		high = indexbytes.length/4 - 1;
		pos = high / 2;
		last = -1;

			/* keys are sorted, do a binary search until we get a hit or
			   stop moving about... */
		while(pos != last){
				// System.err.println("< " + low + " " + pos + " " + high + " >");
			last = pos;

			t = compare(key, len, index(pos));
			if(t < 0) {
				high = pos;
				pos = low + (high - low) / 2;
				continue;
			} 
			if(t > 0){
				low = pos;
				pos = low + (high - low) / 2;
				continue;
			}
			break;
		}

			/* there may be serveral matches... 
			   slide back to the very first entry that matches */
		while((pos > 0) && (compare(key, len, index(pos-1)) == 0)) {
			pos--;
		}

			/* copy matching entries until we run out of them... */
		try {
			last = -1;
			high = 0;
			while(compare(key, len, t = index(pos++)) == 0){
				while((t > 1) && (data(t-1) != '\n')) t--;
				if(t != last) {
					last = t;
						// System.err.println("result: " + t + " ("+index(pos-1)+"@"+(pos-1)+")");
					do {
						c = data(t++);
						switch(c){
						case '/':
							results[high++] = ';';
							results[high++] = ' ';
							break;
						case '[':
							results[high++] = '(';
							break;
						case ']':
							results[high++] = ')';
							break;
						default:
							results[high++] = c;
						}
					} while(c != '\n');
				}
			}
		} catch (Throwable ex){
				/* we might overrun the results buffer... */
			high--;
		}
		return new String(results, 0, high);
	}

	private char[] key;
	private char[] results;
	
	private byte[] databytes;
	private byte[] indexbytes;
}
