package org.cursetheyagudo.ichigo;

import danger.app.Timer;
import danger.app.Application;
import danger.app.Event;
import danger.ui.ScreenWindow;
import danger.ui.Pen;
import danger.ui.Font;
import danger.ui.Color;
import danger.ui.Rect;


public class ichigo extends Application
{
	public ichigo() {
		mWindow = new MainWindow();
		mWindow.setTitle("ichigo");
	}

	public boolean receiveEvent(Event e) {
		return (super.receiveEvent(e));
	}

	public void launch() {
	}

	public void resume() {
        mWindow.show();
		mWindow.start();
	}

	public void suspend() {
		mWindow.stop();
	}

	static MainWindow	mWindow;
}

class MainWindow extends ScreenWindow 
{
	public MainWindow() {
		bounds = getBounds();
        font = Font.findBoldSystemFont();
		time = new VanaTime();
		timer = new Timer(333, true, this); 
	}

	void start() {
		timer.start();
	}

	void stop() {
		timer.stop();
	}
	
	public boolean receiveEvent(Event e) {
		if(e.type == Event.EVENT_TIMER){
			invalidate();
		}
		return super.receiveEvent(e);
	}
	
	public void paint(Pen p) {
		String msg;
		int w,y;
		
		clear(p);

		time.setToEarthTime();

		p.setColor(Color.BLACK);
		p.drawRect(bounds);
		p.setFont(font);

		w = bounds.getWidth();
		y = bounds.getHeight() / 2 - 15;
		
		msg = time.year + "/" + time.mon + "/" + time.mday;
		p.drawText((w - font.getWidth(msg)) / 2, y, msg);
		y += 10;

		msg = time.dayString();
		p.drawText((w - font.getWidth(msg)) / 2, y, msg);
		y += 10;

		msg = time.timeString24();
		p.drawText((w - font.getWidth(msg)) / 2, y, msg);
	}


	Timer timer;
	Rect bounds;
	Font font;
	VanaTime time;
}
