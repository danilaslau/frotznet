// Copyright 2002-2003, Brian Swetland
// See provided LICENSE file.

package net.frotz.jp_im;

import danger.ui.InputMethod;
import danger.ui.EditText;
import danger.app.Event;

class JapaneseInputMethod extends InputMethod
{
	Dictionary d;

		/* romaji -> kana conversion */
	int kanamap[];
	int entries;	
	int key;
	int count;

		/* kana assembly buffer */
	char text[] = new char[64];
	int textlen;

		/* kana -> kanji conversion options */
	String options[] = new String[128];
	String tags[] = new String[128];
	int optcount;
	int curopt;	

		/* useful information about our EditText */
	int point;
	
	int mode = ModeKana;
	
	static final int ModeKana = 1;
	static final int ModeSelect = 2;
	
	JapaneseInputMethod(Dictionary d, int map[], int c, EditText et) {
		this.d = d;
		kanamap = map;
		entries = c;
		mEditText = et;
		if(et != null){
			point = et.getInsertionPoint();
			et.setSelection(point,point);
		}
	}

	public InputMethod createInstance(EditText et) {
		return new JapaneseInputMethod(d, kanamap, entries, et);
	}

	public boolean eventKeyDown(char c, Event event) {
		if(mode == ModeKana){
			switch(c){
			case 13:
				if(textlen > 0) {
					insertText(new String(text, 0, textlen));	
					textlen = 0;
				}
				break;
			case ' ':
				if(d != null){
					optcount = d.lookup(text, textlen, options, tags);
					if(optcount != 0) {
						mode = ModeSelect;
						curopt = 1;
						refresh();
					}
				} else {
					System.err.println("NO DICTIONARY AVAILABLE");
				}
				break;
			case 8:
				if(textlen > 0){
					textlen--;
					refresh();
				}
				break;
			default:
				process(c, event);
				break;
			}
		} else {
			switch(c){
			case 13:
				insertText(options[curopt]);
				textlen = 0;
				mode = ModeKana;
				break;
			case 8:
				mode = ModeKana;
				refresh();
				break;
			case ' ':
				curopt = (curopt + 1) % optcount;
				refresh();
				break;
			}
		}
		return true;
	}

	void refresh() {
		if(mode == ModeKana){
			updateDisplay(new String(text, 0, textlen));
		} else {
			updateDisplay(options[curopt]);
		}
	}
	
	public boolean eventWidgetUp(int widget, Event event) {
		if(widget == Event.DEVICE_WHEEL){
			if(mode != ModeSelect) return true;
			curopt--;
			if(curopt < 0) curopt = optcount - 1;
			refresh();
		}
		return true;
	}
	public boolean eventWidgetDown(int widget, Event event) {
		if(widget == Event.DEVICE_WHEEL){
			if(mode != ModeSelect) return true;
			curopt++;
			if(curopt >= optcount) curopt = 0;
			refresh();
		} else if(widget == Event.DEVICE_WHEEL_BUTTON) {
			return eventKeyDown((char)13, event);
		}
		
		return true;
	}

	public void done() {
	}
	
	void updateDisplay(String content) {
		if(content != null){
			mEditText.insert(content);
			mEditText.setSelection(point, point + content.length());
		}
	}
	
	void insertText(String content) {
		mEditText.insert(content);
		point = mEditText.getInsertionPoint();
		mEditText.setSelection(point, point);
	}
			
	void handlekey(int c) {
		if(c == 0) return;
		text[textlen++] = (char) c;
		refresh();
	}
	
	boolean process(int c, Event event) {
		int i, j;
		int max;
		int[] map;
		
		map = kanamap;		
		if(map == null) {
			System.err.println("IM: no kanamap?!");
			cancel();
			return false;
		}
		
		if(((c >= 'a') && (c <= 'z')) ||
		   ((c >= 'A') && (c <= 'Z')) || (c == '-')){

			if((count == 1) && (c == key)){
					/* handle doubled letters */
				if((c >= 'a') && (c <= 'z') && (c != 'a') &&
				   (c != 'a') && (c != 'i') && (c != 'u') && 
				   (c != 'e') && (c != 'o') && (c != 'y') && (c != 'n')){
					handlekey('\u3063');
					return true;
				}
				if((c >= 'A') && (c <= 'Z') &&
				   (c != 'A') && (c != 'I') && (c != 'U') && 
				   (c != 'E') && (c != 'O') && (c != 'Y') && (c != 'N')){
					handlekey('\u30C3');
					return true;
				}
			}
			count++;
			key = (key << 8) + c;
			
			max = entries;
			
			int l, r, n;
			l = -1;
			r = max;
			
			while((r - l) > 1) {
				n = (r + l) / 2;
				i = map[n << 2];
//				System.err.println("** " + map[n<<2] + " : " + key + " **");
				if(i == key) {
					i = n << 2;
					for(j = i + 1; j < i + 4; j++) handlekey((char) map[j]);
					key = 0;
					count = 0;
					return true;
				} else if(i > key) {
					l = n;
					n = l + (r - l) / 2;
				} else {
					r = n;
					n = l + (r - l) / 2;
				}
			}			
			
				/* no match... toss it */
			if(count == 4){
				key = 0;
				count = 0;
			}
			
			return true;
		} else {
			handlekey(c);
			key = 0;
			count = 0;
			return true;
		}
	}

	static final boolean WITH_KANA2KANJI = false;

}
