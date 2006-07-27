/* Copyright 2002-2003, Brian Swetland */

package net.frotz.dictionary;

import danger.app.Event;
import danger.ui.View;
import danger.ui.Font;
import danger.ui.Pen;
import danger.ui.Color;
import danger.ui.Scrollbar;
import danger.util.Pasteboard;

public class ResultsView extends View
{
	public ResultsView() {
		font = Font.findSystemFont();
		bigfont = null; //Font.findFont("jiskan16");
		lineheight = font.getAscent() + font.getDescent() + 2;
		adjust = font.getAscent() + 1;
		
		tmp = new char[128];
		line = new int[128];
		data = new char[512];
		
		tlen = 0;
		twidth = 0;
		tflags = 4;
		marker = -1;
		markend = -1;
		setAcceptFocus(true);
	}
	
	public void MarkerNext() {
		if(marker < 0) marker = -1;
		
		try {
			while(marker < lines){				
				marker++;
				if((data[line[marker] - 1] & 4) != 0) break;			
			}
			markend = marker + 1;
			while(markend < lines){
				if((data[line[markend] - 1] & 4) != 0) break;
				markend++;
			}
			if(!IsVisible(marker)){
				ScrollTo(marker);
			} else {
				invalidate();
			}
		} catch(Throwable t){
			MarkerPrev();
		}
	}

	public void MarkerPrev() {
		try {
			while(marker > 0){
				marker--;
				if((data[line[marker] - 1] & 4) != 0) break;			
			}
			markend = marker + 1;
			while(markend < lines){
				if((data[line[markend] - 1] & 4) != 0) break;
				markend++;
			}
			
			if(!IsVisible(marker)) {
				ScrollTo(marker);
			} else {
				invalidate();
			}
		} catch(Throwable t){
		}
	}
	
	

	public void paint(Pen p) {
		int y, n, w, h, ptr, flags, mpos;
		p.setFont(font);
		
		h = getHeight();
		w = getWidth();
		mpos = -1;
		
		for(y = 0, n = firstline; (y < h) && (n < lines); y += lineheight, n++){
			ptr = line[n];
			flags = data[ptr - 1];
			
			if((n >= marker) && (n < markend)) {
				p.setColor(Color.BLACK);
				p.fillRect(0, y, w, y + lineheight);
				p.setColor(Color.WHITE);
				if(mpos == -1) mpos = y;
			} else {
				if((flags & 1) == 0){
					p.setColor(0xa0ffa0);
				} else {
					p.setColor(Color.WHITE);
				}
				p.fillRect(0, y, w, y + lineheight);
				p.setColor(Color.BLACK);
			}

			p.drawText(1, y + adjust, data, ptr, data[ptr - 2]);
		}

		if(bigfont != null){
			if(marker != -1){
				if(mpos < (h/2)){
					mpos = h - 23;
				} else {
					mpos = 0;
				}
				p.setColor(Color.WHITE);
				p.fillRect(0, mpos, w, mpos + 22);
				p.setColor(Color.BLACK);
				p.drawRect(0, mpos, w, mpos + 22);
				
				ptr = line[marker];
				p.setFont(bigfont);
				p.drawText(3, mpos + 17, data, ptr, data[ptr - 2]);
			}
		}
	}

	public void SetScrollbar(Scrollbar s) {
		sb = s;
		sb.setData(firstline, getHeight() / lineheight, lines);
	}
	
	public void Clear() {
		dlen = 0;
		lines = 0;
		tlen = 0;
		twidth = 0;
		firstline = 0;
		tflags = 6;
		marker = -1;
		markend = -1;
		if(sb != null) {
			sb.setData(firstline, getHeight() / lineheight, lines);
			sb.invalidate();
		}
		invalidate();
	}
	
	public void Append(String s) {
		int i,l;
		l = s.length();
		for(i = 0; i < l; i++){
			Append(s.charAt(i));
		}
	}

	public void Append(char[] data) {
		int i,l;
		l = data.length;
		for(i = 0; i < l; i++){
			Append(data[i]);
		}
	}
	
