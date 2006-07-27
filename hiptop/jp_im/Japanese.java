// Copyright 2002-2003, Brian Swetland
// See provided LICENSE file.

package net.frotz.jp_im;

import danger.app.Application;
import danger.app.ResourceDatabase;
import danger.app.Resource;

import java.io.InputStream;
import java.io.IOException;

import danger.util.Decompressor;
import danger.util.ByteArray;
import danger.app.DataStore;

import danger.ui.ScreenWindow;

public class Japanese extends Application
{
	byte[] Inflate(InputStream is) {
		try {
			Decompressor dc;
			int n, sz;
			byte[] in = new byte[8192];
			
			if(is.read(in,0,4) != 4) return null;
			sz = ByteArray.readInt(in,0);
			byte[] out = new byte[sz];

			int off = 0;
			dc = new Decompressor();
			while((n = is.read(in)) > 0){
				dc.setInput(in, 0, n);
				n = dc.inflate(out, off, sz - off);
				if(n > 0){
					off += n;
				}
			}			
			System.err.println("DONE? " + dc.finished());

			return out;
		} catch (Throwable t) {
			return null;
		}
		
	}
	
	public Japanese() {
		System.err.println("*** JAPANESE INPUT SUPPORT ***");
		ResourceDatabase db = getResources();

		try {
			InputStream is;
			is = db.getResourceStream(1000, 3);
			byte[] dict = Inflate(is);
			if(dict != null) {
				d = new Dictionary(dict);
			}
		} catch (Throwable t){
		}

		init(db.getResource(1000,2));

		im = new JapaneseInputMethod(d, kanamap, entries, null);
		im.install();
	}
	
	public void resume() {
		if(setup == null){
			setup = new SetupWindow(this);
			setup.show();
		}
	}

	Dictionary d;
	JapaneseInputMethod im;
	SetupWindow setup;
	
	void init(Resource rsrc) {
		if(rsrc != null) {
			System.err.println("@@@ found a kana map " + rsrc.getSize() + " @@@");
			try {
				entries = rsrc.getSize() / 10;
				int offset = 0;
				int i;
				
				kanamap = new int[entries * 4];
				for(i = 0; i < entries; i++){
					kanamap[i * 4 + 0] = rsrc.getInt(offset);
					offset += 4;
					kanamap[i * 4 + 1] = rsrc.getShort(offset);
					offset += 2;
					kanamap[i * 4 + 2] = rsrc.getShort(offset);
					offset += 2;
					kanamap[i * 4 + 3] = rsrc.getShort(offset);
					offset += 2;
				}
			} catch (Throwable t) {
				kanamap = null;
			}
		} else {
			System.err.println("@@@ cannot find hiragana map @@@");
		}			
	}

	int entries;
	int kanamap[];
}


class SetupWindow extends ScreenWindow
{
	SetupWindow(Japanese _app) {
		super("Japanese Input");
		app = _app;
	}

	Japanese app;
}

