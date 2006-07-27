package net.frotz.rpn;

import dgs.libs.hipfloat.hipfloat;
import dgs.libs.hipfloat.hipfloatError;

import danger.ui.ScreenWindow;
import danger.app.Event;
import danger.ui.Font;
import danger.ui.Pen;
import danger.ui.Color;
import danger.ui.StaticTextBox;
import danger.app.ResourceDatabase;


public class CalcWindow extends ScreenWindow 
	implements Events, Resources
{
	public CalcWindow(RPN rpn) {
		super("RPN Calculator");

		rdb = rpn.getResources();
		rdb.addToMenuFromResource(getActionMenu(),MENU_MAIN,this,null);
	
		font = Font.findFont("BortBold12");
		height = font.getAscent() + font.getDescent() + 2;

		help = new StaticTextBox();
		help.setPosition(getWidth()/2 - 8, 0);
		help.setSize(getWidth()/2 + 8, getHeight() - height);
		help.setFont(Font.findFont("Fixed5x7"));
		help.setText(HELP);
		help.show();
		addChild(help);		
	}

	public boolean receiveEvent(Event e){
		switch(e.type){
		case DO_TOGGLE_HELP:
			show_help = !show_help;
			invalidate();
			return true;
		case DO_TOGGLE_SCI:
			sci_mode = !sci_mode;
			invalidate();
			return true;
		case DO_ABOUT_BOX:
			(rdb.getDialog(DIALOG_ABOUT)).show();
			return true;
		default:
			return super.receiveEvent(e);
		}
	}

	public void paint(Pen p) {
		clear(p);
		
		int y = getHeight() - 3;
		int ptr = SP;
		
		p.setFont(font);
		
		p.setColor(Color.BLACK);
		p.fillRect(0, y - height + 3, getWidth(), getHeight());
		p.setColor(Color.WHITE);
		if(EP > 0){
			p.drawText(3, y, ENTRY, 0, EP);
		}
		p.setColor(Color.BLACK);
		y -= height;

		while((y >= 5) && (ptr > 0)){
			ptr--;
			int count = STACK[ptr].toCharArray(scratch, sci_mode);
			p.drawText(3, y, scratch, 0, count);
			y -= height;
		}

		if(show_help) {
			paintChildren(p);
		}
	}
	

	public boolean eventKeyDown(char c, Event event) {
		switch(c) {
		case 8:
			if(EP > 0){
					/* kinda gross, but ensures we end up in
					   the right input state, just as if the
					   user had typed it to this point */
				int ptr;
				int max = EP - 1;
				EP = 0;
				dot = 0;
				ee = 0;
				for(ptr = 0; ptr < max; ptr++){
					eventKeyDown(ENTRY[ptr], event);
				}
			} else {
				if(SP > 0){
					pop();
				}
			}
			break;	

		case '0': case '1': case '2': case '3': case '4':
		case '5': case '6': case '7': case '8': case '9':
			ENTRY[EP++] = c;
			break;
		case '.':
			if((dot == 0) && (ee == 0)){
				dot = 1;
				ENTRY[EP++] = '.';
			}
			break;
		case 'E':
			if((EP > 0) && (ee == 0)){
				ee = 1;
				ENTRY[EP++] = 'E';
			}
			break;
		case '-':
			if((EP > 0) && (ee == 1)){
				ee = 2;
				ENTRY[EP++] = '-';
				break;
			}
				/* fall through */
		case 's':
			accept();
			if(SP > 1){
				hipfloat b = pop();
				hipfloat a = pop();
				push(a.sub(b));
			}
			break;
		case '^':
		case 'p':
			accept();
			if(SP > 1){
				hipfloat b = pop();
				hipfloat a = pop();
				push(a.pow(b));
			}
			break;
		case '+':
		case 'a':
			accept();
			if(SP > 1){
				hipfloat b = pop();
				hipfloat a = pop();
				push(a.add(b));
			}
			break;
		case '*':
		case 'm':
			accept();
			if(SP > 1){
				hipfloat b = pop();
				hipfloat a = pop();
				push(a.mul(b));
			}
			break;
		case '/':
		case 'd':
			accept();
			if(SP > 1){
				hipfloat b = pop();
				hipfloat a = pop();
				push(a.div(b));
			}
			break;
		case 'x':
			accept();
			if(SP > 1){
				hipfloat a = pop();
				hipfloat b = pop();
				push(a);
				push(b);
			}
			break;
		case 'l':
			accept();
			if(SP > 0){
				push(pop().ln());
			}
			break;	
		case 'r':
			accept();
			if(SP > 0){
				push(pop().sqrt());
			}
			break;
		case 'e':
			accept();
			if(SP > 0) {
				push(pop().exp());
			}
			break;
		case 'f':
			accept();
			if(SP > 0) {
				push(pop().floor());
			}
		case 'F':
			accept();
			if(SP > 0) {
				push(pop().ceil());
			}
			break;
		case 'R':
			accept();
			if(SP > 0) {
				push(pop().round());
			}
			break;
		case '!':
			accept();
			if(SP > 0){
				push(pop().factorial());
			}
			break;
		case 'S':
			accept();
			if(SP > 0){
				push(pop().sin());
			}
			break;
		case 'C':
			accept();
			if(SP > 0){
				push(pop().cos());
			}
			break;
		case 'T':
			accept();
			if(SP > 0){
				push(pop().tan());
			}
			break;
		case 'u':
		case '_':
			accept();
			if(SP > 0){
				push(pop().neg());
			}
			break;
		case 13:
			if(EP > 0){
				accept();
			} else {
				if(SP > 0){
					push(top());
				}
			}
			break;
		}

		invalidate();	
		return true;
	}
	
	void accept() {
		if(EP > 0){
			try {
				push(new hipfloat(new String(ENTRY, 0, EP)));
			} catch (hipfloatError err){
				push(err.actual_return);
			}
			EP = 0;
			dot = 0;
			ee = 0;
		}
	}
	

	void push(hipfloat f) {
		STACK[SP] = f;
		SP++;
	}

	hipfloat pop() {
		if(SP == 0){
			return null;
		} else {
			SP--;
			hipfloat f = STACK[SP];
			STACK[SP] = null;
			return f;
		}
	}

	hipfloat top() {
		if(SP == 0){
			return null;
		} else {
			return STACK[SP-1];
		}
	}

	void replace(hipfloat f) {
		STACK[SP-1] = f;
	}		

	String entry;
	
	hipfloat STACK[] = new hipfloat[128];
	int SP;
	
	char ENTRY[] = new char[64];
	char scratch[] = new char[64];
	int EP;
	int dot;
	int ee;
	
	Font font;
	int height;

	ResourceDatabase rdb;
	boolean show_help = true;
	boolean sci_mode = false;
	StaticTextBox help;
	
	String HELP =
	"-QUICK REFERENCE CARD-\n\n"+
	"a>add s>sub m>mul d>div\n"+
	"p>pow e>exp l>ln  u>neg\n\n"+
	"S>sin C>cos T>tan r>sqr\n\n"+
	"!>fact f>floor F>ceil\n\n"+
	"x>swap\n\n"+
	"rtn>dup\n"+
	"del>drop\n";
}