	void Append(char c) {
		int w;
		
		if((c == '\r') || (c == '\n')){
			if(tlen > 0){
				AddLine(tmp, tlen, tflags);
				tlen = 0;
				twidth = 0;
				tflags = (tflags ^ 1) | 4;
			}
			return;
		}

		w = font.charWidth(c);
		
		if((twidth + w) > getWidth()){
			int i;
			for(i = tlen - 1; i > 0; i--){
				if(tmp[i] == ' ') {
					AddLine(tmp, i, tflags);
					tflags &= (~4);
					twidth = 0;
					int j;
					for(j = i + 1, i = 0; j < tlen; j++){
						twidth += font.charWidth(tmp[j]);
						tmp[i++] = tmp[j];
					}
					tlen = i + 1;
					twidth += w;
					tmp[i] = c;
					return;
				}
			}

			AddLine(tmp,tlen, tflags);
			tflags &= (~4);
			tlen = 1;
			twidth = w;
			tmp[0] = c;
		} else {
			tmp[tlen++] = c;
			twidth += w;
		}
	}

	void AddLine(char[] linedata, int len, int flags) {

		len += 2; /* space for flags & len */
		
			/* make space in the data array if needed */
		if((len + dlen) > data.length){
			int l = data.length;
			while((len + dlen) > l) l *= 2;
			char[] x = new char[l];
			System.arraycopy(data, 0, x, 0, dlen);
			data = x;
		}

			/* make space in the line offset array if needed */
		if(lines == line.length){
			int[] x = new int[line.length*2];
			System.arraycopy(line, 0, x, 0, line.length);
			line = x;
		}

		len -= 2;
		
		data[dlen++] = (char) len;
		data[dlen++] = (char) flags;
		
		line[lines++] = dlen;

		System.arraycopy(linedata, 0, data, dlen, len);
		dlen += len;
		
		if(sb != null){
			sb.setData(firstline, getHeight() / lineheight, lines);
			sb.invalidate();
		}
	}
	
	int TotalLines() {
		return lines;
	}

	int VisibleLines() {
		return getHeight() / lineheight;
	}

	boolean IsVisible(int line) {
		if(line < firstline) return false;
		if(line >= (firstline + (getHeight() / lineheight))) return false;
		return true;
	}
	
	void ScrollTo(int posn) {
		firstline = posn;
		scroll();
	}
	
	void Scroll(int delta) {
		firstline += delta;
		scroll();
	}
	
	void scroll() {
		int max = getHeight() / lineheight;
		
		if(firstline > (lines - max)){
			firstline = lines - max;
		}
		if(firstline < 0) {
			firstline = 0;
		}
		if(sb != null){
			sb.setData(firstline, max, lines > max ? lines : max);
			sb.invalidate();
		}
		invalidate();
	}

	public boolean eventWidgetUp(int widget, Event event) {
		switch(widget) {
		case Event.DEVICE_WHEEL_BUTTON:
			if(marker == -1){
				MarkerNext();
			} else {
				int ptr, end;
				ptr = marker;
				marker = -1;
				markend = -1;
				ScrollTo(ptr);		
				ptr = line[ptr];
				end = ptr + 1;
				while(data[end] != ' ') end++;
				Pasteboard.setString(new String(data, ptr, end - ptr));
			}
			return true;
		case Event.DEVICE_WHEEL:
			if(marker == -1) {
				Scroll(-1);
			} else {
				MarkerPrev();
			}
			return true;
		case Event.DEVICE_WHEEL_PAGE_UP:
			Scroll(-(VisibleLines() - 1));
			return true;
		case Event.DEVICE_ARROW_RIGHT:
		case Event.DEVICE_ARROW_DOWN:
			MarkerNext();
			return true;
		case Event.DEVICE_ARROW_LEFT:
		case Event.DEVICE_ARROW_UP:
			MarkerPrev();
			return true;
		default:
			return false;
		}
	}

	public boolean eventWidgetDown(int widget, Event event) {
		switch(widget) {
		case Event.DEVICE_WHEEL:
			if(marker == -1) {
				Scroll(1);
			} else {
				MarkerNext();
			}
			return true;
		case Event.DEVICE_WHEEL_PAGE_DOWN:
			Scroll(VisibleLines()-1);
			return true;
		default:
			return false;
		}
	}	

	
	char data[];
	int dlen;
		
	int line[];
	int lines;
	
	char tmp[];
	int tlen;
	int twidth;
	int tflags;
	
	int lineheight;
	int adjust;
	int firstline;
	Font font;
	Font bigfont;
	
	int marker;
	int markend;
	
	Scrollbar sb;
}
