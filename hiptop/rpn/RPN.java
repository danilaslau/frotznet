package net.frotz.rpn;

import danger.app.Application;

public class RPN extends Application
{
	public RPN() {
	}

	public void resume() {
		if(win == null) {
			win = new CalcWindow(this);
			win.show();
		}
	}

	CalcWindow win;
}
