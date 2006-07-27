// Copyright 2002-2003, Brian Swetland
// See provided LICENSE file.

package net.frotz.jp_im;

import danger.util.ByteArray;

class Dictionary 
{
	static final int NOUN       = 0x40;
	static final int VERB5U     = 0x01;
	static final int VERB5K     = 0x02;
	static final int VERB5G     = 0x03;
	static final int VERB5S     = 0x04;
	static final int VERB5Z     = 0x05;
	static final int VERB5T     = 0x06;
	static final int VERB5N     = 0x07;
	static final int VERB5B     = 0x08;
	static final int VERB5M     = 0x09;
	static final int VERB5R     = 0x0a;
	static final int VERB1      = 0x10;
	static final int VERBSURU   = 0x11;
	static final int IADJ       = 0x12;
	
	byte[] data;
	int[] index;
	int entries;
	
	byte rawtext[] = new byte[256];
	char temp[] = new char[128];	

	Dictionary(byte[] dict) {
		data = dict;
		entries = ByteArray.readInt(dict, 4);
		System.err.println("DICTIONARY: "+entries+" ("+dict.length+")");

		index = new int[entries];
		int ptr = 8;
		int n = 0;
		while(n < entries){
			index[n++] = ptr;
			ptr += dict[ptr] + 1;
			while(dict[ptr] != 0){
				ptr += dict[ptr + 1] + 2; /* skip POS, LEN, wbytes */
			}
			ptr++;
		}
		System.err.println("DICTIONARY INDEXED");
	}

	static String getPOS(int tag) {
		if((tag & NOUN) == 0){
			switch(tag){
			case VERB5U: return "v5u";
			case VERB5K: return "v5k";
			case VERB5G: return "v5g";
			case VERB5S: return "v5s";
			case VERB5Z: return "v5z";
			case VERB5T: return "v5t";
			case VERB5N: return "v5n";
			case VERB5B: return "v5b";
			case VERB5M: return "v5m";
			case VERB5R: return "v5r";
			case VERB1: return "v1";
			case VERBSURU: return "vs";
			case IADJ: return "iadj";
			default: return "";
			}
		} else {
			tag = tag & 0x3f;
			if(tag == 0) return "n";
			return getPOS(tag & 0x3f) + "+n";
		}
	}

	int addWords(int ptr, String[] option, String[] tag, int optcount, int limit, int last) {
		int len, pos, n, count = 1;
		
		while(data[ptr] != 0){
			pos = data[ptr++]; /* ignore POS for now */
			len = data[ptr++];
			n = 0;
			while(len > 0){
				temp[n++] = (char) ((data[ptr] & 0xff) | ((data[ptr+1] & 0xff) << 8));
				len -= 2;
				ptr += 2;
			}
			if(limit == 0){
				tag[optcount] = getPOS(pos);
				option[optcount++] = new String(temp, 0, n);
			} else {
				if(limit == NOUN){
					if((pos & NOUN) != 0){
						tag[optcount] = getPOS(pos);
						option[optcount++] = new String(temp, 0, n);
					}
				} else {
					if((pos & 0x3f) == limit){
						if(last != 0) temp[n++] = (char) last;
						tag[optcount] = getPOS(pos);
						option[optcount++] = new String(temp, 0, n);
					}
				}
			}
		}
		return optcount;
	}

	int compareChars(char[] achr, int alen, char[] bchr, int blen) {
		int n;

		for(n = 0; n < alen; n++){
			if(n == blen) return achr[n];
			if(achr[n] != bchr[n]) return achr[n] - bchr[n];
		}
		if(blen == n) return 0;
		return -bchr[n];
	}
	
	int findReading(char[] key, int len) {
		int low, high, pos, ptr, rlen, n, res;
		byte[] dict = data;
		
		low = -1;
		high = entries;
		while((high - low) > 1){
			pos = (high + low) / 2;
			ptr = index[pos];
			n = dict[ptr++];
			res = ptr;
			rlen = 0;
			while(n > 0){
				temp[rlen++] = (char) ((dict[ptr] & 0xff) | ((dict[ptr+1] & 0xff) << 8));
				n -= 2;
				ptr += 2;
			}
				/* compare key:len with temp:rlen */
			n = compareChars(key, len, temp, rlen);

			if(n == 0) return res + dict[res-1];

			if(n > 0){
				low = pos;
			} else {
				high = pos;
			}
		}
		return -1;
	}	

	int lookup(char[] text, int textlen, String[] option, String[] tag) {
		int optcount, also, ptr;
		if(textlen == 0) return 0;

		option[0] = new String(text, 0, textlen);
		tag[0] = "kana";
		optcount = 1;

		ptr = findReading(text, textlen);
		if(ptr > 0){
			optcount = addWords(ptr, option, tag, optcount, NOUN, 0);
		}
			
		if(textlen > 1){
			int last = text[textlen - 1];
			switch(last){
			case '\u3044': also = IADJ; break;
			case '\u3046': also = VERB5U; break;
			case '\u304f': also = VERB5K; break;
			case '\u3050': also = VERB5G; break;
			case '\u3059': also = VERB5S; break;
			case '\u305a': also = VERB5Z; break;
			case '\u3064': also = VERB5T; break;
			case '\u306c': also = VERB5N; break;
			case '\u3076': also = VERB5B; break;
			case '\u3080': also = VERB5M; break;
			case '\u308b': also = VERB5R; break;
			default: also = 0;
			}

			if(also != 0){
				ptr = findReading(text, textlen-1);
				if(ptr > 0){
					optcount = addWords(ptr, option, tag, optcount, also, last);
					if(also == VERB5R){
						optcount = addWords(ptr, option, tag, optcount, VERB1, last);
					}
				}
			}
		}	
				 
		if(optcount > 1) return optcount;
		return 0;
	}
	
}

