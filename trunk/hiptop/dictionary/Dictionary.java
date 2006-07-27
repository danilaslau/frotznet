/* Copyright 2002, Brian J. Swetland.  Share and Enjoy. */

package net.frotz.dictionary;

import danger.app.Application;
import danger.app.Registrar;
import danger.app.IPCMessage;
import danger.app.IPCIncoming;

import danger.ui.ScreenWindow;
import danger.ui.EditText;
import danger.ui.Layout;
import danger.ui.Window;
import danger.ui.Pen;
import danger.ui.TitleBar;
import danger.ui.Scrollbar;
import danger.ui.Color;

import danger.app.Event;

import java.io.InputStream;
import java.io.IOException;

import danger.system.Hardware;

import danger.util.DEBUG;

import danger.app.ResourceDatabase;
import java.io.InputStream;

import danger.util.Decompressor;
import danger.util.ByteArray;
import danger.app.DataStore;

public class Dictionary extends Application
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
			System.err.println("OOPS: " + t);
			t.printStackTrace();
			return null;
		}
		
	}
	
	public Dictionary() {
		ResourceDatabase db = getResources();
		DataStore ds = DataStore.findDataStore("edict");
		if(ds == null) ds = DataStore.createDataStore("edit");
		
			/* just in case */
		if(ds.getRecordCount() < 2){
			ds.removeRecord(0);
		}

		if(ds.getRecordCount() == 2) {
			System.err.println("dictionary: loading from datastore");
			data = ds.getRecordData(0);
			index = ds.getRecordData(1);
		} else {
			System.err.println("dictionary: loading from resources");
			try {
				InputStream is;
				
				is = db.getResourceStream(1000, 1);
				data = Inflate(is);
				
				is = db.getResourceStream(1000, 2);
				index = Inflate(is);
			} catch (Throwable t){
				System.err.println("OOPS: " +t);
				t.printStackTrace();
			}
			
			if((data != null) && (index != null)) {
				ds.addRecord(data);
				ds.addRecord(index);
			}
		}

		jdict = new JapaneseDictionary(data,index);		
	}
	
	public boolean receiveEvent(Event e) {
		if (e.type == Event.EVENT_MESSAGE) {
			IPCMessage msg = ((IPCIncoming) e.argument).getMessage();
			String s = msg.findString("lookup");
			Query(s);
			bringToForeground();
			return true;
		} else {
			return super.receiveEvent(e);
		}
	}
	
	public void resume() {
		if(win == null) {
			win = new DictionaryWindow(this);
			win.show();
		}
		win.show();
	}

	void Query(String line) {
		int l = line.length();
		if((l > 0) && (line.charAt(l - 1) < ' ')){
				/* Why textfield likes to tack a return on the end,
				   I'll never know... */
			line = line.substring(0, l - 1);
		}		

		if(line.equalsIgnoreCase("/clear")){
			win.MSG(kClear,null);
		} else {
//				win.MSG(kEntry,"Search for: " + line + "\n");

			String s;
			if(line.length() == 0) return;

			s = jdict.Query(line);
			win.MSG(kClear,null);
			if(s.length() == 0){
				win.MSG(kEntry,"- no match -\n");
			} else {
				win.MSG(kEntry,s);
			}
		}
		return;
	}

	DictionaryWindow win;
	static final int kEntry = 1;
	static final int kClear = 2;
	JapaneseDictionary jdict;	
	byte[] data;
	byte[] index;
	
}

class EntryWindow extends Window
{
	public EntryWindow(DictionaryWindow dw) {
		super(0, 24, dw.getWidth(), 24 + 22);
		query = new EditText(true, true);		
		query.setSize(getWidth() - 6, 18);
		query.setPosition(3, 2);
		this.addChild(query);
		query.show();
		setBackgroundColor(Color.GRAY11);

		setFocusedChild(query);		
		this.dw = dw;
	}
	
	public boolean eventKeyDown(char key, Event event) {
		if((key == '\n') || (key == '\r')){
			dw.dict.Query(query.toString());
			query.clear();
			dw.entryactive = false;
			hide();
			return true;
		}
		return super.eventKeyDown(key, event);
	}
	
	DictionaryWindow dw;
	EditText query;
}


class DictionaryWindow extends ScreenWindow
{
	public DictionaryWindow(Dictionary d) {
		super("Dictionary");
		
		dict = d;
		
		query = new EditText(true, true);
		result = new ResultsView();
		sb = new Scrollbar(true);
		
		sb.setSize(sb.getWidth(), getHeight());
		sb.setPosition(getWidth() - sb.getWidth(), 0);
		addChild(sb);
		sb.show();
		
		result.setSize(getWidth() - sb.getWidth(), getHeight());
		addChild(result);
		result.show();
		
		setFocusedChild(result);

		result.SetScrollbar(sb);
		
		ewin = new EntryWindow(this);
	}

	public boolean eventKeyUp(char key, Event event) {
		if((key == '\n') || (key == '\r')){
			dict.Query(query.toString());
			query.clear();
			return true;
		}
		return super.eventKeyUp(key, event);
	}

	void MSG(int id, Object obj) {
		Event e = new Event(this, id);
		e.argument = obj;
		getListener().sendEvent(e);
	}
	
	public boolean eventKeyDown(char c, Event event) {
		entryactive = true;
		ewin.show();
		ewin.eventKeyDown(c,event);
		return true;
	}
	
	public boolean receiveEvent(Event e) {
		if(entryactive){
			ewin.receiveEvent(e);
		}
		switch(e.type){
		case Dictionary.kEntry:
			result.Append((String)e.argument);
			result.invalidate();
			return true;
		case Dictionary.kClear:
			result.Clear();
			result.invalidate();
			return true;
		default:
			return super.receiveEvent(e);
		}
	}

	Dictionary dict;
	
	EditText query;
	ResultsView result;
	Scrollbar sb;
	
	boolean entryactive;
	EntryWindow ewin;
}
